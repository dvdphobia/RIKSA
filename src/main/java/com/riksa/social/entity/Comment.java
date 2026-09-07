package com.riksa.social.entity;

import com.riksa.social.enums.ContentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "comments")
@Getter @Setter @NoArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Nullable: a comment may be image-only. See media_url. */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** Client-supplied image URL, mirroring {@code Post.mediaUrl}. */
    @Column(name = "media_url", columnDefinition = "TEXT")
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "\"ContentStatus\"")
    private ContentStatus status = ContentStatus.VISIBLE;

    @Column(name = "hidden_reason", length = 255)
    private String hiddenReason;

    @Column(name = "hidden_by")
    private UUID hiddenBy;

    @Column(name = "hidden_at")
    private OffsetDateTime hiddenAt;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "parent_id")
    private UUID parentId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}