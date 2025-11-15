package com.microservices.common.event;

import com.microservices.common.dto.OrderDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class OrderCreatedEvent {
    private Long orderId;
    private String customerId;
    private java.math.BigDecimal amount;
    private LocalDateTime createdAt;
    private String eventId;
    private LocalDateTime eventTimestamp;

    public static OrderCreatedEvent fromOrderDTO(OrderDTO orderDTO) {
        return OrderCreatedEvent.builder()
            .orderId(orderDTO.getId())
            .customerId(orderDTO.getCustomerId())
            .amount(orderDTO.getAmount())
            .createdAt(orderDTO.getCreatedAt())
            .eventId(java.util.UUID.randomUUID().toString())
            .eventTimestamp(LocalDateTime.now())
            .build();
    }
}

