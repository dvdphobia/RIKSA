package com.riksa.social.controller;

import com.riksa.social.dto.FeedPage;
import com.riksa.social.dto.PageInfo;
import com.riksa.social.dto.PostResponse;
import com.riksa.social.service.FeedService;
import com.riksa.social.service.SocialResponseResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;
    private final SocialResponseResolver resolver;

    @QueryMapping("feed")
    public FeedPage feed(@Argument UUID categoryId,
                         @Argument UUID authorId,
                         @Argument UUID cursor,
                         @Argument Integer limit) {
        UUID userId = getCurrentUserId();
        int pageLimit = (limit != null && limit > 0) ? Math.min(limit, 50) : 20;

        FeedService.FeedResult result = feedService.getFeed(categoryId, authorId, cursor, pageLimit);

        List<PostResponse> edges = resolver.resolvePosts(result.posts(), userId);

        return new FeedPage(edges, new PageInfo(result.hasNextPage(), result.endCursor()));
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(auth.getName());
    }
}
