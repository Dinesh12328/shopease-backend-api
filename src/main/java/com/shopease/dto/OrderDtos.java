package com.shopease.dto;

import com.shopease.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {
    private OrderDtos() {}
    public record PlaceOrderRequest(@NotBlank @Size(max = 500) String shippingAddress,
                                    @NotNull PaymentMethod paymentMethod) {}
    public record OrderItemResponse(Long productId, String productName, BigDecimal price,
                                    Integer quantity, BigDecimal subtotal) {}
    public record PaymentResponse(Long id, PaymentMethod method, PaymentStatus status,
                                  BigDecimal amount, String transactionId) {}
    public record OrderResponse(Long id, List<OrderItemResponse> items, BigDecimal totalAmount,
                                String shippingAddress, OrderStatus status, Instant createdAt,
                                PaymentResponse payment) {}
    public record UpdateStatusRequest(@NotNull OrderStatus status) {}
}
