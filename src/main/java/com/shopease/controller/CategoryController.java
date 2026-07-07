package com.shopease.controller;

import com.shopease.dto.*;
import com.shopease.dto.ProductDtos.CategoryResponse;
import com.shopease.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categories;
    @GetMapping ApiResponse<List<CategoryResponse>> all() {
        return ApiResponse.ok("Categories retrieved", categories.all());
    }
}
