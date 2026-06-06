package com.riksa.gamification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "streak_milestones")
@Getter
@Setter
public class StreakMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "min_streak", unique = true, nullable = false)
    private int minStreak;

    @Column(name = "streak_name", nullable = false, length = 100)
    private String streakName;

    @Column(name = "icon_url", nullable = false)
    private String iconUrl;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
