package com.microservices.orderservice.messaging;

import com.microservices.common.event.OrderCreatedEvent;
import com.microservices.common.event.OrderStatusUpdatedEvent;
import com.microservices.orderservice.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                event
            );
            log.info("Published order created event: orderId={}, customerId={}, amount={}",
                event.getOrderId(), event.getCustomerId(), event.getAmount());
        } catch (Exception e) {
            log.error("Failed to publish order created event: orderId={}",
                event.getOrderId(), e);
        }
    }

    public void publishOrderStatusUpdated(OrderStatusUpdatedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_STATUS_UPDATED_ROUTING_KEY,
                event
            );
            log.info("Published order status updated event: orderId={}, oldStatus={}, newStatus={}",
                event.getOrderId(), event.getOldStatus(), event.getNewStatus());
        } catch (Exception e) {
            log.error("Failed to publish order status updated event: orderId={}",
                event.getOrderId(), e);
        }
    }
}

