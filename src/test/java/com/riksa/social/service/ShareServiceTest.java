package com.riksa.social.service;

import com.riksa.social.entity.Post;
import com.riksa.social.entity.Share;
import com.riksa.social.enums.SharePlatform;
import com.riksa.social.repository.PostRepository;
import com.riksa.social.repository.ShareRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShareServiceTest {
    @Mock ShareRepository shareRepository;
    @Mock PostRepository postRepository;

    @Test
    void sharePostSavesShareAndIncrementsCount() {
        UUID userId = UUID.randomUUID(), postId = UUID.randomUUID();
        Post post = new Post(); post.setId(postId); post.setShareCount(2);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenReturn(post);
        new ShareService(shareRepository, postRepository).sharePost(userId, postId, SharePlatform.TELEGRAM);
        assertThat(post.getShareCount()).isEqualTo(3);
        ArgumentCaptor<Share> captor = ArgumentCaptor.forClass(Share.class);
        verify(shareRepository).save(captor.capture());
        assertThat(captor.getValue().getPlatform()).isEqualTo(SharePlatform.TELEGRAM);
    }

    @Test
    void sharePost_hiddenPost_throwsException() {
        UUID userId = UUID.randomUUID(), postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setStatus(com.riksa.social.enums.ContentStatus.HIDDEN);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> new ShareService(shareRepository, postRepository)
                .sharePost(userId, postId, SharePlatform.TELEGRAM))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Post not found");

        verify(shareRepository, never()).save(any(Share.class));
    }

    @Test
    void nullPlatformDefaultsToCopyLinkAndMissingPostThrows() {
        UUID userId = UUID.randomUUID(), postId = UUID.randomUUID();
        Post post = new Post(); post.setId(postId);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenReturn(post);
        new ShareService(shareRepository, postRepository).sharePost(userId, postId, null);
        verify(shareRepository).save(argThat(s -> s.getPlatform() == SharePlatform.COPY_LINK));

        UUID missing = UUID.randomUUID();
        when(postRepository.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> new ShareService(shareRepository, postRepository)
                .sharePost(userId, missing, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
