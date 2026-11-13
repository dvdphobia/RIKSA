package com.riksa.auth.service;

import com.riksa.auth.entity.User;
import com.riksa.auth.event.UserAccountDeletedEvent;
import com.riksa.auth.repository.UserRepository;
import com.riksa.blog.entity.BlogPost;
import com.riksa.blog.repository.BlogPostRepository;
import com.riksa.common.cache.CacheNames;
import com.riksa.common.event.EventPublisher;
import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.report.enums.ReportTargetType;
import com.riksa.report.repository.ContentReportRepository;
import com.riksa.resource.entity.LessonResource;
import com.riksa.resource.repository.LessonResourceRepository;
import com.riksa.social.entity.Comment;
import com.riksa.social.entity.Post;
import com.riksa.social.repository.CommentRepository;
import com.riksa.social.repository.PostRepository;
import com.riksa.media.repository.MediaAssetRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AccountDeletionService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final BlogPostRepository blogPostRepository;
    private final LessonResourceRepository lessonResourceRepository;
    private final ContentReportRepository contentReportRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final CacheManager cacheManager;
    private final EventPublisher eventPublisher;

    public AccountDeletionService(
            UserRepository userRepository,
            PostRepository postRepository,
            CommentRepository commentRepository,
            BlogPostRepository blogPostRepository,
            LessonResourceRepository lessonResourceRepository,
            ContentReportRepository contentReportRepository,
            MediaAssetRepository mediaAssetRepository,
            CacheManager cacheManager,
            EventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.blogPostRepository = blogPostRepository;
        this.lessonResourceRepository = lessonResourceRepository;
        this.contentReportRepository = contentReportRepository;
        this.mediaAssetRepository = mediaAssetRepository;
        this.cacheManager = cacheManager;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void delete(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        List<Post> posts = postRepository.findByAuthorId(userId);
        List<UUID> postIds = posts.stream().map(Post::getId).toList();
        Set<UUID> commentIds = new LinkedHashSet<>();
        commentRepository.findByAuthorId(userId).stream().map(Comment::getId).forEach(commentIds::add);
        if (!postIds.isEmpty()) {
            commentRepository.findByPostIdIn(postIds).stream().map(Comment::getId).forEach(commentIds::add);
        }
        List<BlogPost> blogPosts = blogPostRepository.findAllByAuthorId(userId);
        List<LessonResource> resources = lessonResourceRepository.findAllByAuthorId(userId);

        deleteReports(ReportTargetType.POST, postIds);
        deleteReports(ReportTargetType.COMMENT, new ArrayList<>(commentIds));
        deleteReports(ReportTargetType.BLOG, blogPosts.stream().map(BlogPost::getId).toList());
        deleteReports(ReportTargetType.RESOURCE, resources.stream().map(LessonResource::getId).toList());

        List<UserAccountDeletedEvent.MediaAssetReference> mediaAssets = mediaAssetRepository
                .findAllByOwnerId(userId).stream()
                .map(asset -> new UserAccountDeletedEvent.MediaAssetReference(
                        asset.getId(), asset.getPublicUrl()))
                .toList();

        userRepository.delete(user);
        evictUserCaches(userId);
        eventPublisher.publish(new UserAccountDeletedEvent(this, userId, mediaAssets));
    }

    private void deleteReports(ReportTargetType targetType, List<UUID> targetIds) {
        if (!targetIds.isEmpty()) {
            contentReportRepository.deleteByTargetTypeAndTargetIdIn(targetType, targetIds);
        }
    }

    private void evictUserCaches(UUID userId) {
        for (String name : List.of(
                CacheNames.USER_SECURITY_STATE,
                CacheNames.GAMIFICATION_PROFILE,
                CacheNames.GAMIFICATION_ACHIEVEMENTS,
                CacheNames.GAMIFICATION_STREAK)) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.evict(userId.toString());
            }
        }
    }
}
