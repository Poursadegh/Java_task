package com.microservices.orderservice.controller;

import com.microservices.common.annotation.RequireRole;
import com.microservices.common.dto.OrderDTO;
import com.microservices.common.dto.StatusUpdateDTO;
import com.microservices.common.enums.OrderStatus;
import com.microservices.common.enums.UserRole;
import com.microservices.orderservice.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @RequireRole({UserRole.USER, UserRole.ADMIN})
    public Mono<ResponseEntity<OrderDTO>> createOrder(@Valid @RequestBody OrderDTO orderDTO) {
        return orderService.createOrder(orderDTO)
            .map(order -> ResponseEntity.status(HttpStatus.CREATED).body(order));
    }

    @GetMapping("/{id}")
    @RequireRole({UserRole.USER, UserRole.ADMIN})
    public Mono<ResponseEntity<OrderDTO>> getOrder(
            @PathVariable @Positive(message = "Order ID must be a positive number") Long id) {
        return orderService.getOrderById(id)
            .map(ResponseEntity::ok);
    }

    @GetMapping
    @RequireRole({UserRole.USER, UserRole.ADMIN})
    public Mono<ResponseEntity<Page<OrderDTO>>> getAllOrders(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return orderService.getAllOrders(pageable)
            .map(ResponseEntity::ok);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequireRole({UserRole.ADMIN})
    public Flux<OrderDTO> getAllOrdersStreaming() {
        return orderService.getAllOrdersStreaming()
            .delayElements(java.time.Duration.ofMillis(100));
    }

    @GetMapping(value = "/status/{status}", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<OrderDTO> getOrdersByStatus(@PathVariable OrderStatus status) {
        return orderService.getOrdersByStatus(status);
    }

    @GetMapping(value = "/customer/{customerId}", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<OrderDTO> getOrdersByCustomerId(@PathVariable String customerId) {
        return orderService.getOrdersByCustomerId(customerId);
    }

    @PostMapping(value = "/batch", produces = MediaType.APPLICATION_NDJSON_VALUE)
    @RequireRole({UserRole.ADMIN})
    public Flux<OrderDTO> createOrdersBatch(@Valid @RequestBody List<OrderDTO> orderDTOs) {
        return orderService.createOrdersBatch(orderDTOs);
    }

    @PutMapping("/{id}/status")
    @RequireRole({UserRole.ADMIN})
    public Mono<ResponseEntity<OrderDTO>> updateOrderStatus(
            @PathVariable @Positive(message = "Order ID must be a positive number") Long id,
            @Valid @RequestBody StatusUpdateDTO statusUpdate) {
        Long orderId = Objects.requireNonNull(id, "Order ID cannot be null");
        return orderService.updateOrderStatus(orderId, statusUpdate.getOrderStatus())
            .map(ResponseEntity::ok);
    }

    @GetMapping(value = "/{id}/watch", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<OrderDTO> watchOrderStatusChanges(@PathVariable Long id) {
        return orderService.watchOrderStatusChanges(id);
    }
}

