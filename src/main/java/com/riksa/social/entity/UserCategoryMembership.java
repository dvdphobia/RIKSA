package com.riksa.social.entity;

import jakarta.persistence.*;
import com.riksa.social.enums.CommunityRole;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "user_category_memberships",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "category_id"})
)
@Getter @Setter @NoArgsConstructor
public class UserCategoryMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CommunityRole role = CommunityRole.MEMBER;

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private OffsetDateTime joinedAt;
}