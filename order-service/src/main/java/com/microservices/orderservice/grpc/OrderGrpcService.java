package com.microservices.orderservice.grpc;

import com.microservices.common.dto.OrderDTO;
import com.microservices.common.exception.ResourceNotFoundException;
import com.microservices.orderservice.service.OrderService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

@Slf4j
@GrpcService
@Component
@RequiredArgsConstructor
public class OrderGrpcService extends OrderServiceGrpc.OrderServiceImplBase {

    private final OrderService orderService;

    @Override
    public void getOrder(GetOrderRequest request, StreamObserver<GetOrderResponse> responseObserver) {
        try {
            log.debug("gRPC GetOrder request received for orderId: {}", request.getOrderId());
            
            OrderDTO orderDTO = orderService.getOrderById(request.getOrderId())
                .block(); // Convert Mono to blocking call for gRPC
            
            if (orderDTO == null) {
                GetOrderResponse response = GetOrderResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage("Order not found")
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            Order grpcOrder = convertToGrpcOrder(orderDTO);
            GetOrderResponse response = GetOrderResponse.newBuilder()
                .setOrder(grpcOrder)
                .setSuccess(true)
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.debug("gRPC GetOrder response sent for orderId: {}", request.getOrderId());
        } catch (ResourceNotFoundException e) {
            log.warn("Order not found: {}", request.getOrderId());
            GetOrderResponse response = GetOrderResponse.newBuilder()
                .setSuccess(false)
                .setErrorMessage("Order not found: " + e.getMessage())
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in gRPC GetOrder for orderId: {}", request.getOrderId(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Internal server error: " + e.getMessage())
                .asRuntimeException());
        }
    }

    @Override
    public void updateOrderStatus(UpdateOrderStatusRequest request, StreamObserver<UpdateOrderStatusResponse> responseObserver) {
        try {
            log.debug("gRPC UpdateOrderStatus request received for orderId: {}, status: {}", 
                request.getOrderId(), request.getStatus());
            
            com.microservices.common.enums.OrderStatus orderStatus = convertToOrderStatus(request.getStatus());
            OrderDTO orderDTO = orderService.updateOrderStatus(request.getOrderId(), orderStatus)
                .block(); // Convert Mono to blocking call for gRPC

            Order grpcOrder = convertToGrpcOrder(orderDTO);
            UpdateOrderStatusResponse response = UpdateOrderStatusResponse.newBuilder()
                .setOrder(grpcOrder)
                .setSuccess(true)
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.debug("gRPC UpdateOrderStatus response sent for orderId: {}", request.getOrderId());
        } catch (ResourceNotFoundException e) {
            log.warn("Order not found: {}", request.getOrderId());
            UpdateOrderStatusResponse response = UpdateOrderStatusResponse.newBuilder()
                .setSuccess(false)
                .setErrorMessage("Order not found: " + e.getMessage())
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in gRPC UpdateOrderStatus for orderId: {}", request.getOrderId(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Internal server error: " + e.getMessage())
                .asRuntimeException());
        }
    }

    private Order convertToGrpcOrder(OrderDTO orderDTO) {
        Order.Builder builder = Order.newBuilder()
            .setId(orderDTO.getId())
            .setCustomerId(orderDTO.getCustomerId())
            .setAmount(orderDTO.getAmount().toString())
            .setStatus(convertToGrpcStatus(orderDTO.getStatus()));

        if (orderDTO.getDescription() != null) {
            builder.setDescription(orderDTO.getDescription());
        }

        if (orderDTO.getCreatedAt() != null) {
            builder.setCreatedAt(orderDTO.getCreatedAt().atZone(ZoneId.systemDefault()).toEpochSecond());
        }

        if (orderDTO.getUpdatedAt() != null) {
            builder.setUpdatedAt(orderDTO.getUpdatedAt().atZone(ZoneId.systemDefault()).toEpochSecond());
        }

        return builder.build();
    }

    private OrderStatus convertToGrpcStatus(com.microservices.common.enums.OrderStatus status) {
        return switch (status) {
            case PENDING -> OrderStatus.PENDING;
            case PROCESSING -> OrderStatus.PROCESSING;
            case PAID -> OrderStatus.PAID;
            case CANCELLED -> OrderStatus.CANCELLED;
            case COMPLETED -> OrderStatus.COMPLETED;
        };
    }

    private com.microservices.common.enums.OrderStatus convertToOrderStatus(OrderStatus grpcStatus) {
        if (grpcStatus == OrderStatus.UNRECOGNIZED) {
            throw new IllegalArgumentException("Unknown order status: " + grpcStatus);
        }
        return switch (grpcStatus) {
            case PENDING -> com.microservices.common.enums.OrderStatus.PENDING;
            case PROCESSING -> com.microservices.common.enums.OrderStatus.PROCESSING;
            case PAID -> com.microservices.common.enums.OrderStatus.PAID;
            case CANCELLED -> com.microservices.common.enums.OrderStatus.CANCELLED;
            case COMPLETED -> com.microservices.common.enums.OrderStatus.COMPLETED;
            default -> throw new IllegalArgumentException("Unknown order status: " + grpcStatus);
        };
    }
}

