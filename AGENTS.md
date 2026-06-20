# Project Context

## Gamification Module — Full Audit (Jun 8, 2026)

### ✅ Entities (8/8)

| Entity | File | PK | Key Fields |
|--------|------|-----|------------|
| `UserGamification` | `entity/UserGamification.java` | `userId` (UUID, no auto-gen) | totalXp, currentStreak, highestStreak, lastCompletedAt, version (`@Version`) |
| `Achievement` | `entity/Achievement.java` | `id` (UUID auto-gen) | name, description, criteriaType, criteriaValue, requiresAdvanced, iconUrl |
| `UserAchievement` | `entity/UserAchievement.java` | `id` (UUID auto-gen) | userId, achievementId, unlockedAt — unique(userId, achievementId) |
| `RankTier` | `entity/RankTier.java` | `id` (UUID auto-gen) | tierName (enum unique), minXp, iconUrl |
| `RankHistory` | `entity/RankHistory.java` | `id` (UUID auto-gen) | userId, scope, oldRank, newRank, changedAt |
| `StreakMilestone` | `entity/StreakMilestone.java` | `id` (UUID auto-gen) | minStreak (unique), streakName, iconUrl |
| `XpTransaction` | `entity/XpTransaction.java` | `id` (UUID auto-gen) | userId, amount, sourceType, sourceId, runningBalance, createdAt (`@CreationTimestamp`) |
| `Lesson` | `entity/Lesson.java` | `id` (UUID auto-gen) | baseXp, difficulty |

---

### ✅ Repositories (7/7)

| Repository | Key Query Methods |
|------------|-------------------|
| `UserGamificationRepository` | `findByUserId`, `findAllByOrderByTotalXpDesc(Pageable)`, `findLeaderboardAfterCursor(cursorXp, cursorUserId, Pageable)` (cursor JPQL), `findByCurrentStreakGreaterThan`, `findByLastCompletedAtBefore`, `findByIdForUpdate` (PESSIMISTIC_WRITE), `findByIdOptimistic`, `existsByUserId` |
| `AchievementRepository` | `findByCriteriaType` |
| `UserAchievementRepository` | `findByUserId`, `existsByUserIdAndAchievementId` |
| `RankTierRepository` | `findFirstByMinXpLessThanEqualOrderByMinXpDesc` |
| `RankHistoryRepository` | `findByUserIdOrderByChangedAtDesc` |
| `StreakMilestoneRepository` | `findFirstByMinStreakLessThanEqualOrderByMinStreakDesc` |
| `XpTransactionRepository` | `findByUserIdOrderByCreatedAtDesc(Page)` (legacy Page), `findByUserIdAfterCursor(cursorCreatedAt, cursorId, Pageable)` (cursor JPQL) |

---

### ✅ Migrations

| Version | File | Purpose |
|---------|------|---------|
| V1 | `V1__initial_schema.sql` | All gamification tables + enums + indexes + triggers |
| V7 | `V7__seed_gamification_data.sql` | 6 rank tiers, 8 streak milestones, 6 achievements |
| V8 | `V8__add_user_gamification_version.sql` | `ALTER TABLE user_gamification ADD version BIGINT DEFAULT 0` (optimistic locking) |

---

### ✅ API Endpoints (6/6)

| Method | Path | Auth | Params | Returns | Cursor? |
|--------|------|------|--------|---------|:---:|
| `GET` | `/api/v1/gamification/me` | Bearer JWT | — | `GamificationProfileResponse` | — |
| `GET` | `/api/v1/gamification/xp` | Bearer JWT | `?cursor=<base64>&limit=20` | `CursorPage<XpTransactionResponse>` | ✅ |
| `GET` | `/api/v1/gamification/achievements` | Bearer JWT | — | `List<AchievementResponse>` | — |
| `GET` | `/api/v1/gamification/rank-history` | Bearer JWT | — | `List<RankHistoryResponse>` | — |
| `GET` | `/api/v1/gamification/streak` | Bearer JWT | — | `StreakResponse` | — |
| `GET` | `/api/v1/gamification/leaderboard` | Bearer JWT | `?cursor=<base64>&limit=20` | `CursorPage<LeaderboardEntryResponse>` | ✅ |

