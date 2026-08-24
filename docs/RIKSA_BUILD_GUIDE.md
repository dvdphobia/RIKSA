# RIKSA Backend Build Guide

> **⚠️ DEPRECATED:** This document was the original build plan. It has been superseded by `docs/PROJECT_GUIDE.md` which is the current source of truth. Some details here (e.g., social as REST instead of GraphQL, Spring Boot 3.2 instead of 4.0.6) are outdated. Use PROJECT_GUIDE.md for current architecture and endpoint contracts.

*Single source of truth for the 3 backend developers. Everything needed to build the RIKSA API: architecture, endpoint inventory, file structure, domain boundaries, and task breakdown.*

---

## 1. Architecture Overview

**Stack:** Java 21, Spring Boot 3.2+, Maven, PostgreSQL 17, Flyway, JWT (access 15min / refresh 30 days), BCrypt, JPA/Hibernate, Spring Security, Spring ApplicationEvents (in-process event bus).

**Pattern:** Single-module Maven project, package-by-feature under `com.riksa.*`.

**Timezone:** All DB timestamps UTC. Business logic uses Asia/Phnom_Penh (UTC+7).

```
src/main/java/com/riksa/
├── RiksaApiApplication.java       (Boot runner)
├── admin/                         (Admin CRUD — lessons, categories, moderation)
├── auth/                          (Auth identity, JWT, OTP, OAuth — ✅ DONE)
├── common/                        (Cross-cutting: enums, exceptions, filters, scheduler — ✅ DONE)
├── learning/                      (Lesson catalog, completion — ⏳ to build)
├── gamification/                  (XP ledger, streaks, ranks, achievements — ⏳ partial)
├── leaderboard/                   (Daily snapshots, query APIs — ⏳ to build)
├── social/                        (Communities, posts, comments, reactions — ⏳ to build)
├── blog/                          (Blog posts, views, likes, moderation — ⏳ to build)
└── chat/                          (AI chat sessions, messages — ⏳ to build)
```

---

## 2. What's Already Built ✅

### Auth (Complete)
| Feature | Status |
|---------|--------|
| `POST /api/v1/auth/register` | Done |
| `POST /api/v1/auth/login` | Done |
| `POST /api/v1/auth/refresh` | Done |
| `POST /api/v1/auth/logout` | Done |
| `POST /api/v1/auth/google` | Done |
| `POST /api/v1/auth/otp/send` | Done |
| `POST /api/v1/auth/otp/verify` | Done |
| `GET /api/v1/auth/me` | Done |
| `PUT /api/v1/auth/me` | Done |
| `POST /api/v1/auth/forgot-password` | Done |
| `POST /api/v1/auth/reset-password` | Done |
| `POST /api/v1/auth/verify-email` | Done |
| JWT with refresh rotation | Done |
| Rate limiting (5/min on auth) | Done |
| Login lockout (5 fails → 15min) | Done |
| SecurityConfig + JwtAuthFilter | Done |

### Common (Complete)
| Feature | Status |
|---------|--------|
| GlobalExceptionHandler | Done |
| CorrelationIdFilter | Done |
| TokenCleanupTask (scheduled) | Done |
| EnvironmentValidator | Done |
| Shared enums (TierType, ScopeType, DifficultyLevel, AchievementCriteriaType) | Done |

### Gamification (Partial)
| Feature | Status |
|---------|--------|
| All entities (8) + repositories (7) | Done |
| `GET /api/v1/gamification/me` | Done |
| `GamificationService.getProfile()` | Done |

---

## 3. Domain Ownership Rules (Critical — No Cross-Domain Writes)

