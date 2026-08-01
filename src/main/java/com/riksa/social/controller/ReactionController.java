package com.riksa.social.controller;

import com.riksa.common.constants.SocialEndpoints;
import com.riksa.social.dto.PostResponse;
import com.riksa.social.dto.ReactInput;
import com.riksa.social.service.ReactionService;
import com.riksa.social.service.SocialResponseResolver;
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
    private final SocialResponseResolver resolver;

    @MutationMapping(SocialEndpoints.MUTATION_REACT)
    public PostResponse react(@Argument ReactInput input) {
        UUID userId = getCurrentUserId();
        var post = reactionService.react(userId, input.postId(), input.commentId(), input.type());
        return resolver.resolvePost(post, userId);
    }

    @MutationMapping(SocialEndpoints.MUTATION_REMOVE_REACTION)
    public PostResponse removeReaction(@Argument UUID postId, @Argument UUID commentId) {
        UUID userId = getCurrentUserId();
        var post = reactionService.removeReaction(userId, postId, commentId);
        return resolver.resolvePost(post, userId);
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(auth.getName());
    }
}