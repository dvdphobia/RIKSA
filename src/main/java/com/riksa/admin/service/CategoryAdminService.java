package com.riksa.admin.service;

import com.riksa.admin.dto.CategoryRequest;
import com.riksa.admin.dto.CategoryResponse;
import com.riksa.social.entity.Category;
import com.riksa.social.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryAdminService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse create(CategoryRequest req) {
        if (categoryRepository.existsByName(req.getName())) {
            throw new IllegalArgumentException("Category name already exists: " + req.getName());
        }
        if (categoryRepository.existsBySlug(req.getSlug())) {
            throw new IllegalArgumentException("Category slug already exists: " + req.getSlug());
        }

        Category category = new Category();
        category.setName(req.getName());
        category.setSlug(req.getSlug());
        category.setDescription(req.getDescription());
        category.setIconUrl(req.getIconUrl());
        category = categoryRepository.save(category);

        return toResponse(category);
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest req) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));

        // Check name uniqueness if changed (exclude self)
        if (!category.getName().equals(req.getName()) && categoryRepository.existsByName(req.getName())) {
            throw new IllegalArgumentException("Category name already exists: " + req.getName());
        }
        // Check slug uniqueness if changed (exclude self)
        if (!category.getSlug().equals(req.getSlug()) && categoryRepository.existsBySlug(req.getSlug())) {
            throw new IllegalArgumentException("Category slug already exists: " + req.getSlug());
        }

        category.setName(req.getName());
        category.setSlug(req.getSlug());
        category.setDescription(req.getDescription());
        category.setIconUrl(req.getIconUrl());
        category = categoryRepository.save(category);

        return toResponse(category);
    }

    @Transactional
    public void delete(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
        // lessons.category_id has ON DELETE SET NULL — safe to delete
        categoryRepository.delete(category);
    }

    private CategoryResponse toResponse(Category category) {
        CategoryResponse resp = new CategoryResponse();
        resp.setId(category.getId());
        resp.setName(category.getName());
        resp.setSlug(category.getSlug());
        resp.setDescription(category.getDescription());
        resp.setIconUrl(category.getIconUrl());
        resp.setCreatedAt(category.getCreatedAt());
        resp.setUpdatedAt(category.getUpdatedAt());
        return resp;
    }
}