package com.riksa.blog.service;

import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.blog.entity.BlogPost;
import com.riksa.blog.enums.BlogStatus;
import com.riksa.blog.repository.BlogPostRepository;
import com.riksa.moderation.ContentModerationService;
import com.riksa.moderation.AuthorSlidingWindowRateLimiter;
import com.riksa.moderation.ModerationInput;
import com.riksa.moderation.ModerationResult;
import com.riksa.moderation.ModerationSurface;
import com.riksa.moderation.ModerationVerdict;
import com.riksa.moderation.RateLimitedException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlogService {

    private static final int MAX_POSTS_PER_TEN_MINUTES = 3;
    private static final int MAX_EDITS_PER_TEN_MINUTES = 10;

    private final BlogPostRepository blogPostRepository;
    private final UserRepository userRepository;
    private final ContentModerationService contentModerationService;
    private final BlogModerationService blogModerationService;
    private final AuthorSlidingWindowRateLimiter editRateLimiter;

    @Transactional
    public BlogPost createPost(UUID authorId, String title, String content, String coverUrl, UUID categoryId) {
        OffsetDateTime now = OffsetDateTime.now();
        if (blogPostRepository.countByAuthorIdAndCreatedAtAfter(authorId, now.minusMinutes(10))
                >= MAX_POSTS_PER_TEN_MINUTES) {
            throw new RateLimitedException();
        }

        String normalizedHash = BlogPostHash.hash(contentModerationService, title + " " + content);
        ModerationResult result = assess(authorId, title, content, normalizedHash, now, null);
        contentModerationService.requireNotBlocked(result);

        BlogPost post = new BlogPost();
        post.setAuthorId(authorId);
        post.setTitle(title);
        post.setContent(content);
        post.setCoverUrl(coverUrl);
        post.setCategoryId(categoryId);
        post.setNormalizedHash(normalizedHash);
        applyModerationResult(post, result);

        if (result.verdict() == ModerationVerdict.ALLOW) {
            return blogModerationService.publishApproved(post, null);
        }
        post.setStatus(BlogStatus.PENDING);
        return blogPostRepository.save(post);
    }

    @Transactional
    public BlogPost updatePost(UUID authorId, UUID postId, String title, String content,
                               String coverUrl, UUID categoryId) {
        BlogPost post = getPostForUpdate(postId);
        requireOwner(post, authorId);
        editRateLimiter.checkAndRecord("blog-edit", authorId, MAX_EDITS_PER_TEN_MINUTES, Duration.ofMinutes(10));

        OffsetDateTime now = OffsetDateTime.now();
        String normalizedHash = BlogPostHash.hash(contentModerationService, title + " " + content);
        ModerationResult result = assess(authorId, title, content, normalizedHash, now, postId);
        contentModerationService.requireNotBlocked(result);

        post.setTitle(title);
        post.setContent(content);
        post.setCoverUrl(coverUrl);
        post.setCategoryId(categoryId);
        post.setNormalizedHash(normalizedHash);
        post.setRejectionReason(null);
        applyModerationResult(post, result);

        if (result.verdict() == ModerationVerdict.ALLOW) {
            return blogModerationService.publishApproved(post, null);
        }
        post.setStatus(BlogStatus.PENDING);
        return blogPostRepository.save(post);
    }

    @Transactional
    public void deletePost(UUID authorId, UUID postId) {
        BlogPost post = getPostForUpdate(postId);
        requireOwner(post, authorId);
        blogPostRepository.delete(post);
    }

    public Page<BlogPost> listPosts(BlogStatus status, UUID categoryId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (categoryId != null) {
            return blogPostRepository.findByStatusAndCategoryId(status, categoryId, pageable);
        }
        return blogPostRepository.findByStatus(status, pageable);
    }

    public BlogPost getPost(UUID postId) {
        return blogPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Blog post not found: " + postId));
    }

    public Page<BlogPost> getMyPosts(UUID authorId, BlogStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (status != null) {
            return blogPostRepository.findByAuthorIdAndStatus(authorId, status, pageable);
        }
        return blogPostRepository.findByAuthorId(authorId, pageable);
    }

    private ModerationResult assess(UUID authorId, String title, String content, String normalizedHash,
                                    OffsetDateTime now, UUID excludedPostId) {
        OffsetDateTime duplicateCutoff = now.minusHours(24);
        return contentModerationService.assess(
                new ModerationInput(authorId, userRepository.findById(authorId)
                        .map(User::getCreatedAt).orElse(null), ModerationSurface.BLOG, title, content),
                normalizedBody -> {
                    // The engine supplies its canonical body key; the persisted duplicate key
                    // intentionally covers the title and body together.
                    String duplicateHash = BlogPostHash.hash(contentModerationService, title + " " + normalizedBody);
                    return excludedPostId == null
                            ? blogPostRepository.existsByAuthorIdAndNormalizedHashAndCreatedAtAfter(
                                    authorId, duplicateHash, duplicateCutoff)
                            : blogPostRepository.existsByAuthorIdAndNormalizedHashAndCreatedAtAfterAndIdNot(
                                    authorId, duplicateHash, duplicateCutoff, excludedPostId);
                });
    }

    private void applyModerationResult(BlogPost post, ModerationResult result) {
        post.setModerationScore(result.score());
        post.setModerationReasons(result.reasons().stream()
                .map(reason -> reason.code().name())
                .collect(Collectors.joining(",")));
    }

    private void requireOwner(BlogPost post, UUID authorId) {
        if (!post.getAuthorId().equals(authorId)) {
            throw new AccessDeniedException("Only the author can edit or delete this blog post");
        }
    }

    private BlogPost getPostForUpdate(UUID postId) {
        return blogPostRepository.findByIdForUpdate(postId)
                .orElseThrow(() -> new IllegalArgumentException("Blog post not found: " + postId));
    }
}
