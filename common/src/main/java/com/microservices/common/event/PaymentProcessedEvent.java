package com.microservices.common.event;

import com.microservices.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessedEvent {
    private Long paymentId;
    private Long orderId;
    private BigDecimal amount;
    private PaymentStatus status;
    private LocalDateTime processedAt;
    private String eventId;
    private LocalDateTime eventTimestamp;

    public static PaymentProcessedEvent create(Long paymentId, Long orderId, BigDecimal amount, PaymentStatus status) {
        return PaymentProcessedEvent.builder()
            .paymentId(paymentId)
            .orderId(orderId)
            .amount(amount)
            .status(status)
            .processedAt(LocalDateTime.now())
            .eventId(java.util.UUID.randomUUID().toString())
            .eventTimestamp(LocalDateTime.now())
            .build();
    }
}

