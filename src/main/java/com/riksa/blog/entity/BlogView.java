package com.riksa.blog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "blog_views")
@Getter @Setter @NoArgsConstructor
public class BlogView {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "blog_post_id", nullable = false)
    private UUID blogPostId;

    @Column(name = "user_id")
    private UUID userId;

    @CreationTimestamp
    @Column(name = "viewed_at", updatable = false)
    private OffsetDateTime viewdAt;
}
