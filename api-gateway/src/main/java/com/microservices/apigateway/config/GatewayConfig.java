package com.microservices.apigateway.config;

import com.microservices.apigateway.filter.RateLimitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GatewayConfig {

    @Value("${order.service.uri:http://localhost:8081}")
    private String orderServiceUri;

    @Value("${payment.service.uri:http://localhost:8082}")
    private String paymentServiceUri;

    @Value("${spring.cloud.consul.discovery.enabled:false}")
    private boolean consulDiscoveryEnabled;

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder, RateLimitFilter rateLimitFilter) {
        // Use direct URLs if Consul discovery is disabled, otherwise use load-balanced service names
        String orderUri = consulDiscoveryEnabled ? "lb://order-service" : orderServiceUri;
        String paymentUri = consulDiscoveryEnabled ? "lb://payment-service" : paymentServiceUri;

        return builder.routes()
            .route("order-service", r -> r
                .path("/api/orders/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .filter(rateLimitFilter)
                )
                .uri(orderUri)
            )
            .route("payment-service", r -> r
                .path("/api/payments/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .filter(rateLimitFilter)
                )
                .uri(paymentUri)
            )
            .build();
    }
}

