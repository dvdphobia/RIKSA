package com.riksa.learning.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "lesson_completions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lesson_id"}))
@Getter
@Setter
@NoArgsConstructor
public class LessonCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Column(name = "xp_earned", nullable = false)
    private int xpEarned;

    @Column(name = "completed_at", updatable = false)
    private OffsetDateTime completedAt = OffsetDateTime.now();

    @Column(name = "reflection_text", nullable = false, columnDefinition = "TEXT")
    private String reflectionText = "";

    @Column(name = "score", nullable = false)
    private int score = 0;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;
}
