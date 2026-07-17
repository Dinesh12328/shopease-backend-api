package com.shopease.service;

import com.shopease.dto.ProductDtos.*;
import com.shopease.entity.*;
import com.shopease.exception.*;
import com.shopease.repository.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository products;
    private final CategoryRepository categories;

    @Transactional(readOnly = true)
    public Page<ProductResponse> search(String name, Long categoryId, String brand, Pageable pageable) {
        return products.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (name != null && !name.isBlank())
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            if (categoryId != null) predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            if (brand != null && !brand.isBlank())
                predicates.add(cb.like(cb.lower(root.get("brand")), "%" + brand.toLowerCase() + "%"));
            return cb.and(predicates.toArray(Predicate[]::new));
        }, pageable).map(this::map);
    }
    @Transactional(readOnly = true)
    public ProductResponse get(Long id) { return map(entity(id)); }
    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        apply(product, request);
        return map(products.save(product));
    }
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = entity(id);
        apply(product, request);
        return map(products.save(product));
    }
    @Transactional
    public void delete(Long id) {
        Product product = entity(id);
        if (products.quantityReservedInCarts(id) > 0)
            throw new BadRequestException("Product cannot be deleted while it is present in a cart");
        if (products.quantitySoldInOrders(id) > 0)
            throw new BadRequestException("Product cannot be deleted because it is already used in an order");
        products.delete(product);
    }
    @Transactional
    public ProductResponse setImage(Long id, String imageUrl) {
        Product product = entity(id);
        product.setImageUrl(imageUrl);
        return map(products.save(product));
    }
    Product entity(Long id) {
        return products.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }
    private void apply(Product p, ProductRequest r) {
        Category category = categories.findById(r.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + r.categoryId()));
        p.setName(r.name().trim()); p.setDescription(r.description()); p.setPrice(r.price());
        p.setStock(r.stock()); p.setBrand(r.brand()); p.setCategory(category); p.setImageUrl(cleanImageUrl(r.imageUrl()));
    }

    private String cleanImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return null;
        String cleaned = imageUrl.trim();
        if (cleaned.startsWith("//")) return "https:" + cleaned;
        if (cleaned.regionMatches(true, 0, "www.", 0, 4)) return "https://" + cleaned;
        if (looksLikeHostPath(cleaned)) return "https://" + cleaned;
        return cleaned;
    }

    private boolean looksLikeHostPath(String value) {
        return !value.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")
                && value.matches("(?i)^[a-z0-9.-]+\\.[a-z]{2,}([:/?#].*)?$");
    }
    ProductResponse map(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getStock(),
                p.getBrand(), p.getImageUrl(), p.getCategory().getId(), p.getCategory().getName(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
