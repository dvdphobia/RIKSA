package com.riksa.social.controller;

import com.riksa.social.dto.*;
import com.riksa.social.entity.Post;
import com.riksa.social.service.CategoryService;
import com.riksa.social.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final CategoryService categoryService;

    @QueryMapping("post")
    public PostResponse post(@Argument UUID id) {
        UUID userId = getCurrentUserId();
        Post post = postService.getPost(id);
        return buildPostResponse(post, userId);
    }

    @MutationMapping("createPost")
    public PostResponse createPost(@Argument CreatePostInput input) {
        UUID userId = getCurrentUserId();
        Post post = postService.createPost(
                userId,
                input.title(),
                input.content(),
                input.mediaUrl(),
                input.categoryId()
        );
        return buildPostResponse(post, userId);
    }

    @MutationMapping("updatePost")
    public PostResponse updatePost(@Argument UpdatePostInput input) {
        UUID userId = getCurrentUserId();
        Post post = postService.updatePost(userId, input.id(), input.title(), input.content());
        return buildPostResponse(post, userId);
    }

    @MutationMapping("deletePost")
    public boolean deletePost(@Argument UUID id) {
        UUID userId = getCurrentUserId();
        postService.deletePost(userId, id);
        return true;
    }

    private PostResponse buildPostResponse(Post post, UUID userId) {
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