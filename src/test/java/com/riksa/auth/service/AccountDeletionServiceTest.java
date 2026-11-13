package com.riksa.auth.service;

import com.riksa.auth.entity.User;
import com.riksa.auth.event.UserAccountDeletedEvent;
import com.riksa.auth.repository.UserRepository;
import com.riksa.blog.repository.BlogPostRepository;
import com.riksa.common.cache.CacheNames;
import com.riksa.common.event.DomainEvent;
import com.riksa.common.event.EventPublisher;
import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.media.entity.MediaAsset;
import com.riksa.media.repository.MediaAssetRepository;
import com.riksa.report.repository.ContentReportRepository;
import com.riksa.resource.repository.LessonResourceRepository;
import com.riksa.social.entity.Post;
import com.riksa.social.repository.CommentRepository;
import com.riksa.social.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountDeletionServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PostRepository postRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private BlogPostRepository blogPostRepository;
    @Mock private LessonResourceRepository lessonResourceRepository;
    @Mock private ContentReportRepository contentReportRepository;
    @Mock private MediaAssetRepository mediaAssetRepository;
    @Mock private CacheManager cacheManager;
    @Mock private Cache cache;
    @Mock private EventPublisher eventPublisher;

    @InjectMocks private AccountDeletionService service;

    @Test
    void deletesUserContentEvictsCachesAndSchedulesAssetCleanup() {
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        Post post = new Post();
        post.setId(postId);
        MediaAsset asset = new MediaAsset("a1b2c3d4-e5f6-4789-8123-456789abcdef.png",
                userId, "https://api.riksa.test/api/v1/media/a1b2c3d4-e5f6-4789-8123-456789abcdef.png");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(postRepository.findByAuthorId(userId)).thenReturn(List.of(post));
        when(commentRepository.findByAuthorId(userId)).thenReturn(List.of());
        when(commentRepository.findByPostIdIn(List.of(postId))).thenReturn(List.of());
        when(blogPostRepository.findAllByAuthorId(userId)).thenReturn(List.of());
        when(lessonResourceRepository.findAllByAuthorId(userId)).thenReturn(List.of());
        when(mediaAssetRepository.findAllByOwnerId(userId)).thenReturn(List.of(asset));
        when(cacheManager.getCache(any())).thenReturn(cache);

        service.delete(userId);

        verify(userRepository).delete(user);
        verify(cache, times(4)).evict(userId.toString());
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(UserAccountDeletedEvent.class);
        UserAccountDeletedEvent event = (UserAccountDeletedEvent) eventCaptor.getValue();
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getMediaAssets()).containsExactly(
                new UserAccountDeletedEvent.MediaAssetReference(asset.getId(), asset.getPublicUrl()));
    }

    @Test
    void missingAccountDoesNotRunDeletion() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Account not found");

        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(postRepository, commentRepository, blogPostRepository,
                lessonResourceRepository, contentReportRepository, mediaAssetRepository,
                cacheManager, eventPublisher);
    }
}
