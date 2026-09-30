package com.riksa.social.controller;

import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.social.dto.CategoryResponse;
import com.riksa.social.dto.CommunityBanResponse;
import com.riksa.social.dto.CommunityMemberResponse;
import com.riksa.social.dto.CommunityRuleResponse;
import com.riksa.social.dto.SocialMapper;
import com.riksa.social.dto.UpdateCommunityInput;
import com.riksa.social.dto.UserResponse;
import com.riksa.social.entity.Category;
import com.riksa.social.entity.CommunityBan;
import com.riksa.social.entity.UserCategoryMembership;
import com.riksa.social.enums.CommunityRole;
import com.riksa.social.enums.PostPermission;
import com.riksa.social.service.CategoryService;
import com.riksa.social.service.CommunityModerationService;
import com.riksa.social.service.CommunityModerationService.RuleInput;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** A founder's tools for running their community. */
@Controller
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityModerationService moderation;
    private final CategoryService categoryService;
    private final UserRepository userRepository;

    // ── Category fields ─────────────────────────────────

    @SchemaMapping(typeName = "Category", field = "myRole")
    public String myRole(CategoryResponse category) {
        UUID userId = currentUserId();
        if (userId == null) return null;
        if (category.ownedByMe()) return CommunityRole.OWNER.name();
        if (!category.joined()) return null;
        CommunityRole role = moderation.roleOf(userId, moderation.getCommunity(category.id()));
        return role == null ? null : role.name();
    }

    @SchemaMapping(typeName = "Category", field = "rules")
    public List<CommunityRuleResponse> rules(CategoryResponse category) {
        return moderation.rules(category.id()).stream()
                .map(rule -> new CommunityRuleResponse(rule.getTitle(), rule.getBody()))
                .toList();
    }

    // ── Queries ─────────────────────────────────────────

    @QueryMapping
    public CategoryResponse community(@Argument UUID id) {
        return categoryService.listCategories().stream()
                .filter(category -> category.getId().equals(id))
                .findFirst()
                .map(this::toResponse)
                .orElse(null);
    }

    @QueryMapping
    public List<CommunityMemberResponse> communityMembers(@Argument UUID categoryId,
                                                          @Argument String query,
                                                          @Argument Integer limit) {
        Category category = moderation.getCommunity(categoryId);
        List<UserCategoryMembership> rows = moderation.members(categoryId, query, limit);
        Map<UUID, UserResponse> users = users(rows.stream().map(UserCategoryMembership::getUserId).toList());
        return rows.stream()
                .filter(row -> users.containsKey(row.getUserId()))
                .map(row -> new CommunityMemberResponse(
                        users.get(row.getUserId()),
                        (row.getUserId().equals(category.getCreatedBy()) ? CommunityRole.OWNER : row.getRole()).name(),
                        row.getJoinedAt()))
                .toList();
    }

    @QueryMapping
    public List<CommunityBanResponse> communityBans(@Argument UUID categoryId) {
        List<CommunityBan> rows = moderation.bans(requireUser(), categoryId);
        Map<UUID, UserResponse> users = users(rows.stream().map(CommunityBan::getUserId).toList());
        return rows.stream()
                .filter(row -> users.containsKey(row.getUserId()))
                .map(row -> new CommunityBanResponse(users.get(row.getUserId()), row.getReason(), row.getCreatedAt()))
                .toList();
    }

    // ── Mutations ───────────────────────────────────────

    @MutationMapping
    public CategoryResponse updateCommunity(@Argument UpdateCommunityInput input) {
        List<RuleInput> rules = input.rules() == null ? null : input.rules().stream()
                .map(rule -> new RuleInput(rule.title(), rule.body()))
                .toList();
        Category category = moderation.updateCommunity(
                requireUser(), input.id(), input.name(), input.description(), input.iconUrl(),
                Boolean.TRUE.equals(input.clearIcon()),
                input.postPermission() == null ? null : PostPermission.valueOf(input.postPermission()),
                rules,
                input.bannerUrl(), Boolean.TRUE.equals(input.clearBanner()), input.bannerColor());
        return toResponse(category);
    }

    @MutationMapping
    public CommunityMemberResponse setCommunityMemberRole(@Argument UUID categoryId,
                                                          @Argument UUID userId,
                                                          @Argument String role) {
        UserCategoryMembership row = moderation.setRole(
                requireUser(), categoryId, userId, CommunityRole.valueOf(role));
        UserResponse user = users(List.of(userId)).get(userId);
        return new CommunityMemberResponse(user, row.getRole().name(), row.getJoinedAt());
    }

    @MutationMapping
    public boolean removeCommunityMember(@Argument UUID categoryId, @Argument UUID userId,
                                         @Argument Boolean ban, @Argument String reason) {
        moderation.removeMember(requireUser(), categoryId, userId, Boolean.TRUE.equals(ban), reason);
        return true;
    }

    @MutationMapping
    public boolean unbanCommunityMember(@Argument UUID categoryId, @Argument UUID userId) {
        moderation.unban(requireUser(), categoryId, userId);
        return true;
    }

    @MutationMapping
    public CategoryResponse pinCommunityPost(@Argument UUID categoryId, @Argument UUID postId) {
        return toResponse(moderation.pinPost(requireUser(), categoryId, postId));
    }

    // ── Helpers ─────────────────────────────────────────

    private CategoryResponse toResponse(Category category) {
        UUID userId = currentUserId();
        return SocialMapper.toCategoryResponse(
                category,
                categoryService.getMemberCount(category.getId()),
                categoryService.isMember(userId, category.getId()),
                userId);
    }

    private Map<UUID, UserResponse> users(Collection<UUID> ids) {
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, SocialMapper::toUserResponse,
                        (a, b) -> a));
    }

    private UUID requireUser() {
        UUID userId = currentUserId();
        if (userId == null) {
            throw new org.springframework.security.access.AccessDeniedException("Sign in first");
        }
        return userId;
    }

    private UUID currentUserId() {
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
