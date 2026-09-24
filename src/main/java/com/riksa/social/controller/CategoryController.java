package com.riksa.social.controller;

import com.riksa.common.constants.SocialEndpoints;
import com.riksa.social.dto.CategoryResponse;
import com.riksa.social.dto.CreateCommunityInput;
import com.riksa.social.dto.SocialMapper;
import com.riksa.social.entity.Category;
import com.riksa.social.service.CategoryService;
import com.riksa.social.service.CommunityCreationService;
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
    private final CommunityCreationService communityCreationService;

    @QueryMapping(SocialEndpoints.QUERY_CATEGORIES)
    public List<CategoryResponse> categories() {
        UUID userId = getCurrentUserId();
        return categoryService.listCategories().stream()
                .map(c -> SocialMapper.toCategoryResponse(
                        c,
                        categoryService.getMemberCount(c.getId()),
                        categoryService.isMember(userId, c.getId()),
                        userId
                ))
                .toList();
    }

    @MutationMapping(SocialEndpoints.MUTATION_CREATE_COMMUNITY)
    public CategoryResponse createCommunity(@Argument CreateCommunityInput input) {
        UUID userId = getCurrentUserId();
        Category category = communityCreationService.create(
                userId, input.name(), input.description(), input.iconUrl());
        return SocialMapper.toCategoryResponse(category, 1, true, userId);
    }

    @MutationMapping(SocialEndpoints.MUTATION_JOIN_CATEGORY)
    public CategoryResponse joinCategory(@Argument UUID categoryId) {
        UUID userId = getCurrentUserId();
        Category category = categoryService.joinCategory(userId, categoryId);
        return SocialMapper.toCategoryResponse(
                category,
                categoryService.getMemberCount(categoryId),
                true,
                userId
        );
    }

    @MutationMapping(SocialEndpoints.MUTATION_LEAVE_CATEGORY)
    public CategoryResponse leaveCategory(@Argument UUID categoryId) {
        UUID userId = getCurrentUserId();
        Category category = categoryService.leaveCategory(userId, categoryId);
        return SocialMapper.toCategoryResponse(
                category,
                categoryService.getMemberCount(categoryId),
                false,
                userId
        );
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        try {
            return UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}