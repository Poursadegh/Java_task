package com.microservices.common.event;

import com.microservices.common.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdatedEvent {
    private Long orderId;
    private OrderStatus oldStatus;
    private OrderStatus newStatus;
    private LocalDateTime updatedAt;
    private String eventId;
    private LocalDateTime eventTimestamp;

    public static OrderStatusUpdatedEvent create(Long orderId, OrderStatus oldStatus, OrderStatus newStatus) {
        return OrderStatusUpdatedEvent.builder()
            .orderId(orderId)
            .oldStatus(oldStatus)
            .newStatus(newStatus)
            .updatedAt(LocalDateTime.now())
            .eventId(java.util.UUID.randomUUID().toString())
            .eventTimestamp(LocalDateTime.now())
            .build();
    }
}

