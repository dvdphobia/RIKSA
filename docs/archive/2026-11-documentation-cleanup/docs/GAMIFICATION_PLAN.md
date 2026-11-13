# Gamification Engine — Design & Implementation Plan

## Overview

The Gamification module is the core engine of Riksa. Every user action that earns XP flows through this module. It tracks XP, streaks, rank tiers, achievements, and leaderboard positions.

**Design decisions confirmed:**
- XP Sources: Lessons only (for now)
- Streaks: Consecutive days with at least one lesson completion
- Rank Tiers: Purely XP-based thresholds (Bronze → Legend)
- Achievements: Auto-awarded when criteria are met
- Integration: Direct service calls (Learning → Gamification)

---

## Tables Involved

### 8 Gamification Tables (already in V1 schema)

| # | Table | Role | Key Columns |
|---|---|---|---|
| 1 | `user_gamification` | Core state — one row per user | `user_id` (PK), `total_xp`, `current_streak`, `highest_streak`, `last_completed_at` |
| 2 | `xp_transactions` | Ledger — every XP award recorded | `user_id`, `amount`, `source_type`, `source_id`, `running_balance` |
| 3 | `streak_milestones` | Streak badge definitions | `min_streak`, `streak_name`, `icon_url` |
| 4 | `rank_tiers` | Tier definitions | `tier_name` (enum), `min_xp`, `icon_url` |
| 5 | `rank_history` | Tier change audit log | `user_id`, `scope`, `old_rank`, `new_rank` |
| 6 | `achievements` | Achievement definitions | `criteria_type` (enum), `criteria_value`, `requires_advanced` |
| 7 | `user_achievements` | Unlocked achievements | `user_id`, `achievement_id`, `unlocked_at` |
| 8 | `leaderboard_snapshots` | Daily ranking snapshots | `user_id`, `scope`, `scope_id`, `rank`, `score`, `snapshot_date` |

### 1 Feeder Table (from Learning domain — needs minimal Java entity)

| # | Table | Role |
|---|---|---|
| 9 | `lessons` | Provides `base_xp` and `difficulty` for XP calculation |

### DB Enums Used (exist as PostgreSQL types, need Java counterparts)

| Enum | Values | Where Used |
|---|---|---|
| `DifficultyLevel` | EASY, MEDIUM, HARD, DIFFICULT | lessons, exercises |
| `ScopeType` | PROVINCE, COUNTRY | leaderboard_snapshots, rank_history |
| `TierType` | BRONZE, SILVER, GOLD, PLATINUM, DIAMOND, LEGEND | rank_tiers, rank_history, leaderboard_snapshots |
| `AchievementCriteriaType` | COMPLETE_COURSE, COMPLETE_N_COURSES, EARN_XP, etc. | achievements |

---

## Data Flow: Lesson Completion

```
┌─────────────────────────────────────────────────────────────────┐
│ Learning Module (future)                                        │
│  User completes lesson                                          │
│  └─► Create LessonCompletion record                             │
│  └─► Call GamificationService.handleLessonCompletion(userId,     │
│         lessonId, baseXp, difficulty)                            │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│ Gamification Module                                             │
│                                                                  │
│  1. Get/Create UserGamification row                              │
│                                                                  │
│  2. Update Streak                                                │
│     ┌─ Is last_completed_at yesterday?   → streak++              │
│     ├─ Is last_completed_at today?       → no change             │
│     └─ Is last_completed_at older?       → streak = 1            │
│                                                                  │
│  3. Calculate XP (baseXp + difficulty bonus)                     │
│     └─ Record XpTransaction (ledger)                             │
│     └─ Update UserGamification.total_xp                          │
│                                                                  │
│  4. Check Rank Tier Change                                       │
│     └─ Did total_xp cross a rank_tiers threshold?                │
│     └─ If yes → record in rank_history                          │
│                                                                  │
│  5. Check Achievements                                           │
│     └─ Does new total_xp (or lesson count) unlock anything?      │
│     └─ If yes → insert into user_achievements                    │
│                                                                  │
│  6. Check Streak Milestones                                      │
│     └─ Did current_streak cross a milestone threshold?           │
│                                                                  │
│  7. Update Leaderboard Snapshot (today)                          │
│                                                                  │
│  └─► Return GamificationResult to Learning module               │
└─────────────────────────────────────────────────────────────────┘
```

---

## Java Package Structure

```
src/main/java/com/riksa/gamification/
├── entity/
│   ├── UserGamification.java
│   ├── XpTransaction.java
│   ├── StreakMilestone.java
│   ├── RankTier.java
│   ├── RankHistory.java
│   ├── Achievement.java
│   └── UserAchievement.java
├── repository/
│   ├── UserGamificationRepository.java
│   ├── XpTransactionRepository.java
│   ├── StreakMilestoneRepository.java
│   ├── RankTierRepository.java
│   ├── RankHistoryRepository.java
│   ├── AchievementRepository.java
│   └── UserAchievementRepository.java
├── service/
│   ├── GamificationService.java        (orchestrator + public API)
│   ├── StreakService.java              (streak calculation logic)
│   ├── RankService.java                (tier lookup + change detection)
│   ├── AchievementService.java         (criteria checking + auto-award)
│   └── LeaderboardService.java         (daily snapshots + queries)
├── controller/
│   └── GamificationController.java     (REST endpoints)
├── dto/
│   ├── GamificationStateResponse.java
│   ├── XpTransactionResponse.java
│   ├── LeaderboardEntryResponse.java
│   ├── AchievementResponse.java
│   └── UserAchievementResponse.java
└── enums/
    ├── DifficultyLevel.java
    ├── ScopeType.java
    ├── TierType.java
    └── AchievementCriteriaType.java
```

