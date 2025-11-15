package com.microservices.paymentservice.messaging;

import com.microservices.common.event.PaymentProcessedEvent;
import com.microservices.paymentservice.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishPaymentProcessed(PaymentProcessedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_PROCESSED_ROUTING_KEY,
                event
            );
            log.info("Published payment processed event: paymentId={}, orderId={}, status={}",
                event.getPaymentId(), event.getOrderId(), event.getStatus());
        } catch (Exception e) {
            log.error("Failed to publish payment processed event: paymentId={}, orderId={}",
                event.getPaymentId(), event.getOrderId(), e);
        }
    }
}

