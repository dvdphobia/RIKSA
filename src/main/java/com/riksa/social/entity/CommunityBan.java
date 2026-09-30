package com.riksa.social.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "community_bans")
@IdClass(CommunityBan.Key.class)
@Getter @Setter @NoArgsConstructor
public class CommunityBan {

    @Id
    @Column(name = "category_id")
    private UUID categoryId;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "banned_by")
    private UUID bannedBy;

    @Column(length = 200)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
    public static class Key implements Serializable {
        private UUID categoryId;
        private UUID userId;
    }
}
