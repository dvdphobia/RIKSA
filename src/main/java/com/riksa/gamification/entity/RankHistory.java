package com.riksa.gamification.entity;

import com.riksa.common.enums.ScopeType;
import com.riksa.common.enums.TierType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "rank_history")
@Getter
@Setter
public class RankHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "\"ScopeType\"")
    private ScopeType scope;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "old_rank", nullable = false, columnDefinition = "\"TierType\"")
    private TierType oldRank;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "new_rank", nullable = false, columnDefinition = "\"TierType\"")
    private TierType newRank;

    @Column(name = "changed_at", updatable = false)
    private OffsetDateTime changedAt = OffsetDateTime.now();
}
