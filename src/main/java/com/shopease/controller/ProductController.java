package com.shopease.controller;

import com.shopease.dto.*;
import com.shopease.dto.ProductDtos.ProductResponse;
import com.shopease.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService products;
    @GetMapping
    ApiResponse<Page<ProductResponse>> search(@RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId, @RequestParam(required = false) String brand,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok("Products retrieved", products.search(name, categoryId, brand, pageable));
    }
    @GetMapping("/{id}")
    ApiResponse<ProductResponse> get(@PathVariable Long id) {
        return ApiResponse.ok("Product retrieved", products.get(id));
    }
}
