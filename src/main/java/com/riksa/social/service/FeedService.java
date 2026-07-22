package com.riksa.social.service;

import com.riksa.social.entity.Post;
import com.riksa.social.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final PostRepository postRepository;

    public FeedResult getFeed(UUID categoryId, UUID authorId, UUID cursor, int limit) {
        List<Post> rows = postRepository.findFeed(
                categoryId, authorId, cursor, PageRequest.of(0, limit + 1)
        );

        boolean hasNextPage = rows.size() > limit;
        List<Post> posts = hasNextPage ? rows.subList(0, limit) : rows;
        UUID endCursor = hasNextPage ? posts.get(posts.size() - 1).getId() : null;

        return new FeedResult(posts, hasNextPage, endCursor);
    }

    public record FeedResult(List<Post> posts, boolean hasNextPage, UUID endCursor) {}
}
