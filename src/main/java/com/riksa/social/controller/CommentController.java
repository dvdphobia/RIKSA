package com.riksa.social.controller;

import com.riksa.social.dto.CommentResponse;
import com.riksa.social.dto.CreateCommentInput;
import com.riksa.social.dto.SocialMapper;
import com.riksa.social.dto.UpdateCommentInput;
import com.riksa.social.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @MutationMapping("createComment")
    public CommentResponse createComment(@Argument CreateCommentInput input) {
        UUID userId = getCurrentUserId();
        var comment = commentService.createComment(
                userId,
                input.postId(),
                input.content(),
                input.parentId()
        );
        return SocialMapper.toCommentResponse(comment, null, null);
    }

    @MutationMapping("updateComment")
    public CommentResponse updateComment(@Argument UpdateCommentInput input) {
        UUID userId = getCurrentUserId();
        var comment = commentService.updateComment(userId, input.id(), input.content());
        return SocialMapper.toCommentResponse(comment, null, null);
    }

    @MutationMapping("deleteComment")
    public boolean deleteComment(@Argument UUID id) {
        UUID userId = getCurrentUserId();
        commentService.deleteComment(userId, id);
        return true;
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(auth.getName());
    }
}