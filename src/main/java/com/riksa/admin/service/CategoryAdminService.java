package com.riksa.admin.service;

import com.riksa.admin.dto.CategoryRequest;
import com.riksa.admin.dto.CategoryResponse;
import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.learning.service.LessonBackgrounds;
import com.riksa.social.entity.Category;
import com.riksa.social.repository.CategoryRepository;
import com.riksa.social.service.UploadedMediaUrlValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryAdminService {

    private final CategoryRepository categoryRepository;
    private final UploadedMediaUrlValidator uploadedMediaUrlValidator;

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(UUID id) {
        return categoryRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

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
        applyBackground(category, req);
        category.setOfficial(true);
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
        applyBackground(category, req);
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

    private void applyBackground(Category category, CategoryRequest req) {
        category.setBackgroundKey(LessonBackgrounds.normalizeKey(req.getBackgroundKey()));
        category.setBackgroundImageUrl(normalizeImage(req.getBackgroundImageUrl()));
    }

    private String normalizeImage(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return uploadedMediaUrlValidator.normalize(raw);
    }

    private CategoryResponse toResponse(Category category) {
        CategoryResponse resp = new CategoryResponse();
        resp.setId(category.getId());
        resp.setName(category.getName());
        resp.setSlug(category.getSlug());
        resp.setDescription(category.getDescription());
        resp.setIconUrl(category.getIconUrl());
        resp.setBackgroundKey(category.getBackgroundKey());
        resp.setBackgroundImageUrl(category.getBackgroundImageUrl());
        resp.setCreatedAt(category.getCreatedAt());
        resp.setUpdatedAt(category.getUpdatedAt());
        return resp;
    }
}