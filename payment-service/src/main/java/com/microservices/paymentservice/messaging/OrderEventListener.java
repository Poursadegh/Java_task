package com.microservices.paymentservice.messaging;

import com.microservices.common.event.OrderCreatedEvent;
import com.microservices.common.event.OrderStatusUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    @RabbitListener(queues = "${spring.rabbitmq.listener.simple.order-created-queue:order.created.queue}")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received order created event: orderId={}, customerId={}, amount={}",
            event.getOrderId(), event.getCustomerId(), event.getAmount());
        // Payment service can react to new orders if needed
        // For example, send notification, update cache, etc.
    }

    @RabbitListener(queues = "${spring.rabbitmq.listener.simple.order-status-updated-queue:order.status.updated.queue}")
    public void handleOrderStatusUpdated(OrderStatusUpdatedEvent event) {
        log.info("Received order status updated event: orderId={}, oldStatus={}, newStatus={}",
            event.getOrderId(), event.getOldStatus(), event.getNewStatus());
        // Payment service can react to order status changes
        // For example, cancel payment if order is cancelled
    }
}

