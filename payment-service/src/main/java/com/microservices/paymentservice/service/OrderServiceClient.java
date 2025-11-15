package com.microservices.paymentservice.service;

import com.microservices.common.dto.OrderDTO;
import com.microservices.common.dto.StatusUpdateDTO;
import com.microservices.paymentservice.config.SsoConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceClient {

    private final WebClient orderServiceWebClient;
    private final SsoConfig ssoConfig;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    public Mono<OrderDTO> getOrder(Long id, String token) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("orderService");
        Retry retry = retryRegistry.retry("orderService");
        
        return orderServiceWebClient
            .get()
            .uri("/orders/{id}", id)
            .header(ssoConfig.getTokenHeader(), token)
            .retrieve()
            .bodyToMono(OrderDTO.class)
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(RetryOperator.of(retry))
            .doOnError(error -> log.error("Failed to fetch order with id: {}", id, error))
            .onErrorResume(error -> {
                log.warn("Circuit breaker or retry exhausted for getOrder, id: {}", id, error);
                return Mono.error(new RuntimeException("Order service is currently unavailable. Please try again later.", error));
            });
    }

    public Mono<OrderDTO> updateOrderStatus(Long id, StatusUpdateDTO statusUpdate, String token) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("orderService");
        Retry retry = retryRegistry.retry("orderService");
        
        return orderServiceWebClient
            .put()
            .uri("/orders/{id}/status", id)
            .header(ssoConfig.getTokenHeader(), token)
            .bodyValue(statusUpdate)
            .retrieve()
            .bodyToMono(OrderDTO.class)
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(RetryOperator.of(retry))
            .doOnError(error -> log.error("Failed to update order status for id: {}", id, error))
            .onErrorResume(error -> {
                log.warn("Circuit breaker or retry exhausted for updateOrderStatus, id: {}", id, error);
                return Mono.error(new RuntimeException("Order service is currently unavailable. Please try again later.", error));
            });
    }
}

