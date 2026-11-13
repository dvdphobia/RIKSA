package com.riksa.blog.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import com.riksa.notification.entity.NotificationType;
import com.riksa.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import com.riksa.blog.entity.BlogLike;
import com.riksa.blog.entity.BlogPost;
import com.riksa.blog.entity.BlogView;
import com.riksa.blog.enums.BlogStatus;
import com.riksa.blog.event.BlogPostLikedEvent;
import com.riksa.blog.repository.BlogLikeRepository;
import com.riksa.blog.repository.BlogPostRepository;
import com.riksa.blog.repository.BlogViewRepository;
import com.riksa.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BlogEngagementService {

    private final BlogPostRepository blogPostRepository;
    private final BlogViewRepository blogViewRepository;
    private final BlogLikeRepository blogLikeRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** Optional so hand-built services in tests keep working. */
    @Autowired(required = false)
    private NotificationService notificationService;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public BlogPost trackView(UUID blogPostId, UUID userId) {
        BlogPost post = getPostForUpdateFresh(blogPostId);
        requirePublished(post, blogPostId);

        BlogView view = new BlogView();
        view.setBlogPostId(blogPostId);
        view.setUserId(userId); // nullable — guest views pass null
        blogViewRepository.save(view);

        post.setViewCount(post.getViewCount() + 1);
        return blogPostRepository.save(post);
    }

    @Transactional
    public BlogPost getPostForDetail(UUID blogPostId, UUID userId, boolean isModerator) {
        BlogPost post = getPostForUpdateFresh(blogPostId);
        if (post.getStatus() != BlogStatus.APPROVED
                && !isModerator
                && (userId == null || !userId.equals(post.getAuthorId()))) {
            throw new ResourceNotFoundException("Blog post not found: " + blogPostId);
        }

        if (post.getStatus() == BlogStatus.APPROVED) {
            BlogView view = new BlogView();
            view.setBlogPostId(blogPostId);
            view.setUserId(userId);
            blogViewRepository.save(view);

            post.setViewCount(post.getViewCount() + 1);
            return blogPostRepository.save(post);
        }
        return post;
    }

    @Transactional
    public BlogPost toggleLike(UUID blogPostId, UUID userId) {
        BlogPost post = getPostForUpdateFresh(blogPostId);
        requirePublished(post, blogPostId);
        Optional<BlogLike> existing = blogLikeRepository.findByUserIdAndBlogPostId(userId, blogPostId);

        if (existing.isPresent()) {
            // unlike
            blogLikeRepository.delete(existing.get());
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        } else {
            // like
            BlogLike like = new BlogLike();
            like.setUserId(userId);
            like.setBlogPostId(blogPostId);
            blogLikeRepository.save(like);
            post.setLikeCount(post.getLikeCount() + 1);

            // Notify achievement system that the blog author received a like
            eventPublisher.publishEvent(new BlogPostLikedEvent(this, blogPostId, post.getAuthorId(), userId));
            if (notificationService != null) {
                notificationService.notify(post.getAuthorId(), userId, NotificationType.BLOG_LIKE,
                        "BLOG", blogPostId, null, post.getTitle());
            }
        }

        return blogPostRepository.save(post);
    }

    public boolean isLiked(UUID blogPostId, UUID userId) {
        return blogLikeRepository.existsByUserIdAndBlogPostId(userId, blogPostId);
    }

    private BlogPost getPostForUpdateFresh(UUID blogPostId) {
        BlogPost post = blogPostRepository.findByIdForUpdate(blogPostId)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found: " + blogPostId));
        // OSIV may have placed a stale instance in the request persistence context before
        // this transaction began. Refresh under the row lock before checking status or saving.
        entityManager.refresh(post, LockModeType.PESSIMISTIC_WRITE);
        return post;
    }

    private void requirePublished(BlogPost post, UUID blogPostId) {
        if (post.getStatus() != BlogStatus.APPROVED) {
            throw new ResourceNotFoundException("Blog post not found: " + blogPostId);
        }
    }
}