**Cross-domain note:** `lessons` table entity is needed by gamification for XP calculation. Create a minimal `Lesson.java` entity in `com.riksa.gamification.entity` with only the fields Gamification needs (`id`, `base_xp`, `difficulty`). The full Learning module will expand it later.

**Shared enums:** `TierType`, `ScopeType`, `AchievementCriteriaType`, and `DifficultyLevel` are also used by other domains (leaderboard, social). Place them in `com.riksa.common.enums` so they're shared.

---

## Entity Design

### UserGamification
```
@Entity @Table(name = "user_gamification")
  @Id UUID userId               // matches users.id (not auto-generated)
  int totalXp                   // default 0
  int currentStreak             // default 0
  int highestStreak             // default 0
  OffsetDateTime lastCompletedAt
  OffsetDateTime updatedAt      // @PreUpdate / DB trigger
```

### XpTransaction
```
@Entity @Table(name = "xp_transactions")
  @Id UUID id                  // auto-generated
  UUID userId
  int amount
  String sourceType            // "LESSON" (future: "DAILY_LOGIN", etc.)
  UUID sourceId                // lesson UUID
  int runningBalance           // user's total XP after this transaction
  OffsetDateTime createdAt     // @CreationTimestamp
```

### StreakMilestone, RankTier, Achievement
Read-only reference tables (seeded with initial data). Simple entities mapping all columns, no relationships.

### RankHistory, UserAchievement, LeaderboardSnapshot
Simple entities mapping all columns, no JPA relationships (cross-domain references use UUID only).

---

## Core Algorithm: `handleLessonCompletion`

```java
@Transactional
public GamificationResult handleLessonCompletion(UUID userId, UUID lessonId, int baseXp, DifficultyLevel difficulty) {
    UserGamification gam = getOrCreate(userId);

    // 1. Streak
    int prevStreak = gam.getCurrentStreak();
    int newStreak = streakService.calculateStreak(gam.getLastCompletedAt());
    gam.setCurrentStreak(newStreak);
    gam.setHighestStreak(Math.max(newStreak, gam.getHighestStreak()));
    gam.setLastCompletedAt(OffsetDateTime.now());

    // 2. XP
    int xpAmount = calculateXp(baseXp, difficulty);
    int prevTotalXp = gam.getTotalXp();
    gam.setTotalXp(gam.getTotalXp() + xpAmount);

    recordXpTransaction(userId, xpAmount, "LESSON", lessonId, gam.getTotalXp());
    userGamificationRepository.save(gam);

    // 3. Rank tier
    RankTier oldTier = rankService.getCurrentTier(prevTotalXp);
    RankTier newTier = rankService.getCurrentTier(gam.getTotalXp());
    boolean tierChanged = !oldTier.equals(newTier);
    if (tierChanged) {
        rankService.recordRankChange(userId, oldTier.getTierName(), newTier.getTierName());
    }

    // 4. Achievements
    List<UserAchievement> newAchievements = achievementService.checkAndAward(userId, gam);

    // 5. Streak milestone
    StreakMilestone milestone = streakService.checkMilestone(newStreak, prevStreak);

    // 6. Leaderboard
    leaderboardService.updateDailySnapshot(userId, gam.getTotalXp(), newTier.getTierName());

    return new GamificationResult(xpAmount, newStreak, newTier.getTierName(), tierChanged, newAchievements, milestone);
}
```

---

## REST API Endpoints

All under `/api/gamification`, authenticated (JWT required).

| Method | Endpoint | Response | Purpose |
|---|---|---|---|
| GET | `/api/gamification/me` | `GamificationStateResponse` | Current user's XP, streak, rank, achievements |
| GET | `/api/gamification/leaderboard?scope=COUNTRY&scopeId=KH` | `List<LeaderboardEntryResponse>` | Top users by scope |
| GET | `/api/gamification/achievements` | `List<AchievementResponse>` | All achievement definitions |
| GET | `/api/gamification/achievements/unlocked` | `List<UserAchievementResponse>` | User's unlocked achievements |
| GET | `/api/gamification/history?page=0&size=20` | `List<XpTransactionResponse>` | XP transaction history |

---

## Authorization in SecurityConfig

Add to `SecurityConfig.authorizeHttpRequests`:
```java
.requestMatchers("/api/gamification/**").authenticated()
```

No public endpoints on gamification. Already covered by `.anyRequest().authenticated()`.

---

