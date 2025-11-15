package com.microservices.apigateway.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RateLimitFilter implements GatewayFilter, Ordered {

    @Value("${gateway.rate-limit.capacity:100}")
    private int capacity;

    @Value("${gateway.rate-limit.refill-tokens:100}")
    private int refillTokens;

    @Value("${gateway.rate-limit.refill-duration-minutes:1}")
    private int refillDurationMinutes;

    @Value("${gateway.rate-limit.cache-cleanup-interval-minutes:60}")
    private int cacheCleanupIntervalMinutes;

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "rate-limit-cleanup");
        t.setDaemon(true);
        return t;
    });

    @PostConstruct
    public void init() {
        // Schedule periodic cleanup of unused buckets
        cleanupScheduler.scheduleAtFixedRate(
            this::cleanupUnusedBuckets,
            cacheCleanupIntervalMinutes,
            cacheCleanupIntervalMinutes,
            TimeUnit.MINUTES
        );
        log.info("Rate limit filter initialized with capacity: {}, refill: {} tokens per {} minutes",
            capacity, refillTokens, refillDurationMinutes);
    }

    @PreDestroy
    public void destroy() {
        log.info("Shutting down rate limit cleanup scheduler");
        cleanupScheduler.shutdown();
        try {
            if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        cache.clear();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String ip = getClientIp(request);

        if (ip == null || ip.isEmpty() || "unknown".equals(ip)) {
            log.warn("Unable to determine client IP, allowing request");
            return chain.filter(exchange);
        }

        Bucket bucket = cache.computeIfAbsent(ip, k -> createNewBucket());

        if (bucket.tryConsume(1)) {
            long remaining = bucket.getAvailableTokens();
            ServerHttpResponse response = exchange.getResponse();
            response.getHeaders().add("X-RateLimit-Limit", String.valueOf(capacity));
            response.getHeaders().add("X-RateLimit-Remaining", String.valueOf(remaining));
            response.getHeaders().add("X-RateLimit-Reset", String.valueOf(
                System.currentTimeMillis() / 1000 + (refillDurationMinutes * 60)
            ));
            return chain.filter(exchange);
        } else {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            response.getHeaders().add("X-RateLimit-Limit", String.valueOf(capacity));
            response.getHeaders().add("X-RateLimit-Remaining", String.valueOf(bucket.getAvailableTokens()));
            response.getHeaders().add("X-RateLimit-Reset", String.valueOf(
                System.currentTimeMillis() / 1000 + (refillDurationMinutes * 60)
            ));
            log.warn("Rate limit exceeded for IP: {}", ip);
            return response.setComplete();
        }
    }

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.builder()
            .capacity(capacity)
            .refillIntervally(refillTokens, Duration.ofMinutes(refillDurationMinutes))
            .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIp(ServerHttpRequest request) {
        // Check X-Forwarded-For header (for proxies/load balancers)
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one (original client)
            String[] ips = xForwardedFor.split(",");
            if (ips.length > 0) {
                String ip = ips[0].trim();
                if (!ip.isEmpty()) {
                    return ip;
                }
            }
        }

        // Check X-Real-IP header
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp.trim();
        }

        // Fallback to remote address
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        return "unknown";
    }

    /**
     * Cleanup unused buckets to prevent memory leaks.
     * Removes buckets that haven't been used recently.
     */
    private void cleanupUnusedBuckets() {
        try {
            int initialSize = cache.size();
            // Remove buckets that are at full capacity (unused)
            cache.entrySet().removeIf(entry -> {
                Bucket bucket = entry.getValue();
                return bucket.getAvailableTokens() >= capacity;
            });
            int removed = initialSize - cache.size();
            if (removed > 0) {
                log.debug("Cleaned up {} unused rate limit buckets. Remaining: {}", removed, cache.size());
            }
        } catch (Exception e) {
            log.error("Error during rate limit cache cleanup", e);
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}

