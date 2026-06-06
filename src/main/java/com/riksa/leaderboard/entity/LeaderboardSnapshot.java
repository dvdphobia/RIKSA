package com.riksa.leaderboard.entity;

import com.riksa.common.enums.ScopeType;
import com.riksa.common.enums.TierType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "leaderboard_snapshots",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "scope", "scope_id", "snapshot_date"}))
@Getter
@Setter
public class LeaderboardSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "\"ScopeType\"")
    private ScopeType scope;

    @Column(name = "scope_id", nullable = false)
    private String scopeId;

    @Column(nullable = false)
    private int rank;

    @Column(nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "ranking_tier", nullable = false, columnDefinition = "\"TierType\"")
    private TierType rankingTier;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate = LocalDate.now();

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
