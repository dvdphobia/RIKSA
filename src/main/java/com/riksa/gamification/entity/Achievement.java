package com.riksa.gamification.entity;

import com.riksa.common.enums.AchievementCriteriaType;
import com.riksa.common.enums.AchievementRarity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "achievements")
@Getter
@Setter
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "criteria_type", nullable = false, columnDefinition = "\"AchievementCriteriaType\"")
    private AchievementCriteriaType criteriaType;

    @Column(name = "criteria_value")
    private Long criteriaValue;

    @Column(name = "requires_advanced", nullable = false)
    private boolean requiresAdvanced = false;

    @Column(name = "icon_url", nullable = false)
    private String iconUrl;

    @Column(name = "category_id")
    private UUID categoryId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "rarity", nullable = false, columnDefinition = "\"AchievementRarity\"")
    private AchievementRarity rarity = AchievementRarity.COMMON;

    @Column(name = "xp_reward", nullable = false)
    private int xpReward = 0;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
