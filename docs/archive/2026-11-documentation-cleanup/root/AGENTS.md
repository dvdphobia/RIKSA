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
| `GET` | `/api/v1/gamification/rank-history` | Bearer JWT | `?cursor=<base64>&limit=20` | `CursorPage<RankHistoryResponse>` | ✅ |
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
| 1 | ~~XP grant / streak bump hot path~~ | ✅ DONE — XpLedgerService, StreakService, RankService, AchievementService + listeners all implemented. LessonCompletedEvent published by LessonCompletionService. |
| 2 | ~~Achievement check/earn~~ | ✅ DONE — AchievementService + AchievementListener implemented. |
| 3 | ~~RankHistory insertion logic~~ | ✅ DONE — RankService + RankListener implemented. |
| 4 | ~~LeaderboardSnapshot batch job~~ | ✅ DONE — LeaderboardRefreshJob (@Scheduled every 5 min) + LeaderboardSnapshotService. |
| 5 | ~~Lesson completion → XP pipeline~~ | ✅ DONE — LessonController + LessonCompletionService implemented. Publishes LessonCompletedEvent which triggers the full gamification hot path. |
| 6 | AI Chat module | Schema exists (chat_sessions, chat_messages) but no Java code. Out of scope for v1. |

---

### 🔗 Dependencies Introduced This Session

- `GamificationService` now depends on `UserRepository` (from `com.riksa.auth`) — for leaderboard displayName/avatarUrl lookups. Batch-loaded via `findAllById()`.
- `GamificationService` now depends on `ObjectMapper` (from Jackson, auto-configured by Spring Boot) — for cursor encode/decode.
- `GamificationService` now depends on `RankHistoryRepository` and `StreakMilestoneRepository`.

The remaining 20% is the hot-path write pipeline — the actual logic that grants XP when a user completes a lesson/task, updates their streak, checks if they earned an achievement, and inserts rank history on tier change. That's the hardest part.

---

## Redis Infrastructure — Implemented (Aug 22, 2026)

- Redis 8.2 is part of Docker Compose and Spring Boot uses Lettuce through `spring-boot-starter-data-redis`.
- Auth endpoint rate limits use atomic Redis Lua counters with TTLs and fail closed with HTTP 503 when Redis is unavailable.
- OTP codes are HMAC-SHA256 hashed, stored with TTLs, verified atomically, and single-use. PostgreSQL lookup remains temporarily as a migration fallback for old OTPs.
- Telegram auth challenges use Redis TTL storage for new flows with a PostgreSQL fallback for challenges created before deployment.
- JWT user security state and short-lived gamification responses use Redis-backed Spring Cache with PostgreSQL fallback on cache errors.
- Current country/province leaderboards use Redis sorted sets with `totalXp + currentStreak * 5`; PostgreSQL snapshots remain the historical/fallback source.
- XP, streak, and profile events update Redis leaderboard projections after transaction commit.
- Leaderboard refresh, streak reset, and token cleanup jobs use Redis-backed ShedLock coordination.
- Redis integration tests use Testcontainers and automatically skip when Docker is unavailable.

---

*Start a new session by sharing this file with the AI agent.*
