package com.shopease.service;

import com.shopease.dto.ProductDtos.*;
import com.shopease.entity.Category;
import com.shopease.exception.*;
import com.shopease.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categories;
    @Transactional(readOnly = true)
    public List<CategoryResponse> all() { return categories.findAll().stream().map(this::map).toList(); }
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categories.findByNameIgnoreCase(request.name().trim()).isPresent())
            throw new BadRequestException("Category name already exists");
        Category c = new Category(); c.setName(request.name().trim()); c.setDescription(request.description());
        return map(categories.save(c));
    }
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category c = entity(id);
        categories.findByNameIgnoreCase(request.name().trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new BadRequestException("Category name already exists"); });
        c.setName(request.name().trim()); c.setDescription(request.description());
        return map(categories.save(c));
    }
    @Transactional public void delete(Long id) { categories.delete(entity(id)); }
    private Category entity(Long id) {
        return categories.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }
    private CategoryResponse map(Category c) { return new CategoryResponse(c.getId(), c.getName(), c.getDescription()); }
}
