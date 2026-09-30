package com.riksa.quest.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
@Entity
@Table(name = "daily_quest", uniqueConstraints = @UniqueConstraint(
        name = "uq_daily_quest_user_date_type", columnNames = {"user_id", "quest_date", "type"}))
@Getter @Setter @NoArgsConstructor
public class DailyQuest {
    @Id @Column(nullable = false) private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "quest_date", nullable = false) private LocalDate questDate;
    @Enumerated(EnumType.STRING) @Column(name = "type", nullable = false, length = 32) private QuestType type;
    @Column(nullable = false) private int target;
    @Column(nullable = false) private int progress;
    @Column(name = "xp_reward", nullable = false) private int xpReward;
    @Column(nullable = false) private boolean claimed;
    @Column(name = "claimed_at") private OffsetDateTime claimedAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
