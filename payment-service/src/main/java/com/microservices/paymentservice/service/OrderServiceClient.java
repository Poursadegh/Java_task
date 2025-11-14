package com.microservices.paymentservice.service;

import com.microservices.common.dto.OrderDTO;
import com.microservices.common.dto.StatusUpdateDTO;
import com.microservices.paymentservice.config.SsoConfig;
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

    public Mono<OrderDTO> getOrder(Long id, String token) {
        return orderServiceWebClient
            .get()
            .uri("/orders/{id}", id)
            .header(ssoConfig.getTokenHeader(), token)
            .retrieve()
            .bodyToMono(OrderDTO.class)
            .doOnError(error -> log.error("Failed to fetch order with id: {}", id, error))
            .onErrorResume(error -> Mono.error(new RuntimeException("Order service unavailable", error)));
    }

    public Mono<OrderDTO> updateOrderStatus(Long id, StatusUpdateDTO statusUpdate, String token) {
        return orderServiceWebClient
            .put()
            .uri("/orders/{id}/status", id)
            .header(ssoConfig.getTokenHeader(), token)
            .bodyValue(statusUpdate)
            .retrieve()
            .bodyToMono(OrderDTO.class)
            .doOnError(error -> log.error("Failed to update order status for id: {}", id, error))
            .onErrorResume(error -> Mono.error(new RuntimeException("Order service unavailable", error)));
    }
}

