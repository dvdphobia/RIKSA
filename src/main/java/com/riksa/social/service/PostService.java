package com.riksa.social.service;

import com.riksa.common.event.EventPublisher;
import com.riksa.realtime.RealtimeEvent;
import com.riksa.realtime.RealtimeTypes;
import com.riksa.social.dto.PostMediaInput;
import com.riksa.social.entity.Post;
import com.riksa.social.entity.PostMedia;
import com.riksa.social.entity.PostMediaId;
import com.riksa.social.enums.ContentStatus;
import com.riksa.social.event.PostCreatedEvent;
import com.riksa.social.repository.PostMediaRepository;
import com.riksa.social.repository.PostRepository;
import com.riksa.social.repository.UserCategoryMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostService {

    private static final int MAX_MEDIA = 4;
    private static final int MAX_DIMENSION = 10_000;
    private static final int MAX_TITLE_LENGTH = 200;

    private final PostRepository postRepository;
    private final PostMediaRepository postMediaRepository;
    private final UserCategoryMembershipRepository membershipRepository;
    private final UploadedMediaUrlValidator uploadedMediaUrlValidator;
    private final EventPublisher eventPublisher;

    @Transactional
    public Post createPost(UUID authorId, String title, String content, String mediaUrl,
                           List<PostMediaInput> media, UUID categoryId) {
        if (!membershipRepository.existsByUserIdAndCategoryId(authorId, categoryId)) {
            throw new IllegalStateException("You must join this category before posting");
        }

        String normalizedTitle = title == null ? "" : title;
        String normalizedContent = content == null ? "" : content;
        List<MediaValue> normalizedMedia = normalizeMedia(media != null ? media : legacyMedia(mediaUrl));
        validatePost(normalizedTitle, normalizedContent, normalizedMedia);

        Post post = new Post();
        post.setAuthorId(authorId);
        post.setCategoryId(categoryId);
        post.setTitle(normalizedTitle);
        post.setContent(normalizedContent);
        post.setMediaUrl(firstUrl(normalizedMedia));

        Post saved = postRepository.save(post);
        saveMediaRows(saved.getId(), normalizedMedia);
        eventPublisher.publish(new PostCreatedEvent(this, saved.getId(), authorId, categoryId));
        eventPublisher.publish(RealtimeEvent.toEveryone(this, RealtimeTypes.POST_CREATED,
                Map.of("postId", saved.getId(), "authorId", authorId)));
        return saved;
    }

    /**
     * Rewrites a post the author owns. A null media list leaves media unchanged;
     * an empty list clears all media. The legacy mediaUrl field is used only
     * when media is null, and a blank legacy value clears the media.
     */
    @Transactional
    public Post updatePost(UUID authorId, UUID postId, String title, String content,
                           String mediaUrl, List<PostMediaInput> media) {
        Post post = getPost(postId);

        if (post.getKind() == com.riksa.social.enums.PostKind.MILESTONE) {
            throw new IllegalStateException("Milestone posts cannot be edited or deleted");
        }
        if (!post.getAuthorId().equals(authorId)) {
            throw new IllegalStateException("You can only edit your own posts");
        }

        String nextTitle = title == null ? post.getTitle() : title;
        String nextContent = content == null ? post.getContent() : content;
        List<MediaValue> replacementMedia = null;
        if (media != null) {
            replacementMedia = normalizeMedia(media);
        } else if (mediaUrl != null) {
            replacementMedia = normalizeMedia(legacyMedia(mediaUrl));
        }
        String nextMediaUrl = replacementMedia == null ? post.getMediaUrl() : firstUrl(replacementMedia);
        List<MediaValue> mediaForValidation = replacementMedia == null
                ? (nextMediaUrl == null || nextMediaUrl.isBlank()
                    ? List.of()
                    : List.of(new MediaValue(nextMediaUrl, null, null)))
                : replacementMedia;
        validatePost(nextTitle, nextContent, mediaForValidation);

        post.setTitle(nextTitle);
        post.setContent(nextContent);
        post.setMediaUrl(nextMediaUrl);
        Post saved = postRepository.save(post);
        if (replacementMedia != null) {
            replaceMediaRows(postId, replacementMedia);
        }
        return saved;
    }

    @Transactional
    public void deletePost(UUID authorId, UUID postId) {
        Post post = getPost(postId);

        if (post.getKind() == com.riksa.social.enums.PostKind.MILESTONE) {
            throw new IllegalStateException("Milestone posts cannot be edited or deleted");
        }
        if (!post.getAuthorId().equals(authorId)) {
            throw new IllegalStateException("You can only delete your own posts");
        }

        postRepository.delete(post);
        eventPublisher.publish(RealtimeEvent.toEveryone(this, RealtimeTypes.POST_DELETED,
                Map.of("postId", postId)));
    }

    public Post getPost(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));
    }

    public Post getVisiblePost(UUID postId) {
        Post post = getPost(postId);
        if (post.getStatus() == ContentStatus.HIDDEN) {
            throw new IllegalArgumentException("Post not found: " + postId);
        }
        return post;
    }

    private List<PostMediaInput> legacyMedia(String mediaUrl) {
        return mediaUrl == null || mediaUrl.isBlank()
                ? List.of()
                : List.of(new PostMediaInput(mediaUrl, null, null));
    }

    private List<MediaValue> normalizeMedia(List<PostMediaInput> inputs) {
        if (inputs.size() > MAX_MEDIA) {
            throw new IllegalArgumentException("A post can contain at most " + MAX_MEDIA + " images");
        }

        List<MediaValue> normalized = new ArrayList<>(inputs.size());
        for (PostMediaInput input : inputs) {
            if (input == null) {
                throw new IllegalArgumentException("Post media entries cannot be null");
            }
            validateDimension("width", input.width());
            validateDimension("height", input.height());
            String url = uploadedMediaUrlValidator.normalize(input.url());
            if (url == null) {
                throw new IllegalArgumentException("Post media URL is required");
            }
            normalized.add(new MediaValue(url, input.width(), input.height()));
        }
        return List.copyOf(normalized);
    }

    private void validateDimension(String name, Integer dimension) {
        if (dimension != null && (dimension < 1 || dimension > MAX_DIMENSION)) {
            throw new IllegalArgumentException("Media " + name + " must be between 1 and " + MAX_DIMENSION);
        }
    }

    private void validatePost(String title, String content, List<MediaValue> media) {
        if (title != null && title.codePointCount(0, title.length()) > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Post title must be at most " + MAX_TITLE_LENGTH + " characters");
        }
        boolean hasTitle = title != null && !title.trim().isEmpty();
        boolean hasContent = content != null && !content.trim().isEmpty();
        if (!hasTitle && !hasContent && media.isEmpty()) {
            throw new IllegalArgumentException("A post must include a title, content, or at least one image");
        }
    }

    private String firstUrl(List<MediaValue> media) {
        return media.isEmpty() ? null : media.getFirst().url();
    }

    private void saveMediaRows(UUID postId, List<MediaValue> media) {
        if (!media.isEmpty()) {
            postMediaRepository.saveAll(toEntities(postId, media));
        }
    }

    private void replaceMediaRows(UUID postId, List<MediaValue> media) {
        postMediaRepository.deleteByPostId(postId);
        saveMediaRows(postId, media);
    }

    private List<PostMedia> toEntities(UUID postId, List<MediaValue> media) {
        List<PostMedia> rows = new ArrayList<>(media.size());
        for (int index = 0; index < media.size(); index++) {
            MediaValue value = media.get(index);
            rows.add(new PostMedia(new PostMediaId(postId, (short) index), value.url(), value.width(), value.height()));
        }
        return rows;
    }

    private record MediaValue(String url, Integer width, Integer height) {}
}
