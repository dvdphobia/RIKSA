package com.riksa.social.entity;

import jakarta.persistence.*;
import com.riksa.social.enums.PostPermission;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "categories")
@Getter @Setter @NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_url")
    private String iconUrl;

    /** Curated by admins; lessons and category achievements only use these. */
    @Column(nullable = false)
    private boolean official;

    /** The member who founded a user community; null for official ones. */
    @Column(name = "created_by")
    private UUID createdBy;

    /** Who may start posts here; replies stay open to every member. */
    @Enumerated(EnumType.STRING)
    @Column(name = "post_permission", nullable = false, length = 16)
    private PostPermission postPermission = PostPermission.MEMBERS;

    /** A post the owner or a moderator keeps at the top of the feed. */
    @Column(name = "pinned_post_id")
    private UUID pinnedPostId;

    @Column(name = "banner_url", columnDefinition = "TEXT")
    private String bannerUrl;

    /** A #RRGGBB colour; null keeps the subject colour. */
    @Column(name = "banner_color", length = 7)
    private String bannerColor;

    /** Subject default preset. Null means the app uses its built-in subject art. */
    @Column(name = "background_key", length = 32)
    private String backgroundKey;

    /** Subject default illustration. Null means no uploaded image. */
    @Column(name = "background_image_url", columnDefinition = "TEXT")
    private String backgroundImageUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}