| Domain | Owns Writes To | Never Writes To |
|--------|---------------|-----------------|
| **Auth** | `users`, `refresh_tokens`, `otp_codes` | `xp_transactions`, `user_gamification` |
| **Admin** | `lessons`, `exercises`, `categories` | `xp_transactions`, `user_gamification`, `posts` |
| **Learning** | `lesson_completitions` | `xp_transactions`, `user_gamification` |
| **Gamification** | `xp_transactions`, `user_gamification`, `rank_history`, `user_achievements` | `lesson_completions`, `posts`, `comments` |
| **Social** | `posts`, `comments`, `reactions`, `shares` | `xp_transactions`, `user_gamification` |
| **Content** | `blog_posts`, `blog_views`, `blog_likes` | `xp_transactions`, `user_gamification` |
| **Leaderboard** | `leaderboard_snapshots` | `user_gamification` |
| **AI** | `chat_sessions`, `chat_messages` | **Read-only** — never writes to any other domain |

**Rule:** If a feature needs to award XP (e.g., lesson completion, streak milestone), it **must publish an event** and let Gamification listen and write the `xp_transactions` row. Never duplicate XP logic.

---

## 4. Event Bus (Cross-Domain Communication)

All cross-domain communication uses Spring ApplicationEvents:

```
Learning ──LESSON_COMPLETED──> Gamification (XP + Streak + Achievement)
Gamification:
  ├── XP_GAINED ──> RankEngine
  ├── STREAK_UPDATED ──> AchievementEngine
  ├── RANK_CHANGED ──> AchievementEngine + AI (context update)
  └── ACHIEVEMENT_UNLOCKED ──> AI (context update)
Social ──POST_CREATED ──> AI (context update)
Content ──BLOG_APPROVED/REJECTED ──> AI
Leaderboard ──LEADERBOARD_REFRESHED ──> AI
```

### Event Classes to Create
```
com.riksa.common.event/
├── DomainEvent.java               (marker interface)
├── EventPublisher.java            (wrapper around ApplicationEventPublisher)
```

```
Per-domain event classes (in their own package):
learning/event/LessonCompletedEvent.java
gamification/event/XpGainedEvent.java
gamification/event/StreakUpdatedEvent.java
gamification/event/RankChangedEvent.java
gamification/event/AchievementUnlockedEvent.java
social/event/PostCreatedEvent.java
blog/event/BlogApprovedEvent.java
blog/event/BlogRejectedEvent.java
leaderboard/event/LeaderboardRefreshedEvent.java
```

---

## 5. Complete API Contract

### 5.1 Admin Endpoints (Admin-Only — `@PreAuthorize("hasRole('MODERATOR')")`)

Lessons are created by the team only. Users cannot create lessons.

```
POST   /api/v1/admin/lessons              — Create lesson + exercises
PUT    /api/v1/admin/lessons/{id}         — Update lesson
DELETE /api/v1/admin/lessons/{id}         — Delete lesson
POST   /api/v1/admin/lessons/bulk        — Bulk upload lessons (CSV/JSON)

POST   /api/v1/admin/categories           — Create community category
PUT    /api/v1/admin/categories/{id}      — Update category
DELETE /api/v1/admin/categories/{id}      — Delete category

PATCH  /api/v1/admin/blog/{id}/status      — Approve/reject blog post
POST   /api/v1/admin/users/{id}/role       — Change user role
```

### 5.2 Auth Endpoints (Public + Authenticated)

```
POST /api/v1/auth/register           — Register new user  [PUBLIC]
POST /api/v1/auth/login              — Login  [PUBLIC]
POST /api/v1/auth/refresh            — Refresh JWT  [PUBLIC]
POST /api/v1/auth/logout             — Logout  [AUTH]
POST /api/v1/auth/google             — Google OAuth  [PUBLIC]
POST /api/v1/auth/otp/send           — Send OTP  [PUBLIC]
POST /api/v1/auth/otp/verify         — Verify OTP  [PUBLIC]
GET  /api/v1/auth/me                 — My profile  [AUTH]
PUT  /api/v1/auth/me                 — Update profile  [AUTH]
POST /api/v1/auth/forgot-password    — Forgot password  [PUBLIC]
POST /api/v1/auth/reset-password     — Reset password  [PUBLIC]
POST /api/v1/auth/verify-email       — Verify email  [PUBLIC]
GET  /api/v1/profiles/{userId}       — Public user profile + gamification  [PUBLIC]
```

### 5.3 Learning Endpoints (Student Only)

