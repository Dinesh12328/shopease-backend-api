package com.shopease.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

public final class ProductDtos {
    private ProductDtos() {}
    public record ProductRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 2000) String description,
            @NotNull @DecimalMin("0.01") BigDecimal price,
            @NotNull @Min(0) Integer stock,
            @Size(max = 100) String brand,
            @NotNull Long categoryId,
            @Size(max = 5000) String imageUrl) {}
    public record ProductResponse(Long id, String name, String description, BigDecimal price,
            Integer stock, String brand, String imageUrl, Long categoryId, String categoryName,
            Instant createdAt, Instant updatedAt) {}
    public record CategoryRequest(@NotBlank @Size(max = 100) String name, @Size(max = 500) String description) {}
    public record CategoryResponse(Long id, String name, String description) {}
}
