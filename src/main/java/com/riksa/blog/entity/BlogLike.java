package com.riksa.blog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "blog_likes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "blog_post_id"})
)
@Getter @Setter @NoArgsConstructor
public class BlogLike {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "blog_post_id", nullable = false)
    private UUID blogPostId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}