package com.riksa.social.dto;

import com.riksa.social.entity.Category;
import com.riksa.social.entity.Comment;
import com.riksa.social.entity.Post;
import com.riksa.auth.entity.User;
import com.riksa.social.enums.ReactionType;
import com.riksa.social.enums.MilestoneType;

import java.util.UUID;

public class SocialMapper {

    public static UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl()
        );
    }

    public static CategoryResponse toCategoryResponse(
            Category category, int memberCount, boolean joined, UUID viewerId) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getIconUrl(),
                memberCount,
                joined,
                category.isOfficial(),
                viewerId != null && viewerId.equals(category.getCreatedBy()),
                category.getPostPermission() == null ? "MEMBERS" : category.getPostPermission().name(),
                category.getPinnedPostId()
        );
    }

    public static PostResponse toPostResponse(Post post, UserResponse author,
                                              CategoryResponse category, ReactionType myReaction) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getMediaUrl(),
                post.getReactionCount(),
                post.getCommentCount(),
                post.getShareCount(),
                author,
                category,
                myReaction,
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getKind(),
                post.getKind() == com.riksa.social.enums.PostKind.MILESTONE
                        ? new MilestoneResponse(MilestoneType.valueOf(post.getMilestoneType()),
                                post.getMilestoneTitle(), post.getMilestoneTitleKm(), post.getMilestoneValue())
                        : null
        );
    }

    public static CommentResponse toCommentResponse(Comment comment, UserResponse author,
                                                    ReactionType myReaction) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getMediaUrl(),
                author,
                comment.getPostId(),
                comment.getParentId(),
                myReaction,
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
