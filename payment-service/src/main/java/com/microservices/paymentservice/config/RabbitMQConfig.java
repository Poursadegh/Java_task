package com.microservices.paymentservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange names
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String PAYMENT_EXCHANGE = "payment.exchange";

    // Queue names
    public static final String ORDER_CREATED_QUEUE = "order.created.queue";
    public static final String ORDER_STATUS_UPDATED_QUEUE = "order.status.updated.queue";
    public static final String PAYMENT_PROCESSED_QUEUE = "payment.processed.queue";

    // Routing keys
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String ORDER_STATUS_UPDATED_ROUTING_KEY = "order.status.updated";
    public static final String PAYMENT_PROCESSED_ROUTING_KEY = "payment.processed";

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    // Order Exchange and Queues
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(ORDER_CREATED_QUEUE).build();
    }

    @Bean
    public Queue orderStatusUpdatedQueue() {
        return QueueBuilder.durable(ORDER_STATUS_UPDATED_QUEUE).build();
    }

    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder
            .bind(orderCreatedQueue())
            .to(orderExchange())
            .with(ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding orderStatusUpdatedBinding() {
        return BindingBuilder
            .bind(orderStatusUpdatedQueue())
            .to(orderExchange())
            .with(ORDER_STATUS_UPDATED_ROUTING_KEY);
    }

    // Payment Exchange and Queues
    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public Queue paymentProcessedQueue() {
        return QueueBuilder.durable(PAYMENT_PROCESSED_QUEUE).build();
    }

    @Bean
    public Binding paymentProcessedBinding() {
        return BindingBuilder
            .bind(paymentProcessedQueue())
            .to(paymentExchange())
            .with(PAYMENT_PROCESSED_ROUTING_KEY);
    }
}

