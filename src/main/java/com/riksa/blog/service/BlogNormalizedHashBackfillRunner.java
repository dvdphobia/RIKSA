package com.riksa.blog.service;

import com.riksa.blog.entity.BlogPost;
import com.riksa.blog.repository.BlogPostRepository;
import com.riksa.moderation.ContentModerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/** Backfills canonical duplicate hashes missing on recent posts after V42 deploys. */
@Component
@RequiredArgsConstructor
public class BlogNormalizedHashBackfillRunner implements ApplicationRunner {

    private static final int BACKFILL_HOURS = 48;

    private final BlogPostRepository blogPostRepository;
    private final ContentModerationService contentModerationService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<BlogPost> posts = blogPostRepository.findByNormalizedHashIsNullAndCreatedAtAfter(
                OffsetDateTime.now().minusHours(BACKFILL_HOURS));
        if (posts.isEmpty()) {
            return;
        }

        posts.forEach(post -> post.setNormalizedHash(BlogPostHash.hash(contentModerationService,
                post.getTitle() + " " + post.getContent())));
        blogPostRepository.saveAll(posts);
    }
}
