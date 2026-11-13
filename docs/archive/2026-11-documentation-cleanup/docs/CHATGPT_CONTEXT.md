# RIKSA Backend — Full Context for AI Assistant

## Project Overview

RIKSA is a Cambodia-focused educational learning platform backend. Users take lessons, complete exercises, earn XP, climb rank tiers, maintain streaks, unlock achievements, interact on a social feed (GraphQL), and write blog posts. Built as a Spring Boot 4 monolith with PostgreSQL, designed to serve a Flutter mobile frontend.

## Tech Stack

- Java 21, Spring Boot 4.0.6, Maven (pom.xml)
- PostgreSQL 17 (docker-compose.yml), Flyway migrations (V1–V14), JPA/Hibernate with `ddl-auto=validate`
- Spring Security + JWT (jjwt 0.12.6), stateless, BCrypt passwords
- Spring for GraphQL (social module only — `src/main/resources/graphql/social.graphqls`)
- Google OAuth2 (google-oauth-client 1.36.0, google-api-client 2.7.1)
- Telegram bot integration for OTP (`src/main/java/com/riksa/auth/telegram/`)
- Lombok, spring-dotenv 4.0.0 (reads .env), Spring Actuator
- Docker: multi-stage build (Maven 3.9.16 / Eclipse Temurin JRE 21), healthcheck on `/actuator/health`
- CI: GitHub Actions (`.github/workflows/ci.yml`) — Postgres 17 service, `./mvnw verify`, Docker build
- Tests: JUnit 5 + Mockito + AssertJ, 145 test methods across 19 test files, all unit-level (no integration/@SpringBootTest tests)

## Architecture

Package-by-feature under `com.riksa.*`. 196 Java source files across 10 modules:

### auth/ — Authentication & User Management
- User registration, login, JWT access+refresh, logout, Google OAuth, OTP (SMS/Telegram), email verification, forgot/reset password, profile get/update
- REST at `/api/v1/auth/**`
- `SecurityConfig.java` permits `/api/v1/auth/**` and actuator health; everything else requires authentication. Method security via `@EnableMethodSecurity`.
- `JwtAuthenticationFilter` and `RateLimitFilter` added before `UsernamePasswordAuthenticationFilter`
- `AuthService.register()` creates a `UserGamification` row for the new user so the XP hot path doesn't crash on first lesson completion
- `OAuth2Service.googleAuth()` does the same for Google OAuth users
- Entities: `User`, `Province`, `Role`. Repos: `UserRepository`, `ProvinceRepository`
- OTP: `OtpService` generates codes, `OtpCode` entity, `OtpCodeRepository`, channels SMS + TELEGRAM
- Tokens: `RefreshToken`, `EmailVerificationToken` with cleanup via `TokenCleanupTask` (@Scheduled 3am)
- Telegram: `TelegramService` + `TelegramConfig` for bot-based OTP delivery

### learning/ — Lessons & Exercises
- Lesson catalog (filter by category/difficulty, paginated), lesson detail (with exercises, no answer key exposed), exercise attempt submission, lesson completion with reflection text
- REST at `/api/v1/lessons/**`
- `LessonCompletionService.complete()` publishes `LessonCompletedEvent` — the entry point to the gamification hot path
- Requires all exercises to have correct attempts before completion is allowed
- XP earned = lesson.baseXp + sum(exercise.points)
- Entities: `Lesson`, `Exercise`, `LessonCompletion`, `ExerciseAttempt`
- `ReflectionValidator` / `SimpleReflectionValidator` — validates reflection text (min length, non-blank), returns a score
- `ExerciseAttemptService.submitAttempt()` — upserts attempt per (user, exercise), keeps only latest result

