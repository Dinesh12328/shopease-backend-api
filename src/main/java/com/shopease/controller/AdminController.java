package com.shopease.controller;

import com.shopease.dto.*;
import com.shopease.dto.OrderDtos.*;
import com.shopease.dto.ProductDtos.*;
import com.shopease.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final ProductService products;
    private final CategoryService categories;
    private final OrderService orders;
    private final FileStorageService storage;

    @PostMapping("/products")
    ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Product created", products.create(request)));
    }
    @PutMapping("/products/{id}") ApiResponse<ProductResponse> updateProduct(@PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok("Product updated", products.update(id, request));
    }
    @DeleteMapping("/products/{id}") ApiResponse<Void> deleteProduct(@PathVariable Long id) {
        products.delete(id); return ApiResponse.ok("Product deleted", null);
    }
    @PostMapping(value = "/products/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<ProductResponse> uploadImage(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok("Product image uploaded", products.setImage(id, storage.store(file)));
    }
    @PostMapping("/categories")
    ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Category created", categories.create(request)));
    }
    @PutMapping("/categories/{id}") ApiResponse<CategoryResponse> updateCategory(@PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok("Category updated", categories.update(id, request));
    }
    @DeleteMapping("/categories/{id}") ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        categories.delete(id); return ApiResponse.ok("Category deleted", null);
    }
    @GetMapping("/orders") ApiResponse<Page<OrderResponse>> allOrders(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok("Orders retrieved", orders.all(pageable));
    }
    @PatchMapping("/orders/{id}/status") ApiResponse<OrderResponse> updateStatus(@PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        return ApiResponse.ok("Order status updated", orders.updateStatus(id, request.status()));
    }
}
