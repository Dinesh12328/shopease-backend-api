package com.shopease.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public final class CartDtos {
    private CartDtos() {}
    public record AddItemRequest(@NotNull Long productId, @NotNull @Min(1) Integer quantity) {}
    public record UpdateItemRequest(@NotNull @Min(1) Integer quantity) {}
    public record CartItemResponse(Long id, Long productId, String productName, BigDecimal unitPrice,
            Integer quantity, BigDecimal subtotal, String imageUrl) {}
    public record CartResponse(Long id, List<CartItemResponse> items, BigDecimal total) {}
}
