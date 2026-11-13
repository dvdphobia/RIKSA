package com.riksa.blog.entity;

import com.riksa.blog.enums.BlogStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "blog_posts")
@Getter @Setter @NoArgsConstructor
public class BlogPost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "cover_url")
    private String coverUrl;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "\"BlogStatus\"")
    private BlogStatus status = BlogStatus.PENDING;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "hidden_reason", length = 1200)
    private String hiddenReason;

    @Column(name = "first_approved_at")
    private OffsetDateTime firstApprovedAt;

    @Column(name = "normalized_hash", length = 64)
    private String normalizedHash;

    @Column(name = "moderation_score")
    private Integer moderationScore;

    @Column(name = "moderation_reasons", length = 500)
    private String moderationReasons;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