```
GET  /api/v1/lessons?page=0&size=20&categoryId=...&difficulty=EASY
     → Paginated lesson list with completion status  [AUTH]

GET  /api/v1/lessons/{id}
     → Lesson detail with exercises  [AUTH]

POST /api/v1/lessons/{id}/complete
     → Submit answers, get score + XP  [AUTH]
     → Publishes LessonCompletedEvent (async XP/streak/rank/achievement)
     → Response: { completionId, lessonId, xpEarned, score, passed }
     → Frontend polls GET /api/v1/gamification/me or WebSocket for updates
```

### 5.4 Gamification Endpoints

```
GET /api/v1/gamification/me
    → { totalXp, currentStreak, highestStreak, currentRank (tier, minXp, nextTier, progress%), lastCompletedAt, nextMilestone }

GET /api/v1/gamification/xp-history?page=0&size=20
    → Paginated XP transactions: { amount, sourceType, sourceId, runningBalance, createdAt }

GET /api/v1/gamification/achievements
    → { unlocked: [...], locked: [...] with progressPercent }

GET /api/v1/gamification/rank-history
    → Paginated rank promotions: { scope, oldRank, newRank, changedAt }
```

### 5.5 Social Endpoints

```
GET  /api/v1/categories
     → All categories (created by admin, read-only for users)  [AUTH]

POST /api/v1/categories/{slug}/join
     → Join a community category  [AUTH]

POST /api/v1/categories/{slug}/leave
     → Leave a community category  [AUTH]

GET  /api/v1/feed?page=0&size=20&sort=NEWEST
     → Posts from user's joined categories  [AUTH]

POST /api/v1/posts
     → Create post (title, content, mediaUrl, categoryId)  [AUTH]
     → Must be member of category
     → Content relevance check: must match STEM/Tech/History topics

GET  /api/v1/posts/{id}
     → Post detail with comments  [AUTH]

POST /api/v1/posts/{id}/comments
     → Comment on post (supports nested via parentId)  [AUTH]

POST /api/v1/reactions
     → { targetType: "POST"|"COMMENT", targetId, type: "UPVOTE"|"DOWNVOTE"|"LOVE"|"HAHA" }  [AUTH]
     → Upsert: toggle off if same type, update if different
```

### 5.6 Blog Endpoints

```
GET  /api/v1/blog?page=0&size=20&status=APPROVED&categoryId=uuid
     → Paginated blog posts  [AUTH]

POST /api/v1/blog
     → Create blog post (status = PENDING)  [AUTH]
     → Bad-word filter applied (Khmer + English)

GET  /api/v1/blog/{id}
     → Blog detail (+ increments view count)  [AUTH]

POST /api/v1/blog/{id}/like
     → Toggle like  [AUTH]

POST /api/v1/blog/{id}/view
     → Track guest view (nullable userId)  [PUBLIC]
```

### 5.7 Leaderboard Endpoints (Khmer/Cambodia Focus)

```
GET /api/v1/leaderboard?scope=PROVINCE&scopeId=uuid&date=2024-05-30&page=0&size=50
    → Paginated by scope (PROVINCE or COUNTRY — COUNTRY = Cambodia default)  [AUTH]

GET /api/v1/leaderboard/me?scope=PROVINCE&scopeId=uuid
    → Personal rank: { rank, score, tier, provinceRank, countryRank, trend: UP/DOWN/SAME }  [AUTH]
```

### 5.8 AI/Chat Endpoints

```
POST /api/v1/chat/sessions
     → Create new chat session  [AUTH]

GET  /api/v1/chat/sessions
     → List my sessions  [AUTH]

POST /api/v1/chat/sessions/{id}/messages
     → Send message, get AI response with context  [AUTH]
```

---

## 6. Database Tables (Already Exist via Flyway V1-V8)

