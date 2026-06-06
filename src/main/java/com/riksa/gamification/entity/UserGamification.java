package com.riksa.gamification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_gamification")
@Getter
@Setter
public class UserGamification {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "total_xp", nullable = false)
    private int totalXp = 0;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak = 0;

    @Column(name = "highest_streak", nullable = false)
    private int highestStreak = 0;

    @Column(name = "last_completed_at")
    private OffsetDateTime lastCompletedAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
