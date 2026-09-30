package com.riksa.social.service;

import com.riksa.auth.repository.UserRepository;
import com.riksa.common.event.EventPublisher;
import com.riksa.moderation.ContentBlockedException;
import com.riksa.moderation.ContentModerationService;
import com.riksa.moderation.AuthorSlidingWindowRateLimiter;
import com.riksa.moderation.RateLimitedException;
import com.riksa.moderation.ModerationProperties;
import com.riksa.moderation.WordListLoader;
import com.riksa.social.dto.PostMediaInput;
import com.riksa.social.entity.Post;
import com.riksa.social.entity.PostMedia;
import com.riksa.social.enums.PostKind;
import com.riksa.social.event.PostCreatedEvent;
import com.riksa.social.repository.PostMediaRepository;
import com.riksa.social.repository.PostRepository;
import com.riksa.social.repository.UserCategoryMembershipRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.time.OffsetDateTime;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private PostMediaRepository postMediaRepository;
    @Mock private UserCategoryMembershipRepository membershipRepository;
    @Mock private UploadedMediaUrlValidator uploadedMediaUrlValidator;
    @Mock private EventPublisher eventPublisher;

    @Mock private UserRepository userRepository;

    private PostService postService;

    @BeforeEach
    void setUp() {
        ContentModerationService moderation = new ContentModerationService(
                new WordListLoader(), new ModerationProperties(5, 2, List.of(), Duration.ofHours(24)));
        postService = new PostService(postRepository, postMediaRepository, membershipRepository,
                uploadedMediaUrlValidator, eventPublisher, userRepository, moderation,
                new AuthorSlidingWindowRateLimiter());
    }

    @Test
    void createPost_withZeroMedia_savesAndPublishesEvent() {
        UUID authorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        allowMembership(authorId, categoryId);
        saveWithId();

        Post result = postService.createPost(authorId, "Physics Question", "Help with physics", null, null, categoryId);

        assertThat(result.getTitle()).isEqualTo("Physics Question");
        assertThat(result.getMediaUrl()).isNull();
        verify(postMediaRepository, never()).saveAll(any());
        verify(eventPublisher).publish(any(PostCreatedEvent.class));
    }

    @Test
    void createPost_severeTermIsBlockedBeforeSaveOrEvent() {
        UUID authorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        allowMembership(authorId, categoryId);

        assertThatThrownBy(() -> postService.createPost(authorId, "Question", "shit", null, null, categoryId))
                .isInstanceOf(ContentBlockedException.class);

        verify(postRepository, never()).save(any());
        verify(postMediaRepository, never()).saveAll(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void createPost_reviewVerdictStillSavesAndPublishes() {
        UUID authorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        allowMembership(authorId, categoryId);
        saveWithId();

        Post saved = postService.createPost(authorId, "History", "Free money is a phrase to flag",
                null, null, categoryId);

        assertThat(saved.getId()).isNotNull();
        verify(postRepository).save(any(Post.class));
        verify(eventPublisher).publish(any(PostCreatedEvent.class));
    }

    @Test
    void createPost_mediaOnlyWithBlankTextPassesModeration() {
        UUID authorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        allowMembership(authorId, categoryId);
        saveWithId();
        when(uploadedMediaUrlValidator.normalize("https://api.example.test/photo.png"))
                .thenReturn("https://api.example.test/photo.png");

        Post saved = postService.createPost(authorId, "  ", "", null,
                List.of(new PostMediaInput("https://api.example.test/photo.png", 100, 100)), categoryId);

        assertThat(saved.getMediaUrl()).isEqualTo("https://api.example.test/photo.png");
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void createPost_withOneMedia_persistsDimensionsAndCompatibilityUrl() {
        UUID authorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        allowMembership(authorId, categoryId);
        saveWithId();
        when(uploadedMediaUrlValidator.normalize("https://api.example.test/api/v1/media/one.png"))
                .thenReturn("https://api.example.test/api/v1/media/one.png");

        Post result = postService.createPost(authorId, "Diagram", "See this", null,
                List.of(new PostMediaInput("https://api.example.test/api/v1/media/one.png", 640, 480)), categoryId);

        assertThat(result.getMediaUrl()).isEqualTo("https://api.example.test/api/v1/media/one.png");
        ArgumentCaptor<List<PostMedia>> rows = rowsCaptor();
        verify(postMediaRepository).saveAll(rows.capture());
        assertThat(rows.getValue()).singleElement().satisfies(media -> {
            assertThat(media.getId().getPosition()).isZero();
            assertThat(media.getUrl()).isEqualTo(result.getMediaUrl());
            assertThat(media.getWidth()).isEqualTo(640);
            assertThat(media.getHeight()).isEqualTo(480);
        });
    }

    @Test
    void createPost_withFourMedia_preservesDisplayOrder() {
        UUID authorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        allowMembership(authorId, categoryId);
        saveWithId();
        when(uploadedMediaUrlValidator.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        List<PostMediaInput> media = List.of(
                media("first.png"), media("second.png"), media("third.png"), media("fourth.png"));

        postService.createPost(authorId, "Gallery", "", null, media, categoryId);

        ArgumentCaptor<List<PostMedia>> rows = rowsCaptor();
        verify(postMediaRepository).saveAll(rows.capture());
        assertThat(rows.getValue()).hasSize(4);
        assertThat(rows.getValue()).extracting(row -> row.getId().getPosition()).containsExactly((short) 0, (short) 1, (short) 2, (short) 3);
        assertThat(rows.getValue()).extracting(PostMedia::getUrl)
                .containsExactly("https://api.example.test/api/v1/media/first.png",
                        "https://api.example.test/api/v1/media/second.png",
                        "https://api.example.test/api/v1/media/third.png",
                        "https://api.example.test/api/v1/media/fourth.png");
    }

    @Test
    void createPost_withFiveMedia_isRejected() {
        UUID authorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        allowMembership(authorId, categoryId);

        assertThatThrownBy(() -> postService.createPost(authorId, "Title", "", null,
                List.of(media("1.png"), media("2.png"), media("3.png"), media("4.png"), media("5.png")), categoryId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most 4");
        verify(postRepository, never()).save(any());
    }

    @Test
    void createPost_foreignMediaUrl_isRejected() {
        UUID authorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        allowMembership(authorId, categoryId);
        when(uploadedMediaUrlValidator.normalize("https://foreign.example/image.png"))
                .thenThrow(new IllegalArgumentException("Media URL must use this API's media upload URL"));

        assertThatThrownBy(() -> postService.createPost(authorId, "Title", "body", null,
                List.of(new PostMediaInput("https://foreign.example/image.png", null, null)), categoryId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("this API");
        verify(postRepository, never()).save(any());
    }

    @Test
    void createPost_omittedTitleWithContent_storesEmptyTitle() {
        UUID authorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        allowMembership(authorId, categoryId);
        saveWithId();

        Post result = postService.createPost(authorId, null, "Only content", null, null, categoryId);

        assertThat(result.getTitle()).isEmpty();
        assertThat(result.getContent()).isEqualTo("Only content");
    }

    @Test
    void createPost_everythingEmpty_isRejected() {
        UUID authorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        allowMembership(authorId, categoryId);

        assertThatThrownBy(() -> postService.createPost(authorId, null, "  ", null, List.of(), categoryId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must include a title, content, or at least one image");
        verify(postRepository, never()).save(any());
    }

    @Test
    void createPost_titleLongerThanTwoHundredCharacters_isRejected() {
        UUID authorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        allowMembership(authorId, categoryId);

        assertThatThrownBy(() -> postService.createPost(authorId, "x".repeat(201), "body", null, null, categoryId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most 200");
        verify(postRepository, never()).save(any());
    }

    @Test
    void createPost_outOfRangeDimensions_areRejected() {
        UUID authorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        allowMembership(authorId, categoryId);

        assertThatThrownBy(() -> postService.createPost(authorId, "Title", "body", null,
                List.of(new PostMediaInput("https://api.example.test/api/v1/media/image.png", 10_001, 1)), categoryId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("width must be between 1 and 10000");
        verify(postRepository, never()).save(any());
        verifyNoInteractions(uploadedMediaUrlValidator);
    }

    @Test
    void createPost_legacyMediaUrlOnly_createsFirstMediaAndKeepsAlias() {
        UUID authorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        allowMembership(authorId, categoryId);
        saveWithId();
        when(uploadedMediaUrlValidator.normalize("https://api.example.test/api/v1/media/legacy.png"))
                .thenReturn("https://api.example.test/api/v1/media/legacy.png");

        Post result = postService.createPost(authorId, null, "", "https://api.example.test/api/v1/media/legacy.png", null, categoryId);

        assertThat(result.getMediaUrl()).isEqualTo("https://api.example.test/api/v1/media/legacy.png");
        ArgumentCaptor<List<PostMedia>> rows = rowsCaptor();
        verify(postMediaRepository).saveAll(rows.capture());
        assertThat(rows.getValue()).singleElement().satisfies(row -> {
            assertThat(row.getId().getPosition()).isZero();
            assertThat(row.getUrl()).isEqualTo(result.getMediaUrl());
            assertThat(row.getWidth()).isNull();
            assertThat(row.getHeight()).isNull();
        });
    }

    @Test
    void createPost_mediaWinsWhenLegacyAliasIsAlsoPresent() {
        UUID authorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        allowMembership(authorId, categoryId);
        saveWithId();
        when(uploadedMediaUrlValidator.normalize("https://api.example.test/api/v1/media/new.png"))
                .thenReturn("https://api.example.test/api/v1/media/new.png");

        Post result = postService.createPost(authorId, "Title", "body",
                "https://foreign.example/legacy.png",
                List.of(new PostMediaInput("https://api.example.test/api/v1/media/new.png", 5, 6)), categoryId);

        assertThat(result.getMediaUrl()).isEqualTo("https://api.example.test/api/v1/media/new.png");
        verify(uploadedMediaUrlValidator, never()).normalize("https://foreign.example/legacy.png");
    }

    @Test
    void updatePost_emptyMediaListClearsAllMedia() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = existingPost(authorId, postId, "Old", "Body", "https://api.example.test/api/v1/media/old.png");
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Post result = postService.updatePost(authorId, postId, null, null, null, List.of());

        assertThat(result.getMediaUrl()).isNull();
        verify(postMediaRepository).deleteByPostId(postId);
        verify(postMediaRepository, never()).saveAll(any());
    }

    @Test
    void updatePost_nullMediaListKeepsExistingMedia() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = existingPost(authorId, postId, "Old", "Body", "https://api.example.test/api/v1/media/old.png");
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Post result = postService.updatePost(authorId, postId, "New title", null, null, null);

        assertThat(result.getMediaUrl()).isEqualTo("https://api.example.test/api/v1/media/old.png");
        verifyNoInteractions(postMediaRepository);
        verifyNoInteractions(uploadedMediaUrlValidator);
    }

    @Test
    void updatePost_cannotClearEveryContentField() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = existingPost(authorId, postId, "Title", "Body", "https://api.example.test/api/v1/media/old.png");
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.updatePost(authorId, postId, "", "", null, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(post.getTitle()).isEqualTo("Title");
        assertThat(post.getMediaUrl()).isEqualTo("https://api.example.test/api/v1/media/old.png");
        verify(postRepository, never()).save(any());
    }

    @Test
    void updatePost_byAuthor_updatesFields() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = existingPost(authorId, postId, "Old Title", "Old content", null);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Post result = postService.updatePost(authorId, postId, "New Title", "New content", null, null);

        assertThat(result.getTitle()).isEqualTo("New Title");
        assertThat(result.getContent()).isEqualTo("New content");
    }

    @Test
    void eleventhEditOfAnOldPostInTenMinutesIsRateLimited() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = existingPost(authorId, postId, "Old title", "Old content", null);
        post.setCreatedAt(OffsetDateTime.now().minusDays(2));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.findByAuthorIdAndCreatedAtAfterOrderByCreatedAtDescIdDesc(any(), any(), any()))
                .thenReturn(List.of());
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        for (int edit = 0; edit < 10; edit++) {
            postService.updatePost(authorId, postId, "Updated " + edit, "A normal study question " + edit,
                    null, null);
        }

        assertThatThrownBy(() -> postService.updatePost(authorId, postId, "Too many", "Another study question",
                null, null)).isInstanceOf(RateLimitedException.class);
    }

    @Test
    void updatePost_severeTermIsBlockedWithoutChangingOrSavingPost() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = existingPost(authorId, postId, "Old Title", "Old content", null);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.updatePost(authorId, postId, "Updated", "shit", null, null))
                .isInstanceOf(ContentBlockedException.class);

        assertThat(post.getTitle()).isEqualTo("Old Title");
        assertThat(post.getContent()).isEqualTo("Old content");
        verify(postRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void updatePost_byNonAuthor_throwsException() {
        UUID postId = UUID.randomUUID();
        Post post = existingPost(UUID.randomUUID(), postId, "Title", "Body", null);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.updatePost(UUID.randomUUID(), postId, "New", "content", null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You can only edit your own posts");
    }

    @Test
    void updatePost_nonExistentPost_throwsException() {
        UUID postId = UUID.randomUUID();
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.updatePost(UUID.randomUUID(), postId, "New", "content", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Post not found");
    }

    @Test
    void updatePost_milestoneCannotBeEdited() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = existingPost(authorId, postId, "Milestone", "Body", null);
        post.setKind(PostKind.MILESTONE);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.updatePost(authorId, postId, "New", "content", null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Milestone posts cannot be edited or deleted");
    }

    @Test
    void deletePost_byAuthor_deletesPost() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = existingPost(authorId, postId, "Title", "Body", null);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        postService.deletePost(authorId, postId);

        verify(postRepository).delete(post);
    }

    @Test
    void deletePost_byNonAuthor_throwsException() {
        UUID postId = UUID.randomUUID();
        Post post = existingPost(UUID.randomUUID(), postId, "Title", "Body", null);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deletePost(UUID.randomUUID(), postId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You can only delete your own posts");
        verify(postRepository, never()).delete(any());
    }

    private void allowMembership(UUID authorId, UUID categoryId) {
        when(membershipRepository.existsByUserIdAndCategoryId(authorId, categoryId)).thenReturn(true);
    }

    private void saveWithId() {
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            post.setId(UUID.randomUUID());
            return post;
        });
    }

    private Post existingPost(UUID authorId, UUID postId, String title, String content, String mediaUrl) {
        Post post = new Post();
        post.setId(postId);
        post.setAuthorId(authorId);
        post.setTitle(title);
        post.setContent(content);
        post.setMediaUrl(mediaUrl);
        return post;
    }

    private PostMediaInput media(String fileName) {
        return new PostMediaInput("https://api.example.test/api/v1/media/" + fileName, 100, 80);
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<PostMedia>> rowsCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }
}