| Table | Domain | Notes |
|-------|--------|-------|
| `users` | Auth | ✅ exists |
| `provinces` | Auth | ✅ Cambodian provinces seeded |
| `refresh_tokens` | Auth | ✅ |
| `otp_codes` | Auth | ✅ |
| `email_verification_tokens` | Auth | ✅ |
| `lessons` | Admin/Learning | ✅ schema, needs admin CRUD |
| `exercises` | Admin/Learning | ✅ schema, needs admin CRUD |
| `lesson_completions` | Learning | ✅ schema |
| `xp_transactions` | Gamification | ✅ schema |
| `user_gamification` | Gamification | ✅ schema (with optimistic lock V8) |
| `rank_tiers` | Gamification | ✅ seeded (6 tiers) |
| `rank_history` | Gamification | ✅ schema |
| `achievements` | Gamification | ✅ seeded (6 achievements) |
| `user_achievements` | Gamification | ✅ schema |
| `streak_milestones` | Gamification | ✅ seeded (8 milestones) |
| `categories` | Admin/Social | ✅ schema |
| `user_category_memberships` | Social | ✅ schema |
| `posts` | Social | ✅ schema |
| `comments` | Social | ✅ schema |
| `reactions` | Social | ✅ schema |
| `shares` | Social | ✅ schema |
| `blog_posts` | Blog | ✅ schema |
| `blog_views` | Blog | ✅ schema |
| `blog_likes` | Blog | ✅ schema |
| `leaderboard_snapshots` | Leaderboard | ✅ schema |
| `chat_sessions` | AI | ✅ schema |
| `chat_messages` | AI | ✅ schema |
| `user_activity_summary` | AI | ✅ DB view |

---

## 7. File Inventory (Java Files to Create)

### Admin Package (`com.riksa.admin`)
```
controller/
├── LessonAdminController.java      (CRUD lessons/exercises)
├── CategoryAdminController.java    (CRUD categories)
└── BlogAdminController.java        (moderation approve/reject)
service/
├── LessonAdminService.java
├── CategoryAdminService.java
└── BlogAdminService.java
dto/
├── LessonCreateRequest.java
├── LessonUpdateRequest.java
├── LessonResponse.java
├── ExerciseRequest.java
├── CategoryRequest.java
├── ModerationRequest.java
└── ...
```

### Common Package (`com.riksa.common`) — Create Events
```
event/
├── DomainEvent.java                (marker interface)
└── EventPublisher.java             (wrapper bean)
```

### Learning Package (`com.riksa.learning`)
```
entity/
├── Lesson.java                     (JPA mapping to lessons table)
├── Exercise.java                   (JPA mapping to exercises table)
└── LessonCompletion.java           (JPA mapping to lesson_completions)
repository/
├── LessonRepository.java
├── ExerciseRepository.java
└── LessonCompletionRepository.java
service/
├── LessonService.java             (catalog queries)
└── LessonCompletionService.java   (answer validation, score, event publish)
controller/
└── LessonController.java          (GET /lessons, GET /lessons/{id}, POST /lessons/{id}/complete)
dto/
├── LessonListResponse.java
├── LessonDetailResponse.java
├── ExerciseResponse.java
├── CompletionRequest.java
├── CompletionResponse.java
└── AnswerResult.java
event/
└── LessonCompletedEvent.java      (userId, lessonId, xpEarned)
```

### Gamification Package (`com.riksa.gamification`) — Add To Existing
```
service/ — NEW:
├── XpLedgerService.java           (append XP, update balance, publish XP_GAINED)
├── StreakService.java             (track current/highest streak, last_completed_at)
├── RankService.java               (check thresholds, insert rank_history)
└── AchievementService.java        (evaluate criteria, unlock, publish)
listener/ — NEW:
├── XpLedgerListener.java          (@EventListener for LESSON_COMPLETED)
├── StreakListener.java            (@EventListener for LESSON_COMPLETED)
├── RankListener.java              (@EventListener for XP_GAINED)
└── AchievementListener.java       (@EventListener for XP_GAINED, STREAK_UPDATED, RANK_CHANGED)
controller/ — ADD:
└── GamificationController.java    (add xp-history, achievements, rank-history)
event/ — NEW:
├── XpGainedEvent.java
├── StreakUpdatedEvent.java
├── RankChangedEvent.java
└── AchievementUnlockedEvent.java
dto/ — ADD:
├── XpTransactionResponse.java
├── AchievementResponse.java
├── RankHistoryResponse.java
└── StreakResponse.java
```

