package com.riksa.social.service;

import com.riksa.social.entity.Post;
import com.riksa.social.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock private PostRepository postRepository;
    @InjectMocks private FeedService feedService;

    @Test
    void getFeed_filtersByAuthorAndUsesLimitPlusOne() {
        UUID categoryId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID cursor = UUID.randomUUID();
        Post first = post(UUID.randomUUID());
        Post second = post(UUID.randomUUID());
        Post extra = post(UUID.randomUUID());

        when(postRepository.findFeedAfterCursor(
                org.mockito.ArgumentMatchers.eq(categoryId),
                org.mockito.ArgumentMatchers.eq(authorId),
                org.mockito.ArgumentMatchers.eq(cursor),
                org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of(first, second, extra));

        FeedService.FeedResult result = feedService.getFeed(categoryId, authorId, cursor, 2);

        assertThat(result.posts()).containsExactly(first, second);
        assertThat(result.hasNextPage()).isTrue();
        assertThat(result.endCursor()).isEqualTo(second.getId());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(postRepository).findFeedAfterCursor(
                org.mockito.ArgumentMatchers.eq(categoryId),
                org.mockito.ArgumentMatchers.eq(authorId),
                org.mockito.ArgumentMatchers.eq(cursor),
                pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(3);
    }

    private Post post(UUID id) {
        Post post = new Post();
        post.setId(id);
        return post;
    }
}
