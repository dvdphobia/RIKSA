package com.riksa.buddy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "buddy")
@Getter
@Setter
@NoArgsConstructor
public class Buddy {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 20)
    private String species;

    @Column(nullable = false, length = 16)
    private String name;

    // V34 stores energy as SMALLINT; schema validation rejects the default INTEGER.
    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private int energy;

    @Column(nullable = false)
    private int bond;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