---

### ✅ DTOs (7/7)

| DTO | File | Fields |
|-----|------|--------|
| `GamificationProfileResponse` | `dto/GamificationProfileResponse.java` | userId, totalXp, currentStreak, highestStreak, currentTier, currentTierIcon, nextTier, xpToNext |
| `XpTransactionResponse` | `dto/XpTransactionResponse.java` | id, amount, sourceType, sourceId, runningBalance, createdAt |
| `AchievementResponse` | `dto/AchievementResponse.java` | id, name, description, iconUrl, earned, unlockedAt |
| `RankHistoryResponse` | `dto/RankHistoryResponse.java` | id, scope, oldRank, newRank, changedAt |
| `StreakResponse` | `dto/StreakResponse.java` | currentStreak, highestStreak, currentMilestoneName, currentMilestoneIcon, nextMilestoneName, nextMilestoneMinStreak, daysToNext |
| `LeaderboardEntryResponse` | `dto/LeaderboardEntryResponse.java` | rank, userId, displayName, avatarUrl, totalXp, tierName, tierIcon |
| `CursorPage<T>` | `dto/CursorPage.java` | edges (List\<T\>), hasNextPage, endCursor — generic cursor pagination wrapper |

---

### ✅ Service (`GamificationService.java`)

| Method | Returns | Notes |
|--------|---------|-------|
| `getProfile(userId)` | `GamificationProfileResponse` | Looks up user gamification + current/next tier |
| `getXpHistory(userId, cursor, limit)` | `CursorPage<XpTransactionResponse>` | Cursor-based (base64-encoded `{t: createdAt, i: id}`). No COUNT query. |
| `getAchievements(userId)` | `List<AchievementResponse>` | All achievements with earned status + unlockedAt |
| `getRankHistory(userId)` | `List<RankHistoryResponse>` | User's rank progression (ordered newest first) |
| `getStreak(userId)` | `StreakResponse` | Current streak + current/next milestone with daysToNext |
| `getLeaderboard(cursor, limit)` | `CursorPage<LeaderboardEntryResponse>` | Cursor-based (base64 `{x: totalXp, u: userId, r: lastRank}`). Batch-loads User profiles + RankTiers to avoid N+1. |

**Cursor design:** Cursors are base64url-encoded JSON (no padding). `ObjectMapper` handles serialization. The WHERE clause uses composite-key pattern `(a < cursorA) OR (a = cursorA AND b < cursorB)` to handle ties correctly.

---

### 📋 Not Yet Implemented (Future Work)

| # | What | Notes |
|---|------|-------|
| 1 | `XP grant / streak bump` hot path | Need a service method that uses `findByIdForUpdate` (PESSIMISTIC_WRITE), increments `totalXp`, inserts `XpTransaction` with `runningBalance`, checks streak (compare `lastCompletedAt` to cutoff), and potentially inserts `RankHistory` on tier change. Callers: lesson completion, task completion, daily challenge. |
| 2 | `Achievement check/earn` | After XP grant, check if any unearned achievements now meet criteria. Need to compare `totalXp`, completed lessons, streak, etc. against achievement definitions. |
| 3 | `RankHistory` insertion logic | Detect tier change during XP grant and insert a `RankHistory` row. |
| 4 | `LeaderboardSnapshot` batch job | Scheduled job that runs daily/weekly to materialize `LeaderboardSnapshot` rows. The entity + repo are already in `com.riksa.leaderboard`. |
| 5 | Lesson completion → XP pipeline | Wire `Lesson` entity to actual course completion events. The `Lesson` entity (baseXp, difficulty) is designed for this but no completion logic exists yet. |

---

### 🔗 Dependencies Introduced This Session

- `GamificationService` now depends on `UserRepository` (from `com.riksa.auth`) — for leaderboard displayName/avatarUrl lookups. Batch-loaded via `findAllById()`.
- `GamificationService` now depends on `ObjectMapper` (from Jackson, auto-configured by Spring Boot) — for cursor encode/decode.
- `GamificationService` now depends on `RankHistoryRepository` and `StreakMilestoneRepository`.

---

*Start a new session by sharing this file with the AI agent.*
