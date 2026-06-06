package com.riksa.gamification.entity;

import com.riksa.common.enums.AchievementCriteriaType;
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
    private Integer criteriaValue;

    @Column(name = "requires_advanced", nullable = false)
    private boolean requiresAdvanced = false;

    @Column(name = "icon_url", nullable = false)
    private String iconUrl;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
