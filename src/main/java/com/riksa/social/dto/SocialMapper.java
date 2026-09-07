package com.riksa.social.dto;

import com.riksa.social.entity.Category;
import com.riksa.social.entity.Comment;
import com.riksa.social.entity.Post;
import com.riksa.auth.entity.User;
import com.riksa.social.enums.ReactionType;

public class SocialMapper {

    public static UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl()
        );
    }

    public static CategoryResponse toCategoryResponse(Category category, int memberCount, boolean joined) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getIconUrl(),
                memberCount,
                joined
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
                post.getUpdatedAt()
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