### Social Package (`com.riksa.social`) — NEW
```
entity/
├── Category.java
├── UserCategoryMembership.java
├── Post.java
├── Comment.java
├── Reaction.java
└── Share.java
repository/
├── CategoryRepository.java
├── UserCategoryMembershipRepository.java
├── PostRepository.java
├── CommentRepository.java
├── ReactionRepository.java
└── ShareRepository.java
service/
├── CategoryService.java
├── FeedService.java
├── PostService.java
├── CommentService.java
├── ReactionService.java
└── ContentFilterService.java      (STEM relevance keyword check)
controller/
├── CategoryController.java
├── FeedController.java
├── PostController.java
├── CommentController.java
└── ReactionController.java
dto/
├── CategoryResponse.java
├── PostResponse.java
├── PostCreateRequest.java
├── CommentResponse.java
├── CommentRequest.java
├── ReactionRequest.java
└── FeedPageResponse.java
event/
└── PostCreatedEvent.java
```

### Blog Package (`com.riksa.blog`) — NEW
```
entity/
├── BlogPost.java
├── BlogView.java
└── BlogLike.java
repository/
├── BlogPostRepository.java
├── BlogViewRepository.java
└── BlogLikeRepository.java
service/
├── BlogService.java
├── BlogModerationService.java
└── BlogEngagementService.java
controller/
└── BlogController.java
dto/
├── BlogPostRequest.java
├── BlogPostResponse.java
├── BlogListResponse.java
└── ModerationRequest.java
util/
└── BadWordFilter.java             (Khmer + English hardcoded list)
event/
├── BlogPublishedEvent.java
├── BlogApprovedEvent.java
└── BlogRejectedEvent.java
```

### Leaderboard Package (`com.riksa.leaderboard`) — ADD
```
entity — EXISTS:
└── LeaderboardSnapshot.java
repository — EXISTS:
└── LeaderboardSnapshotRepository.java
service — NEW:
├── LeaderboardSnapshotService.java
└── LeaderboardQueryService.java
controller — NEW:
└── LeaderboardController.java
dto — NEW:
├── LeaderboardRowResponse.java
└── MyRankResponse.java
job — NEW:
└── LeaderboardRefreshJob.java     (@Scheduled 00:05 Asia/Phnom_Penh)
event — NEW:
└── LeaderboardRefreshedEvent.java
```

### AI/Chat Package (`com.riksa.chat`) — NEW
```
entity/
├── ChatSession.java
└── ChatMessage.java
repository/
├── ChatSessionRepository.java
└── ChatMessageRepository.java
service/
├── ChatService.java
└── ContextAssembler.java
controller/
└── ChatController.java
dto/
├── ChatSessionResponse.java
├── CreateSessionResponse.java
├── SendMessageRequest.java
└── SendMessageResponse.java
client/
└── LlmClient.java                 (stub interface)
event/
└── ContextUpdateListener.java
```

---

## 8. Team Assignment (3 Developers — 15 Days)

### Week 1 — Foundation + Core Domains

#### Dev A — Event Infra + Gamification Engine (Days 1-5)
| Day | Tasks |
|-----|-------|
| 1 | Create `DomainEvent` marker, `EventPublisher` in `common/event/`. Create all event classes across domains. |
| 2 | `XpLedgerService` + `XpLedgerListener` (listen `LESSON_COMPLETED`, write XP, publish `XP_GAINED`). |
| 3 | `StreakService` + `StreakListener` (listen `LESSON_COMPLETED`, track streak, publish `STREAK_UPDATED`). |
| 4 | `RankService` + `RankListener` (listen `XP_GAINED`, check thresholds, insert history, publish `RANK_CHANGED`). |
| 5 | `AchievementService` + `AchievementListener` (listen 3 events, evaluate rules, unlock, publish). Add remaining gamification endpoints: xp-history, achievements, rank-history. |

