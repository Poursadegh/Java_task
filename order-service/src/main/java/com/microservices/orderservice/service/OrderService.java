package com.microservices.orderservice.service;

import com.microservices.common.dto.OrderDTO;
import com.microservices.common.enums.OrderStatus;
import com.microservices.common.exception.ResourceNotFoundException;
import com.microservices.orderservice.model.Order;
import com.microservices.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    public Mono<OrderDTO> createOrder(OrderDTO orderDTO) {
        return Mono.fromCallable(() -> {
            Order order = new Order(
                orderDTO.getCustomerId(),
                orderDTO.getAmount(),
                OrderStatus.PENDING,
                orderDTO.getDescription()
            );
            return orderRepository.save(order);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .map(this::convertToDTO);
    }

    public Mono<OrderDTO> getOrderById(@NonNull Long id) {
        Long orderId = Objects.requireNonNull(id, "Order ID cannot be null");
        return Mono.fromCallable(() -> orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId)))
        .subscribeOn(Schedulers.boundedElastic())
        .map(this::convertToDTO);
    }

    public Mono<Page<OrderDTO>> getAllOrders(Pageable pageable) {
        return Mono.fromCallable(() -> orderRepository.findAll(pageable)
            .map(this::convertToDTO))
        .subscribeOn(Schedulers.boundedElastic());
    }

    public Flux<OrderDTO> getAllOrdersStreaming() {
        return Flux.fromIterable(orderRepository.findAll())
            .map(this::convertToDTO)
            .subscribeOn(Schedulers.boundedElastic())
            .doOnNext(order -> log.debug("Streaming order: {}", order.getId()));
    }

    public Flux<OrderDTO> getOrdersByStatus(OrderStatus status) {
        return Flux.fromIterable(orderRepository.findByStatus(status))
            .map(this::convertToDTO)
            .subscribeOn(Schedulers.boundedElastic())
            .doOnNext(order -> log.debug("Streaming order with status {}: {}", status, order.getId()));
    }

    public Flux<OrderDTO> getOrdersByCustomerId(String customerId) {
        return Flux.fromIterable(orderRepository.findByCustomerId(customerId))
            .map(this::convertToDTO)
            .subscribeOn(Schedulers.boundedElastic())
            .doOnNext(order -> log.debug("Streaming order for customer {}: {}", customerId, order.getId()));
    }

    public Mono<OrderDTO> updateOrderStatus(@NonNull Long id, OrderStatus status) {
        Long orderId = Objects.requireNonNull(id, "Order ID cannot be null");
        return Mono.fromCallable(() -> {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
            order.setStatus(status);
            return orderRepository.save(order);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .map(this::convertToDTO);
    }

    public Flux<OrderDTO> createOrdersBatch(List<OrderDTO> orderDTOs) {
        log.info("Creating {} orders in parallel", orderDTOs.size());
        
        return Flux.fromIterable(orderDTOs)
            .flatMap(orderDTO -> 
                createOrder(orderDTO)
                    .onErrorResume(error -> {
                        log.error("Failed to create order for customerId: {}", orderDTO.getCustomerId(), error);
                        OrderDTO failedOrder = new OrderDTO();
                        failedOrder.setCustomerId(orderDTO.getCustomerId());
                        failedOrder.setAmount(orderDTO.getAmount());
                        failedOrder.setDescription(orderDTO.getDescription());
                        failedOrder.setStatus(OrderStatus.CANCELLED);
                        return Mono.just(failedOrder);
                    })
            )
            .parallel()
            .runOn(Schedulers.parallel())
            .sequential();
    }

    public Flux<OrderDTO> watchOrderStatusChanges(Long orderId) {
        return Flux.interval(java.time.Duration.ofSeconds(1))
            .flatMap(tick -> getOrderById(orderId))
            .distinctUntilChanged(OrderDTO::getStatus)
            .doOnNext(order -> log.info("Order {} status changed to: {}", orderId, order.getStatus()))
            .take(10);
    }

    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setCustomerId(order.getCustomerId());
        dto.setAmount(order.getAmount());
        dto.setStatus(order.getStatus());
        dto.setDescription(order.getDescription());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        return dto;
    }
}
