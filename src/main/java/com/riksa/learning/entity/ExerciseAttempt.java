package com.riksa.learning.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "exercise_attempts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "exercise_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ExerciseAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "attempted_at", updatable = false)
    private OffsetDateTime attemptedAt = OffsetDateTime.now();
}