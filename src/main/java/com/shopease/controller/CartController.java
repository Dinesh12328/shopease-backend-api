package com.shopease.controller;

import com.shopease.dto.*;
import com.shopease.dto.CartDtos.*;
import com.shopease.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cart;
    @PostMapping("/add") ApiResponse<CartResponse> add(@Valid @RequestBody AddItemRequest request) {
        return ApiResponse.ok("Item added to cart", cart.add(request));
    }
    @GetMapping ApiResponse<CartResponse> get() { return ApiResponse.ok("Cart retrieved", cart.get()); }
    @PutMapping("/items/{itemId}") ApiResponse<CartResponse> update(@PathVariable Long itemId,
            @Valid @RequestBody UpdateItemRequest request) {
        return ApiResponse.ok("Cart item updated", cart.update(itemId, request));
    }
    @DeleteMapping("/items/{itemId}") ApiResponse<CartResponse> remove(@PathVariable Long itemId) {
        return ApiResponse.ok("Cart item removed", cart.remove(itemId));
    }
    @DeleteMapping ApiResponse<Void> clear() { cart.clear(); return ApiResponse.ok("Cart cleared", null); }
}