### gamification/ — XP, Streaks, Ranks, Achievements
- REST at `/api/v1/gamification/**` — 6 GET endpoints: `/me`, `/xp`, `/achievements`, `/rank-history`, `/streak`, `/leaderboard`
- Cursor pagination: base64url-encoded JSON cursors, composite-key tie-breaking `(a < cursorA) OR (a = cursorA AND b < cursorB)`, no COUNT queries
- `CursorPage<T>` generic wrapper with `edges`, `hasNextPage`, `endCursor`
- Event-driven hot path — 4 listeners:
  - `XpLedgerListener` listens to `LessonCompletedEvent` → grants XP via `XpLedgerService.awardXp()` → publishes `XpGainedEvent`
  - `StreakListener` listens to `LessonCompletedEvent` → updates streak via `StreakService.updateStreak()` → publishes `StreakUpdatedEvent`
  - `RankListener` listens to `XpGainedEvent` → checks rank via `RankService.checkAndPromoteRank()` → publishes `RankChangedEvent` on tier change
  - `AchievementListener` listens to `XpGainedEvent`, `StreakUpdatedEvent`, `RankChangedEvent` → checks achievements via `AchievementService` → publishes `AchievementUnlockedEvent`
- All listeners use `@EventListener` (synchronous, same thread/transaction). Each wraps its body in try/catch and logs errors — failures are swallowed, not propagated.
- Concurrency: `UserGamification` has `@Version` (optimistic locking) + `findByIdForUpdate` (PESSIMISTIC_WRITE)
- Entities: `UserGamification`, `XpTransaction`, `RankTier`, `RankHistory`, `Achievement`, `UserAchievement`, `StreakMilestone`
- `StreakService.resetStaleStreaks()` exists but is never scheduled (no @Scheduled calls it)
- Seed data in V7 migration: 6 rank tiers, 8 streak milestones, 6 achievements
- BIGINT columns (V9 migration) for XP/streak values — entities use `long` types

