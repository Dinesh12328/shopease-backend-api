package com.shopease.controller;

import com.shopease.dto.*;
import com.shopease.dto.OrderDtos.*;
import com.shopease.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orders;
    @PostMapping("/place")
    ResponseEntity<ApiResponse<OrderResponse>> place(@Valid @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Order placed", orders.place(request)));
    }
    @GetMapping("/user") ApiResponse<Page<OrderResponse>> history(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok("Order history retrieved", orders.history(pageable));
    }
    @GetMapping("/{id}") ApiResponse<OrderResponse> get(@PathVariable Long id) {
        return ApiResponse.ok("Order retrieved", orders.get(id));
    }
}