## Seed Data — New Flyway Migration (V7)

Create `V7__seed_gamification_data.sql` with:
```sql
-- Rank tiers (uncomment and adjust from V1)
INSERT INTO "rank_tiers" (tier_name, min_xp, icon_url) VALUES
    ('BRONZE', 0, '/icons/rank-bronze.png'),
    ('SILVER', 100, '/icons/rank-silver.png'),
    ('GOLD', 500, '/icons/rank-gold.png'),
    ('PLATINUM', 1000, '/icons/rank-platinum.png'),
    ('DIAMOND', 2500, '/icons/rank-diamond.png'),
    ('LEGEND', 5000, '/icons/rank-legend.png');

-- Streak milestones (uncomment from V1)
INSERT INTO "streak_milestones" (min_streak, streak_name, icon_url) VALUES
    (1, 'First Day', '/icons/streak-1.png'),
    (3, 'Getting Started', '/icons/streak-3.png'),
    (7, 'Week Warrior', '/icons/streak-7.png'),
    (14, 'Two Week Streak', '/icons/streak-14.png'),
    (30, 'Monthly Master', '/icons/streak-30.png'),
    (60, 'Two Month Streak', '/icons/streak-60.png'),
    (90, 'Quarterly Quest', '/icons/streak-90.png'),
    (365, 'Yearly Legend', '/icons/streak-365.png');

-- Sample achievements
INSERT INTO "achievements" (name, description, criteria_type, criteria_value, icon_url) VALUES
    ('First Lesson', 'Complete your first lesson', 'COMPLETE_N_COURSES', 1, '/icons/ach-first-lesson.png'),
    ('Quick Learner', 'Complete 10 lessons', 'COMPLETE_N_COURSES', 10, '/icons/ach-quick-learner.png'),
    ('Dedicated Student', 'Complete 50 lessons', 'COMPLETE_N_COURSES', 50, '/icons/ach-dedicated.png'),
    ('XP Hunter', 'Earn 1000 XP', 'EARN_XP', 1000, '/icons/ach-xp-hunter.png'),
    ('XP Master', 'Earn 5000 XP', 'EARN_XP', 5000, '/icons/ach-xp-master.png'),
    ('Hard Mode', 'Complete a HARD difficulty lesson', 'COMPLETE_COURSE_WITH_DIFFICULTY', 0, '/icons/ach-hard-mode.png');
```

---

## DTO Designs

### GamificationStateResponse
```json
{
  "totalXp": 1250,
  "currentStreak": 7,
  "highestStreak": 14,
  "currentTier": "GOLD",
  "tierIconUrl": "/icons/rank-gold.png",
  "recentAchievements": [
    { "id": "...", "name": "Quick Learner", "unlockedAt": "..." }
  ],
  "rank": 42,
  "totalLessonsCompleted": 15
}
```

### LeaderboardEntryResponse
```json
{
  "rank": 1,
  "userId": "...",
  "username": "john",
  "displayName": "John",
  "avatarUrl": "avatar-3.png",
  "score": 5000,
  "tier": "LEGEND"
}
```

---

## Implementation Order (Vertical Slices)

### Slice 1: Foundation (Enums + Entities + Repositories)
- Create shared enums in `common/enums/`
- Create all 7 gamification entities
- Create all 7 repositories
- Create minimal `Lesson` entity (just id, baseXp, difficulty)
- Create V7 migration with seed data

### Slice 2: Core Engine (Services)
- `StreakService` — streak calculation logic
- `RankService` — tier lookup, change detection, rank_history
- `GamificationService.handleLessonCompletion()` — the core orchestrator
- Unit tests for all services

### Slice 3: Achievements + Leaderboard
- `AchievementService` — criteria evaluation engine
- `LeaderboardService` — daily snapshot management
- Wire into `GamificationService`

### Slice 4: REST API
- `GamificationController` — all endpoints
- `SecurityConfig` update (already covered by `.anyRequest().authenticated()`)
- Integration/controller tests

---

## Design Decision: Who Calculates XP?

**Option A (recommended):** Learning passes `(userId, lessonId, baseXp, difficulty)` to Gamification. Gamification owns the XP formula (difficulty multiplier, bonus logic). This keeps the XP economy centralized.

**Option B:** Learning calculates XP and passes the final amount. Simpler but scatters XP logic.

**Recommendation:** Option A — start simple with `xp = baseXp` (no multiplier for now), but the formula lives in Gamification so it can be adjusted later without touching Learning.

---

## Dependencies on Other Domains

| Dependency | What's Needed | Status |
|---|---|---|
| `users` table | User existence check (optional — trust the caller) | Exists |
| `lessons` table | Minimal entity with `id`, `base_xp`, `difficulty` | Needs creation |
| `lesson_completions` table | Not needed by Gamification — created by Learning | Future |

Gamification should **not** call UserRepository or AuthService. It treats `userId` as a UUID and only touches its own tables.
# Deprecated design plan

> This document is retained for historical context and may not match the current implementation. Use [PROJECT_GUIDE.md](PROJECT_GUIDE.md) as the source of truth.