### leaderboard/ — Snapshot-based Leaderboard
- REST at `/api/v1/leaderboard/**` — GET list (by scope + date), GET `/me` (current user's rank)
- `LeaderboardRefreshJob` — @Scheduled every 5 min, runs `LeaderboardSnapshotService.refreshSnapshots()`
- Snapshots by scope: COUNTRY (all users, scopeId="CAMBODIA") and PROVINCE (partitioned by province_id)
- Score = totalXp + (currentStreak * 5)
- `LeaderboardRefreshedEvent` published after each refresh cycle
- Uses raw JdbcTemplate for snapshot SQL queries (not JPA)
- Entity: `LeaderboardSnapshot`. Repo: `LeaderboardSnapshotRepository`

### social/ — Feed, Posts, Comments, Reactions, Shares, Categories
- GraphQL only at `/graphql` — schema in `src/main/resources/graphql/social.graphqls`
- Controllers use `@QueryMapping`, `@MutationMapping`, `@SchemaMapping` (Spring for GraphQL)
- Queries: `feed(categoryId, cursor, limit)`, `post(id)`, `categories`, `comments(postId, cursor, limit)`
- Mutations: `createPost`, `updatePost`, `deletePost`, `createComment`, `updateComment`, `deleteComment`, `react`, `removeReaction`, `sharePost`, `joinCategory`, `leaveCategory`
- Field resolvers: `Post.comments` (via `@SchemaMapping` in `PostController`), `Comment.replies` (via `@SchemaMapping` in `CommentController`)
- `SocialResponseResolver` — resolves cross-domain fields: `author` (User from auth via `UserRepository.findById`), `myReaction` (Reaction lookup), `category` (from `CategoryService.listCategories()`). Currently does 3 DB queries per post (N+1 problem — no batching).
- `PostCreatedEvent` is a plain record, NOT a `DomainEvent` — uses `ApplicationEventPublisher` directly instead of `EventPublisher` (inconsistent with rest of codebase)
- Feed cursor: simple UUID (post ID), no tie-breaking on `createdAt` — can skip posts with identical timestamps
- Entities: `Post`, `Comment`, `Reaction`, `Share`, `Category`, `UserCategoryMembership`
- Enums: `ReactionType` (UPVOTE, DOWNVOTE, HAHA, LOVE), `SharePlatform` (TELEGRAM, FACEBOOK, COPY_LINK)
- `ContentFilterService` — bad word filtering for posts/comments
- `ShareService` — increments share count, saves share record

### blog/ — Blog Posts, Likes, Views, Moderation
- REST at `/api/v1/blog/**` — GET list, POST create, GET detail, POST like, POST view, PATCH admin moderation
- `BlogModerationService` — approve/reject with events: publishes `BlogApprovedEvent` / `BlogRejectedEvent` (includes postId, authorId, moderatorId, reason)
- Duplicate moderation endpoint: also exists at `/api/v1/admin/blog/{id}/status` (BlogAdminController) — different contract (body params vs query param)
- `BlogEngagementService` — tracks views and likes
- `BadWordFilter` — content filtering utility
- Entities: `BlogPost`, `BlogLike`, `BlogView`. Enum: `BlogStatus` (PENDING, APPROVED, REJECTED)

### admin/ — Admin Panel
- REST at `/api/v1/admin/**` — `@PreAuthorize("hasRole('ADMIN')")` or `hasRole('MODERATOR')")` at controller class level
- `LessonAdminController` — CRUD + bulk create + GET list (paginated)
- `CategoryAdminController` — CRUD
- `BlogAdminController` — moderation (approve/reject with moderatorId from JWT)
- `UserAdminController` — role update + GET list (paginated)

### common/ — Cross-cutting Concerns
- `DomainEvent` — abstract base class extending Spring's `ApplicationEvent`. All cross-domain events extend this.
- `EventPublisher` — thin wrapper around `ApplicationEventPublisher`. Centralizes event publishing. Synchronous by default.
- `GlobalExceptionHandler` (`@RestControllerAdvice`) — maps: `ResourceNotFoundException`→404, `IllegalArgumentException`→400, `IllegalStateException`→409, `InvalidCursorException`→400, `ObjectOptimisticLockingFailureException`→409, generic `Exception`→500. JSON body: `{timestamp, status, error, message}`
- `CorrelationIdFilter` — adds `requestId` to MDC for log tracing
- `EnvironmentValidator` — validates required env vars at startup
- `TokenCleanupTask` — @Scheduled 3am, cleans expired refresh tokens and email verification tokens
- `GraphQlConfig` — GraphQL configuration
- `JacksonConfig` — Jackson ObjectMapper configuration
- Shared enums: `DifficultyLevel`, `TierType`, `ScopeType`, `AchievementCriteriaType`

### chatAi/ — AI Chat (Empty)
- 5 `.gitkeep` files, zero Java code
- DB tables exist in V1 migration: `chat_sessions`, `chat_messages`
- Out of scope for v1

## Event Flow (The Hot Path)

```
LessonCompletionService.complete()
  → publishes LessonCompletedEvent
    → XpLedgerListener.handleLessonCompleted()
        → XpLedgerService.awardXp() [PESSIMISTIC_WRITE, @Transactional]
        → publishes XpGainedEvent
          → RankListener.handleXpGained()
              → RankService.checkAndPromoteRank()
              → publishes RankChangedEvent (if tier changed)
          → AchievementListener.handleXpGained()
              → AchievementService.checkXpAchievements()
    → StreakListener.handleLessonCompleted()
        → StreakService.updateStreak() [PESSIMISTIC_WRITE, @Transactional]
        → publishes StreakUpdatedEvent
          → AchievementListener.handleStreakUpdated()
              → AchievementService.checkStreakAchievements()
          → (RankChangedEvent also triggers AchievementListener.handleRankChanged())
```

All listeners run synchronously in the same thread and transaction as the publisher. Each listener catches exceptions and logs them — failures are swallowed, not propagated to the caller. This means if XP grant fails, the user's lesson completion still succeeds but they get no XP — silently.

No `@Async` is used anywhere. No `@TransactionalEventListener` — all use `@EventListener`.

## Database Schema

14 Flyway migrations (V1–V14), `ddl-auto=validate` (schema is source of truth):

- V1: Initial schema — 24 tables (provinces, categories, users, lessons, exercises, lesson_completions, xp_transactions, user_gamification, streak_milestones, rank_tiers, rank_history, achievements, user_achievements, leaderboard_snapshots, user_category_memberships, posts, comments, reactions, shares, blog_posts, blog_views, blog_likes, chat_sessions, chat_messages)
- V2: refresh_tokens, otp_codes tables, phone_verified column
- V3: OtpChannel enum (SMS, TELEGRAM), rename phone_number to identifier
- V4: telegram_id column on users
- V5: Login lockout (failed_login_attempts, locked_until) + email_verified column
- V6: email_verification_tokens table
- V7: Seed gamification data (6 rank tiers, 8 streak milestones, 6 achievements)
- V8: user_gamification.version (optimistic locking)
- V9: BIGINT for XP columns (xp_transactions.amount, running_balance, user_gamification.total_xp, current_streak, highest_streak, rank_tiers.min_xp, achievements.criteria_value, streak_milestones.min_streak)
- V10: blog_posts.cover_url
- V11: blog_posts.rejection_reason
- V12: blog_likes.created_at (renamed from duplicate V9)
- V13: lessons.reflection_prompt, lesson_completions.reflection_text + score, exercise_attempts table
- V14: Backfill user_gamification rows for existing users

DB naming: snake_case, quoted identifiers (`"user_gamification"`), UUID primary keys via `uuid_generate_v4()`, `updated_at` triggers.

## Configuration

All config via env vars with defaults in `application.yml`, loaded by spring-dotenv from `.env`:

- Database: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
- JWT: JWT_SECRET (256-bit min), JWT_ACCESS_EXPIRATION (15min default), JWT_REFRESH_EXPIRATION (30days default)
- Google OAuth: GOOGLE_CLIENT_ID
- OTP: OTP_EXPIRATION_MINUTES (5), OTP_MAX_ATTEMPTS (3), OTP_CODE_LENGTH (6)
- Telegram: TELEGRAM_ENABLED, TELEGRAM_BOT_TOKEN, TELEGRAM_BOT_USERNAME
- CORS: CORS_ALLOWED_ORIGINS (http://localhost:3000)
- Rate limiting: RATE_LIMIT_AUTH_MAX (5), RATE_LIMIT_AUTH_WINDOW (60s)
- Login lockout: LOGIN_MAX_FAILED_ATTEMPTS (5), LOGIN_LOCKOUT_MINUTES (15)
- Server: PORT (8080)
- Actuator: health + info endpoints exposed, readiness/liveness probes enabled

docker-compose.yml: postgres:17 + app (builds from Dockerfile), postgres healthcheck, app depends on postgres healthy.

## Tests

- 145 test methods across 19 test files, all unit-level with Mockito mocks
- Framework: JUnit 5 + Mockito + AssertJ
- No integration tests (no @SpringBootTest, no Testcontainers, no H2)
- CI runs `./mvnw verify` with Postgres 17 service container

Modules with tests:
- auth: AuthServiceTest (15), OAuth2ServiceTest (2), OtpServiceTest (6), RateLimitFilterTest (3)
- blog: BlogServiceTest, BlogModerationServiceTest (6), BlogEngagementServiceTest, BadWordFilterTest
- social: PostServiceTest (10), CommentServiceTest (8), ReactionServiceTest (6), CategoryServiceTest (6), ContentFilterServiceTest (8)
- gamification: GamificationServiceTest (14), HotPathServiceTest (XpLedger, Streak, Rank, Achievement)
- leaderboard: LeaderboardQueryServiceTest (3)
- admin: CategoryAdminServiceTest (8), UserAdminServiceTest (6), LessonAdminServiceTest (9)

Modules with NO tests:
- learning (0 test files — LessonService, LessonCompletionService, ExerciseAttemptService, SimpleReflectionValidator all untested)
- SocialResponseResolver (0)
- ShareService (0)
- blog events (0)
- leaderboard events (0)
- common infrastructure (0)
- chatAi (0 — module is empty)

## Conventions & Patterns

- Package-by-feature: each module owns its own `entity/`, `repository/`, `dto/`, `service/`, `controller/`, `event/` packages
- Cross-domain communication via events only — no module writes to another module's tables
- Lombok throughout: `@Getter/@Setter/@RequiredArgsConstructor` on entities and services, `@Slf4j` for logging
- Controllers extract user ID from `SecurityContextHolder.getContext().getAuthentication()` via private `getCurrentUserId()` helper
- Admin endpoints use `@PreAuthorize` at controller class level
- Entities use `@CreationTimestamp`, `@Version`, `@PrePersist`/`@PreUpdate` for timestamps
- REST controllers are `@RestController`, social controllers are `@Controller` (GraphQL)
- Migrations use `IF NOT EXISTS` on ALTERs, sequential versioning
- No TODO/FIXME/XXX comments in the codebase

## Known Issues (from docs/FIX_PLAN.md)

Already fixed:
- Duplicate V9 Flyway migration (renamed to V12)
- UserGamification row not created on registration (fixed in AuthService + OAuth2Service + V14 backfill)
- Entity/DB BIGINT type mismatches (entities updated to long)
- BlogModerationService used jakarta.transaction.Transactional (changed to Spring's)
- UserResponse typo: avatartUrl → avatarUrl
- CommentRepository wrong Pageable import (java.awt.print → org.springframework.data.domain)

Remaining (not yet fixed):
- N+1 query explosion in SocialResponseResolver (3 queries per post in feed, no batching)
- Feed cursor has no tie-breaking on createdAt (can skip posts with same timestamp)
- Event pipeline all synchronous — listener failures silently swallowed, no retry/compensation
- StreakService.resetStaleStreaks() never scheduled
- PostCreatedEvent not a DomainEvent, uses ApplicationEventPublisher directly
- LessonListResponse missing "completed" and "exerciseCount" fields
- CompletionResponse missing "completionId" and "passed" fields
- Duplicate blog moderation endpoints (different contracts)
- 34 stale .gitkeep files in directories with real Java files
- No tests for learning module, SocialResponseResolver, ShareService, events
- No integration test for full hot path
- RIKSA_BUILD_GUIDE.md deprecated but still references old Spring Boot version

## Git State

- Branch: dev (tracks origin/dev)
- Also: main, origin/staging, origin/copilot/fix-failing-github-actions-job
- Last commits:
  - 5790a34 Refactor code structure for improved readability and maintainability
  - 8032a65 fix: security vulnerabilities, bugs, and add comprehensive unit tests
  - b6346e2 some plans
- 196 Java source files, 19 test files, 14 migrations, 34 .gitkeep files

## Documentation Files

- `docs/PROJECT_GUIDE.md` — 810 lines, current source of truth (architecture, endpoints, Flutter screen mapping, known gaps)
- `docs/RIKSA_BUILD_GUIDE.md` — deprecated, references Spring Boot 3.2 and REST social API (actually GraphQL)
- `docs/LEARNING_API_PLAN.md` — learning module API plan
- `docs/SOCIAL_MODULE_PLAN.md` — social module plan (may be stale)
- `docs/GAMIFICATION_PLAN.md` — gamification plan (may be stale)
- `docs/ADMIN_PANEL_GUIDE.md` — admin panel guide
- `docs/FIX_PLAN.md` — remaining fix plan with 15 items across 5 phases
- `AGENTS.md` — project context for AI agents (gamification module audit)
- `agent.md` — unclear purpose, similar name to AGENTS.md
- `flutter.md` — Flutter app overview
- `flutterPlan/` — 5 Flutter dev plan files + API contract reference
- `screenDetail/` — screen design specs
- `docs/wireframes/index.html` — wireframe HTML

## Open Questions

1. Event pipeline safety: if XpLedgerService.awardXp() fails, the user's lesson completion succeeds but they get no XP — silently. No retry, no compensation. Is this acceptable?
2. StreakService.resetStaleStreaks() is implemented but never scheduled. Intentional or forgotten?
3. PostCreatedEvent doesn't extend DomainEvent and uses ApplicationEventPublisher directly. Is anything supposed to listen to it?
4. Duplicate blog moderation endpoints — which is canonical?
5. Duplicate leaderboard endpoints — which does the Flutter app use?
6. SocialResponseResolver N+1 — 60 queries per feed load. Performance plan?
7. Feed cursor no tie-breaking — posts with same createdAt can be skipped. Known issue?
8. DB tables in docs/wireframes but missing from schema: user_follows, banner_url, user_interests, exercise_options, daily_challenges, posts.view_count. Are these v2?
9. Reactions enum has HAHA/LOVE but UI only shows up/down. Which side changes?
10. .env file exists in repo root. Is it gitignored? Does it contain real secrets?
11. agent.md vs AGENTS.md — two files with similar names. Which is authoritative?