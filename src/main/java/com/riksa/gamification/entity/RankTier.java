package com.riksa.gamification.entity;

import com.riksa.common.enums.TierType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "rank_tiers")
@Getter
@Setter
public class RankTier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tier_name", unique = true, nullable = false, columnDefinition = "\"TierType\"")
    private TierType tierName;

    @Column(name = "min_xp", nullable = false)
    private long minXp;

    @Column(name = "icon_url", nullable = false)
    private String iconUrl;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}