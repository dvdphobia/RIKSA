package com.riksa.social.controller;

import com.riksa.common.constants.SocialEndpoints;
import com.riksa.social.dto.*;
import com.riksa.social.entity.Post;
import com.riksa.social.repository.PostMediaRepository;
import com.riksa.social.service.PostService;
import com.riksa.social.service.ShareService;
import com.riksa.social.service.SocialResponseResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final ShareService shareService;
    private final SocialResponseResolver resolver;
    private final PostMediaRepository postMediaRepository;

    @QueryMapping(SocialEndpoints.QUERY_POST)
    public PostResponse post(@Argument UUID id) {
        UUID userId = getCurrentUserId();
        Post post = postService.getVisiblePost(id);
        return resolver.resolvePost(post, userId);
    }

    @MutationMapping(SocialEndpoints.MUTATION_CREATE_POST)
    public PostResponse createPost(@Argument CreatePostInput input) {
        UUID userId = getCurrentUserId();
        Post post = postService.createPost(
                userId,
                input.title(),
                input.content(),
                input.mediaUrl(),
                input.media(),
                input.categoryId()
        );
        return resolver.resolvePost(post, userId);
    }

    @MutationMapping(SocialEndpoints.MUTATION_UPDATE_POST)
    public PostResponse updatePost(@Argument UpdatePostInput input) {
        UUID userId = getCurrentUserId();
        Post post = postService.updatePost(
                userId, input.id(), input.title(), input.content(), input.mediaUrl(), input.media());
        return resolver.resolvePost(post, userId);
    }

    @MutationMapping(SocialEndpoints.MUTATION_DELETE_POST)
    public boolean deletePost(@Argument UUID id) {
        UUID userId = getCurrentUserId();
        postService.deletePost(userId, id);
        return true;
    }

    @MutationMapping(SocialEndpoints.MUTATION_SHARE_POST)
    public PostResponse sharePost(@Argument SharePostInput input) {
        UUID userId = getCurrentUserId();
        Post post = shareService.sharePost(userId, input.postId(), input.platform());
        return resolver.resolvePost(post, userId);
    }

    // ── Field resolver for nested comments on Post ──────────

    @SchemaMapping(field = "comments", typeName = "Post")
    public CommentPage postComments(PostResponse post, @Argument UUID cursor, @Argument Integer limit) {
        UUID userId = getCurrentUserId();
        int pageLimit = (limit != null && limit > 0) ? Math.min(limit, 50) : 10;
        return resolver.resolveComments(post.id(), userId, pageLimit);
    }

    /** Loads all media for the current post page with one IN query. */
    @BatchMapping(typeName = "Post", field = "media")
    public Map<PostResponse, List<PostMediaResponse>> postMedia(List<PostResponse> posts) {
        if (posts.isEmpty()) {
            return Map.of();
        }

        Map<UUID, List<PostMediaResponse>> mediaByPostId = postMediaRepository.findAllByPostIds(
                        posts.stream().map(PostResponse::id).distinct().toList())
                .stream()
                .collect(Collectors.groupingBy(
                        media -> media.getId().getPostId(),
                        LinkedHashMap::new,
                        Collectors.mapping(media -> new PostMediaResponse(
                                media.getUrl(), media.getWidth(), media.getHeight()), Collectors.toList())));

        Map<PostResponse, List<PostMediaResponse>> result = new LinkedHashMap<>();
        for (PostResponse post : posts) {
            result.put(post, mediaByPostId.getOrDefault(post.id(), List.of()));
        }
        return result;
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(auth.getName());
    }
}
