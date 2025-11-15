package com.microservices.paymentservice.service;

import com.microservices.common.dto.PaymentDTO;
import com.microservices.common.dto.StatusUpdateDTO;
import com.microservices.common.enums.OrderStatus;
import com.microservices.common.enums.PaymentMethod;
import com.microservices.common.enums.PaymentStatus;
import com.microservices.common.event.PaymentProcessedEvent;
import com.microservices.common.exception.BusinessException;
import com.microservices.common.exception.ResourceNotFoundException;
import com.microservices.paymentservice.messaging.PaymentEventPublisher;
import com.microservices.paymentservice.model.Payment;
import com.microservices.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderServiceClient orderServiceClient;
    private final PlatformTransactionManager transactionManager;
    private final PaymentEventPublisher paymentEventPublisher;

    public Mono<PaymentDTO> processPayment(PaymentDTO paymentDTO, String token) {
        log.info("Processing payment for orderId: {}", paymentDTO.getOrderId());
        
        return orderServiceClient.getOrder(paymentDTO.getOrderId(), token)
            .flatMap(order -> {
                if (paymentDTO.getAmount().compareTo(order.getAmount()) != 0) {
                    return Mono.error(new BusinessException("PAYMENT_AMOUNT_MISMATCH", 
                        "Payment amount does not match order amount"));
                }
                
                Payment payment = new Payment(
                    paymentDTO.getOrderId(),
                    paymentDTO.getAmount(),
                    paymentDTO.getPaymentMethod(),
                    PaymentStatus.PENDING
                );
                
                TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
                txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
                return Mono.fromCallable(() -> txTemplate.execute(status -> 
                    paymentRepository.save(payment)))
                    .subscribeOn(Schedulers.boundedElastic());
            })
            .flatMap(savedPayment -> {
                return Mono.delay(java.time.Duration.ofMillis(100))
                    .then(Mono.fromCallable(() -> {
                        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
                        txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
                        return txTemplate.execute(status -> {
                            savedPayment.setStatus(PaymentStatus.COMPLETED);
                            return paymentRepository.save(savedPayment);
                        });
                    }))
                    .subscribeOn(Schedulers.boundedElastic());
            })
            .flatMap(completedPayment -> {
                // Publish payment processed event
                PaymentProcessedEvent event = PaymentProcessedEvent.create(
                    completedPayment.getId(),
                    completedPayment.getOrderId(),
                    completedPayment.getAmount(),
                    completedPayment.getStatus()
                );
                paymentEventPublisher.publishPaymentProcessed(event);
                
                StatusUpdateDTO statusUpdate = new StatusUpdateDTO(OrderStatus.PAID);
                
                return orderServiceClient.updateOrderStatus(
                    paymentDTO.getOrderId(), 
                    statusUpdate,
                    token
                )
                .thenReturn(completedPayment)
                .onErrorResume(throwable -> {
                    log.error("Failed to update order status", throwable);
                    return Mono.fromCallable(() -> {
                        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
                        txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
                        return txTemplate.execute(status -> {
                            completedPayment.setStatus(PaymentStatus.FAILED);
                            Payment failedPayment = paymentRepository.save(completedPayment);
                            // Publish failed payment event
                            PaymentProcessedEvent failedEvent = PaymentProcessedEvent.create(
                                failedPayment.getId(),
                                failedPayment.getOrderId(),
                                failedPayment.getAmount(),
                                PaymentStatus.FAILED
                            );
                            paymentEventPublisher.publishPaymentProcessed(failedEvent);
                            return failedPayment;
                        });
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .then(Mono.error(new BusinessException("ORDER_STATUS_UPDATE_FAILED", 
                        "Failed to update order status", throwable)));
                });
            })
            .map(this::convertToDTO)
            .doOnError(error -> log.error("Payment processing failed", error));
    }

    public Mono<PaymentDTO> getPaymentById(@NonNull Long id) {
        Long paymentId = Objects.requireNonNull(id, "Payment ID cannot be null");
        return Mono.fromCallable(() -> paymentRepository.findById(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId)))
        .subscribeOn(Schedulers.boundedElastic())
        .map(this::convertToDTO);
    }

    public Mono<Page<PaymentDTO>> getAllPayments(Pageable pageable) {
        return Mono.fromCallable(() -> paymentRepository.findAll(pageable)
            .map(this::convertToDTO))
        .subscribeOn(Schedulers.boundedElastic());
    }

    public Flux<PaymentDTO> getAllPaymentsStreaming() {
        return Flux.fromIterable(paymentRepository.findAll())
            .map(this::convertToDTO)
            .subscribeOn(Schedulers.boundedElastic())
            .doOnNext(payment -> log.debug("Streaming payment: {}", payment.getId()));
    }

    public Mono<PaymentDTO> updatePaymentStatus(@NonNull Long id, PaymentStatus status) {
        Long paymentId = Objects.requireNonNull(id, "Payment ID cannot be null");
        return Mono.fromCallable(() -> {
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
            return txTemplate.execute(transactionStatus -> {
                Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
                payment.setStatus(status);
                return paymentRepository.save(payment);
            });
        })
        .subscribeOn(Schedulers.boundedElastic())
        .map(this::convertToDTO);
    }

    public Flux<PaymentDTO> processPaymentsBatch(List<PaymentDTO> paymentDTOs, String token) {
        log.info("Processing {} payments in parallel", paymentDTOs.size());
        
        return Flux.fromIterable(paymentDTOs)
            .flatMap(paymentDTO -> 
                processPayment(paymentDTO, token)
                    .onErrorResume(error -> {
                        log.error("Failed to process payment for orderId: {}", paymentDTO.getOrderId(), error);
                        PaymentDTO failedPayment = new PaymentDTO();
                        failedPayment.setOrderId(paymentDTO.getOrderId());
                        failedPayment.setAmount(paymentDTO.getAmount());
                        failedPayment.setPaymentMethod(paymentDTO.getPaymentMethod());
                        failedPayment.setStatus(PaymentStatus.FAILED);
                        return Mono.just(failedPayment);
                    })
            )
            .parallel()
            .runOn(Schedulers.parallel())
            .sequential();
    }

    public Mono<PaymentDTO> processPaymentWithBackpressure(PaymentDTO paymentDTO, String token) {
        return orderServiceClient.getOrder(paymentDTO.getOrderId(), token)
            .flatMap(order -> {
                if (paymentDTO.getAmount().compareTo(order.getAmount()) != 0) {
                    return Mono.error(new BusinessException("PAYMENT_AMOUNT_MISMATCH", 
                        "Payment amount does not match order amount"));
                }
                
                Payment payment = new Payment(
                    paymentDTO.getOrderId(),
                    paymentDTO.getAmount(),
                    paymentDTO.getPaymentMethod(),
                    PaymentStatus.PENDING
                );
                
                TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
                txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
                return Mono.fromCallable(() -> txTemplate.execute(status -> 
                    paymentRepository.save(payment)))
                    .subscribeOn(Schedulers.boundedElastic());
            })
            .flatMap(savedPayment -> {
                return Mono.delay(java.time.Duration.ofMillis(100))
                    .then(Mono.fromCallable(() -> {
                        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
                        txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
                        return txTemplate.execute(status -> {
                            savedPayment.setStatus(PaymentStatus.COMPLETED);
                            return paymentRepository.save(savedPayment);
                        });
                    }))
                    .subscribeOn(Schedulers.boundedElastic());
            })
            .flatMap(completedPayment -> {
                // Publish payment processed event
                PaymentProcessedEvent event = PaymentProcessedEvent.create(
                    completedPayment.getId(),
                    completedPayment.getOrderId(),
                    completedPayment.getAmount(),
                    completedPayment.getStatus()
                );
                paymentEventPublisher.publishPaymentProcessed(event);
                
                StatusUpdateDTO statusUpdate = new StatusUpdateDTO(OrderStatus.PAID);
                
                return orderServiceClient.updateOrderStatus(
                    paymentDTO.getOrderId(), 
                    statusUpdate,
                    token
                )
                .thenReturn(completedPayment)
                .onErrorResume(throwable -> {
                    log.error("Failed to update order status", throwable);
                    return Mono.fromCallable(() -> {
                        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
                        txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
                        return txTemplate.execute(status -> {
                            completedPayment.setStatus(PaymentStatus.FAILED);
                            Payment failedPayment = paymentRepository.save(completedPayment);
                            // Publish failed payment event
                            PaymentProcessedEvent failedEvent = PaymentProcessedEvent.create(
                                failedPayment.getId(),
                                failedPayment.getOrderId(),
                                failedPayment.getAmount(),
                                PaymentStatus.FAILED
                            );
                            paymentEventPublisher.publishPaymentProcessed(failedEvent);
                            return failedPayment;
                        });
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .then(Mono.error(new BusinessException("ORDER_STATUS_UPDATE_FAILED", 
                        "Failed to update order status", throwable)));
                });
            })
            .map(this::convertToDTO)
            .doOnError(error -> log.error("Payment processing failed", error));
    }

    private PaymentDTO convertToDTO(Payment payment) {
        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getId());
        dto.setOrderId(payment.getOrderId());
        dto.setAmount(payment.getAmount());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setStatus(payment.getStatus());
        dto.setTransactionId(payment.getTransactionId());
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setUpdatedAt(payment.getUpdatedAt());
        return dto;
    }
}
