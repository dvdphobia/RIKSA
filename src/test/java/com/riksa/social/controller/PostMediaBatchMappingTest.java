package com.riksa.social.controller;

import com.riksa.social.dto.PostMediaResponse;
import com.riksa.social.dto.PostResponse;
import com.riksa.social.entity.PostMedia;
import com.riksa.social.entity.PostMediaId;
import com.riksa.social.repository.PostMediaRepository;
import com.riksa.social.service.PostService;
import com.riksa.social.service.ShareService;
import com.riksa.social.service.SocialResponseResolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PostMediaBatchMappingTest {

    @Test
    void aTwentyPostFeedUsesOneMediaRepositoryQuery() {
        PostMediaRepository mediaRepository = mock(PostMediaRepository.class);
        PostController controller = new PostController(
                mock(PostService.class), mock(ShareService.class), mock(SocialResponseResolver.class), mediaRepository);
        List<PostResponse> posts = java.util.stream.IntStream.range(0, 20)
                .mapToObj(index -> post(UUID.randomUUID()))
                .toList();
        List<UUID> postIds = posts.stream().map(PostResponse::id).toList();
        PostMedia first = new PostMedia(new PostMediaId(postIds.getFirst(), (short) 0), "https://api/media/a.png", 640, 480);
        PostMedia second = new PostMedia(new PostMediaId(postIds.getFirst(), (short) 1), "https://api/media/b.png", 800, 600);
        when(mediaRepository.findAllByPostIds(postIds)).thenReturn(List.of(first, second));

        var result = controller.postMedia(posts);

        assertThat(result).hasSize(20);
        assertThat(result.get(posts.getFirst())).containsExactly(
                new PostMediaResponse("https://api/media/a.png", 640, 480),
                new PostMediaResponse("https://api/media/b.png", 800, 600));
        assertThat(result.get(posts.get(1))).isEmpty();
        verify(mediaRepository, times(1)).findAllByPostIds(postIds);
    }

    private PostResponse post(UUID id) {
        return new PostResponse(id, "Title", "Content", null, 0, 0, 0,
                null, null, null, null, null, null, null);
    }
}
