package com.riksa.social.controller;

import com.riksa.common.constants.SocialEndpoints;
import com.riksa.social.dto.CommentPage;
import com.riksa.social.dto.CommentResponse;
import com.riksa.social.dto.CreateCommentInput;
import com.riksa.social.dto.UpdateCommentInput;
import com.riksa.social.service.CommentService;
import com.riksa.social.service.SocialResponseResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final SocialResponseResolver resolver;

    @QueryMapping(SocialEndpoints.QUERY_COMMENTS)
    public CommentPage comments(@Argument UUID postId,
                                @Argument UUID cursor,
                                @Argument Integer limit) {
        UUID userId = getCurrentUserId();
        int pageLimit = (limit != null && limit > 0) ? Math.min(limit, 50) : 20;
        return resolver.resolveComments(postId, userId, pageLimit);
    }

    @MutationMapping(SocialEndpoints.MUTATION_CREATE_COMMENT)
    public CommentResponse createComment(@Argument CreateCommentInput input) {
        UUID userId = getCurrentUserId();
        var comment = commentService.createComment(
                userId,
                input.postId(),
                input.content(),
                input.parentId()
        );
        return resolver.resolveComment(comment, userId);
    }

    @MutationMapping(SocialEndpoints.MUTATION_UPDATE_COMMENT)
    public CommentResponse updateComment(@Argument UpdateCommentInput input) {
        UUID userId = getCurrentUserId();
        var comment = commentService.updateComment(userId, input.id(), input.content());
        return resolver.resolveComment(comment, userId);
    }

    @MutationMapping(SocialEndpoints.MUTATION_DELETE_COMMENT)
    public boolean deleteComment(@Argument UUID id) {
        UUID userId = getCurrentUserId();
        commentService.deleteComment(userId, id);
        return true;
    }

    // ── Field resolver for nested replies on Comment ───────

    @SchemaMapping(field = "replies", typeName = "Comment")
    public CommentPage commentReplies(CommentResponse comment, @Argument UUID cursor, @Argument Integer limit) {
        UUID userId = getCurrentUserId();
        int pageLimit = (limit != null && limit > 0) ? Math.min(limit, 50) : 5;
        return resolver.resolveReplies(comment.id(), userId, pageLimit);
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(auth.getName());
    }
}