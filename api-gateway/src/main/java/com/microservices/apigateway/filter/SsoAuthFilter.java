package com.microservices.apigateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.apigateway.config.SsoConfig;
import com.microservices.common.dto.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class SsoAuthFilter implements GlobalFilter, Ordered {

    private final SsoConfig ssoConfig;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        if (!ssoConfig.isValidateEnabled()) {
            return chain.filter(exchange);
        }

        String token = request.getHeaders().getFirst(ssoConfig.getTokenHeader());
        
        if (token == null || token.isEmpty()) {
            return handleUnauthorized(exchange, "Missing authentication token");
        }

        return getUserInfo(token)
            .flatMap(userInfo -> {
                if (userInfo != null && userInfo.isAuthenticated()) {
                    try {
                        String userInfoJson = objectMapper.writeValueAsString(userInfo);
                        ServerHttpRequest modifiedRequest = request.mutate()
                            .header(ssoConfig.getTokenHeader(), token)
                            .header("X-User-Info", userInfoJson)
                            .header("X-User-Role", userInfo.getRole().name())
                            .header("X-User-Id", userInfo.getUserId())
                            .build();
                        return chain.filter(exchange.mutate().request(modifiedRequest).build());
                    } catch (Exception e) {
                        log.error("Error serializing user info", e);
                        return handleUnauthorized(exchange, "Token validation failed");
                    }
                } else {
                    return handleUnauthorized(exchange, "Invalid authentication token");
                }
            })
            .onErrorResume(error -> {
                log.error("Token validation error", error);
                return handleUnauthorized(exchange, "Token validation failed");
            });
    }

    private Mono<UserInfo> getUserInfo(String token) {
        return webClientBuilder
            .baseUrl(ssoConfig.getSsoServiceUrl())
            .build()
            .get()
            .uri("/auth/userinfo")
            .header(ssoConfig.getTokenHeader(), token)
            .retrieve()
            .bodyToMono(UserInfo.class)
            .onErrorResume(error -> {
                log.error("Failed to get user info from SSO service", error);
                return Mono.empty();
            });
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("X-Error-Message", message);
        log.warn("Unauthorized access attempt: {}", message);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}

