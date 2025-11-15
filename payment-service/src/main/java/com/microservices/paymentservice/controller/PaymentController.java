package com.microservices.paymentservice.controller;

import com.microservices.common.annotation.RequireRole;
import com.microservices.common.dto.PaymentDTO;
import com.microservices.common.dto.StatusUpdateDTO;
import com.microservices.common.enums.UserRole;
import com.microservices.paymentservice.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/payments")
@RequiredArgsConstructor
@Validated
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @RequireRole({UserRole.USER, UserRole.ADMIN})
    public Mono<ResponseEntity<PaymentDTO>> processPayment(
            @Valid @RequestBody PaymentDTO paymentDTO,
            HttpServletRequest request) {
        String token = (String) request.getAttribute("authToken");
        return paymentService.processPayment(paymentDTO, token)
            .map(payment -> ResponseEntity.status(HttpStatus.CREATED).body(payment));
    }

    @PostMapping("/backpressure")
    public Mono<ResponseEntity<PaymentDTO>> processPaymentWithBackpressure(
            @Valid @RequestBody PaymentDTO paymentDTO,
            HttpServletRequest request) {
        String token = (String) request.getAttribute("authToken");
        return paymentService.processPaymentWithBackpressure(paymentDTO, token)
            .map(payment -> ResponseEntity.status(HttpStatus.CREATED).body(payment));
    }

    @GetMapping("/{id}")
    @RequireRole({UserRole.USER, UserRole.ADMIN})
    public Mono<ResponseEntity<PaymentDTO>> getPayment(
            @PathVariable @Positive(message = "Payment ID must be a positive number") Long id) {
        return paymentService.getPaymentById(id)
            .map(ResponseEntity::ok);
    }

    @GetMapping
    @RequireRole({UserRole.USER, UserRole.ADMIN})
    public Mono<ResponseEntity<Page<PaymentDTO>>> getAllPayments(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return paymentService.getAllPayments(pageable)
            .map(ResponseEntity::ok);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequireRole({UserRole.ADMIN})
    public Flux<PaymentDTO> getAllPaymentsStreaming() {
        return paymentService.getAllPaymentsStreaming()
            .delayElements(java.time.Duration.ofMillis(100));
    }

    @PutMapping("/{id}/status")
    @RequireRole({UserRole.ADMIN})
    public Mono<ResponseEntity<PaymentDTO>> updatePaymentStatus(
            @PathVariable @Positive(message = "Payment ID must be a positive number") Long id,
            @Valid @RequestBody StatusUpdateDTO statusUpdate) {
        Long paymentId = Objects.requireNonNull(id, "Payment ID cannot be null");
        return paymentService.updatePaymentStatus(paymentId, statusUpdate.getPaymentStatus())
            .map(ResponseEntity::ok);
    }

    @PostMapping(value = "/batch", produces = MediaType.APPLICATION_NDJSON_VALUE)
    @RequireRole({UserRole.ADMIN})
    public Flux<PaymentDTO> processPaymentsBatch(
            @Valid @RequestBody List<PaymentDTO> paymentDTOs,
            HttpServletRequest request) {
        String token = (String) request.getAttribute("authToken");
        return paymentService.processPaymentsBatch(paymentDTOs, token);
    }
}

