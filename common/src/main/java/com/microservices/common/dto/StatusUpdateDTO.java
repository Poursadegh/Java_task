package com.microservices.common.dto;

import com.microservices.common.enums.OrderStatus;
import com.microservices.common.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;

public class StatusUpdateDTO {
    @NotNull(message = "Status is required")
    private OrderStatus orderStatus;
    
    @NotNull(message = "Status is required")
    private PaymentStatus paymentStatus;

    public StatusUpdateDTO() {
    }

    public StatusUpdateDTO(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }
    
    public StatusUpdateDTO(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
    
    // Helper method for backward compatibility
    public String getStatus() {
        if (orderStatus != null) {
            return orderStatus.name();
        }
        if (paymentStatus != null) {
            return paymentStatus.name();
        }
        return null;
    }
    
    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }
    
    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
}

