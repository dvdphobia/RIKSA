package com.riksa.learning.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "roadmap_unit_skips",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "roadmap_unit_id"}))
@Getter
@Setter
@NoArgsConstructor
public class RoadmapUnitSkip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "roadmap_unit_id", nullable = false)
    private UUID roadmapUnitId;

    @Column(name = "skipped_at", nullable = false, updatable = false)
    private OffsetDateTime skippedAt = OffsetDateTime.now();
}