#### Dev B — Learning Domain (Days 1-4)
| Day | Tasks |
|-----|-------|
| 1 | `Lesson`, `Exercise`, `LessonCompletion` entities + repositories in `learning/`. |
| 2 | `LessonService` — catalog queries with pagination, filtering, completion status. `LessonController` — GET list + detail. |
| 3 | `LessonCompletionService` — answer validation, score calculation (70% pass), XP math. |
| 4 | Wire `LessonCompletedEvent` publish. Integration test: complete → event → gamification listener receives. |

#### Dev C — Social Domain (Days 1-5)
| Day | Tasks |
|-----|-------|
| 1 | `Category`, `UserCategoryMembership`, `Post`, `Comment`, `Reaction`, `Share` entities + repositories in `social/`. |
| 2 | `CategoryController` + `CategoryService` (list, join, leave). `FeedService` (posts from joined categories, paginated). |
| 3 | `PostService` + `PostController` (create with member check + content relevance filter, get detail). |
| 4 | `CommentService` + `CommentController` (create, nested replies via parentId). |
| 5 | `ReactionService` + `ReactionController` (upsert, counter sync, toggle off). `PostCreatedEvent` publish. |

### Week 2 — Content + Leaderboard + AI

#### Dev A — Blog Domain (Days 6-8)
| Day | Tasks |
|-----|-------|
| 6 | `BlogPost`, `BlogView`, `BlogLike` entities + repositories in `blog/`. `BadWordFilter` (Khmer + English). |
| 7 | `BlogService` — create (status PENDING, bad-word check), list with pagination + status filter. |
| 8 | `BlogModerationService` — approve/reject. `BlogEngagementService` — view tracking, like toggle. Wire blog events. |

#### Dev B — Leaderboard Domain (Days 6-8)
| Day | Tasks |
|-----|-------|
| 6 | `LeaderboardSnapshotService` — batch procedure: score = total_xp + (current_streak * 5), rank within PROVINCE + COUNTRY scopes. |
| 7 | `LeaderboardRefreshJob` — `@Scheduled` at 00:05 Asia/Phnom_Penh with lock guard. |
| 8 | `LeaderboardController` — list (paginated by scope/date) + my rank (province + country + trend). Wire event. |

#### Dev C — AI/Chat Domain (Days 6-9)
| Day | Tasks |
|-----|-------|
| 6 | `ChatSession`, `ChatMessage` entities + repositories in `chat/`. |
| 7 | `ChatService` — create session, send message, build context via `ContextAssembler`. |
| 8 | `LlmClient` stub interface. `ChatController` — session CRUD + message endpoints. |
| 9 | `ContextUpdateListener` — listens to `LESSON_COMPLETED`, `RANK_CHANGED`, `ACHIEVEMENT_UNLOCKED`, `LEADERBOARD_REFRESHED`. Invalidate context cache on event. |

### Week 3 — Admin Endpoints + Polish (Days 10-15)

#### All 3 Devs

| Day | Tasks | Assign To |
|-----|-------|-----------|
| 10 | `LessonAdminController` — CRUD lessons + exercises bulk upload | Dev B |
| 10 | `CategoryAdminController` — CRUD categories | Dev C |
| 10 | `BlogAdminController` — moderation endpoint (already exists in blog, expose via admin prefix) | Dev A |
| 11 | Security review: `@PreAuthorize` on all admin endpoints, verify rate limit path fix (`/api/auth/` → `/api/v1/auth/`) | Dev A |
| 11 | Pagination on all list endpoints, DB index audit | Dev B |
| 11 | Integration test: full user journey (register → complete lesson → rank up → leaderboard → chat) | Dev C |
| 12-13 | Fix rate limit filter path. CORS verify. Lock API contract. | Dev A |
| 12-13 | Add Swagger/OpenAPI annotations → generate `api-spec.yaml` | Dev B |
| 12-13 | Write remaining integration tests (event chain, social flows, blog moderation) | Dev C |
| 14 | Buffer — unexpected issues, code review, refactoring | All |
| 15 | Final integration test on staging, deploy prep | All |

