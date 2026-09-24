package com.riksa.buddy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "buddy_care_event")
@Getter
@Setter
@NoArgsConstructor
public class BuddyCareEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "source_key", nullable = false, length = 20)
    private String sourceKey;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(name = "care_day", nullable = false)
    private LocalDate careDay;

    // Both gains are SMALLINT columns in V34.
    @Column(name = "energy_gain", nullable = false)
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private int energyGain;

    @Column(name = "bond_gain", nullable = false)
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private int bondGain;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
