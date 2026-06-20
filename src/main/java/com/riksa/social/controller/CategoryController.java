package com.riksa.social.controller;

import com.riksa.social.dto.CategoryResponse;
import com.riksa.social.dto.SocialMapper;
import com.riksa.social.entity.Category;
import com.riksa.social.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @QueryMapping("categories")
    public List<CategoryResponse> categories() {
        UUID userId = getCurrentUserId();
        return categoryService.listCategories().stream()
                .map(c -> SocialMapper.toCategoryResponse(
                        c,
                        categoryService.getMemberCount(c.getId()),
                        categoryService.isMember(userId, c.getId())
                ))
                .toList();
    }

    @MutationMapping("joinCategory")
    public CategoryResponse joinCategory(@Argument UUID categoryId) {
        UUID userId = getCurrentUserId();
        Category category = categoryService.joinCategory(userId, categoryId);
        return SocialMapper.toCategoryResponse(
                category,
                categoryService.getMemberCount(categoryId),
                true
        );
    }

    @MutationMapping("leaveCategory")
    public CategoryResponse leaveCategory(@Argument UUID categoryId) {
        UUID userId = getCurrentUserId();
        Category category = categoryService.leaveCategory(userId, categoryId);
        return SocialMapper.toCategoryResponse(
                category,
                categoryService.getMemberCount(categoryId),
                false
        );
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(auth.getName());
    }
}