---

## 9. Coding Conventions (Avoid Conflicts)

### Package Structure
```
controller/    — @RestController + @RequestMapping("/api/v1/{domain}")
service/       — @Service, inject repositories + EventPublisher
entity/        — @Entity, match Flyway table exactly
repository/    — extends JpaRepository
dto/           — request/response POJOs
event/         — extends DomainEvent
listener/      — @EventListener on domain events
```

### Code Patterns to Follow

**Entity pattern** (copy existing `UserGamification.java` style):
```java
@Entity
@Table(name = "table_name")
@Getter @Setter @NoArgsConstructor
public class EntityName {
    @Id private UUID id;
    // ... columns
}
```

**Controller pattern** (copy existing `GamificationController.java` style):
```java
@RestController
@RequestMapping("/api/v1/{domain}")
@RequiredArgsConstructor
public class XController {
    private final XService xService;
    private final JwtService jwtService;

    private UUID extractUserId(String authHeader) {
        // same pattern as existing controllers
    }
}
```

**Event publish pattern:**
```java
// In service, after write:
eventPublisher.publish(new LessonCompletedEvent(this, userId, lessonId, xpEarned));
```

**Event listener pattern:**
```java
@Component
@RequiredArgsConstructor
public class XListener {
    private final XService xService;

    @EventListener
    public void handleEvent(LessonCompletedEvent event) {
        xService.process(event);
    }
}
```

### Avoiding Conflicts

| Rule | Reason |
|------|--------|
| Never edit another dev's domain package | Clear ownership |
| Never write to another domain's DB table | Use events instead |
| Always use JPA Specifications for dynamic queries | No SQL injection |
| Run `mvn clean install` before pushing | Catch compile errors |
| Prefix branch with domain name: `feature/learning-lesson-list` | Clear branch ownership |

---

## 10. Content Relevance Filter (Social Posts)

Posts created in social must be relevant to STEM/Tech/History/Education topics.

**V1 approach:** Keyword-based scan in `ContentFilterService`:
- Maintain a set of Khmer + English STEM keywords (e.g., "science", "គណិតវិទ្យា", "programming", "technology", "history", "biology")
- On `POST /api/v1/posts`, scan title + content for at least N keyword matches
- If below threshold → reject with 400: "Post must be related to STEM, Tech, or History"
- Admin can override via role

**Future:** Upgrade to ML topic classifier.

---

## 11. Leaderboard — Khmer/Cambodia Focus

- `COUNTRY` scope defaults to Cambodia (single country focus)
- `PROVINCE` scope uses the `provinces` table (seeded with 6 Cambodian provinces)
- Score formula: `total_xp + (current_streak * 5)` rewards consistency
- Only `MODERATOR`/`ADMIN` can view all provinces; regular users see their own province + country

---

## 12. Build Commands

```bash
# Compile
./mvnw clean install -DskipTests

# Run
./mvnw spring-boot:run

# Health check
curl http://localhost:8080/api/v1/health

# Run tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=GamificationServiceTest
```

---

## 13. Unresolved Decisions (Resolve Before Week 2)

| # | Question | Current Assumption | Decision Needed By |
|---|----------|-------------------|-------------------|
| 1 | Should social posts/reactions award XP? | No — keep XP learning-only | End of Week 1 |
| 2 | Should blog contributions feed into leaderboard? | No — separate engagement | End of Week 1 |
| 3 | OTP SMS provider for production? | Console log for dev, Telegram for test | Before launch |
| 4 | File uploads (avatars, media)? | Local for dev, S3 for prod | Before launch |
| 5 | WebSocket for real-time notifications? | Polling first, WebSocket later | Before launch |
| 6 | Post relevance threshold (min keywords)? | TBD — need to test | Before Week 2 |

---

*This is the single backend build document. All endpoints and structures reference the existing codebase at `/api/v1/*`. Do not create multi-module structure — keep everything under `com.riksa.*` in a single Maven module.*
