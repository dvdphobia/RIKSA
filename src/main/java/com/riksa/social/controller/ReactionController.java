package com.riksa.social.controller;

import com.riksa.social.dto.PostResponse;
import com.riksa.social.dto.ReactInput;
import com.riksa.social.dto.SocialMapper;
import com.riksa.social.service.CategoryService;
import com.riksa.social.service.ReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;
    private final CategoryService categoryService;

    @MutationMapping("react")
    public PostResponse react(@Argument ReactInput input) {
        UUID userId = getCurrentUserId();
        var post = reactionService.react(userId, input.postId(), input.commentId(), input.type());
        return buildPostResponse(post, userId);
    }

    @MutationMapping("removeReaction")
    public PostResponse removeReaction(@Argument UUID postId, @Argument UUID commentId) {
        UUID userId = getCurrentUserId();
        var post = reactionService.removeReaction(userId, postId, commentId);
        return buildPostResponse(post, userId);
    }

    private PostResponse buildPostResponse(com.riksa.social.entity.Post post, UUID userId) {
        var category = categoryService.listCategories().stream()
                .filter(c -> c.getId().equals(post.getCategoryId()))
                .findFirst().orElse(null);

        var categoryResponse = category != null
                ? SocialMapper.toCategoryResponse(category,
                categoryService.getMemberCount(category.getId()),
                categoryService.isMember(userId, category.getId()))
                : null;

        return SocialMapper.toPostResponse(post, null, categoryResponse, null);
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(auth.getName());
    }
}