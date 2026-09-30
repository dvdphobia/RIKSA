package com.riksa.blog.service;

import com.riksa.blog.entity.BlogPost;
import com.riksa.blog.repository.BlogPostRepository;
import com.riksa.moderation.ContentModerationService;
import com.riksa.moderation.ModerationProperties;
import com.riksa.moderation.WordListLoader;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BlogNormalizedHashBackfillRunnerTest {

    @Test
    void startupBackfillsRecentPostsWithMissingNormalizedHash() throws Exception {
        BlogPostRepository repository = mock(BlogPostRepository.class);
        ContentModerationService moderation = new ContentModerationService(new WordListLoader(),
                new ModerationProperties(5, 2, List.of(), Duration.ofHours(24)));
        BlogPost recent = new BlogPost();
        recent.setTitle("A recent question");
        recent.setContent("A body that predates the moderation migration.");
        recent.setCreatedAt(OffsetDateTime.now().minusHours(1));
        recent.setNormalizedHash(null);

        when(repository.findByNormalizedHashIsNullAndCreatedAtAfter(any(OffsetDateTime.class)))
                .thenReturn(List.of(recent));
        BlogNormalizedHashBackfillRunner runner = new BlogNormalizedHashBackfillRunner(repository, moderation);

        OffsetDateTime beforeRun = OffsetDateTime.now();
        runner.run(new DefaultApplicationArguments(new String[0]));
        OffsetDateTime afterRun = OffsetDateTime.now();

        String canonical = moderation.normalizeForDuplicate(recent.getTitle() + " " + recent.getContent());
        String expectedHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        assertThat(recent.getNormalizedHash()).isEqualTo(expectedHash);
        ArgumentCaptor<OffsetDateTime> cutoff = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(repository).findByNormalizedHashIsNullAndCreatedAtAfter(cutoff.capture());
        assertThat(cutoff.getValue()).isBetween(beforeRun.minusHours(48), afterRun.minusHours(48));
        verify(repository).saveAll(List.of(recent));
    }
}
