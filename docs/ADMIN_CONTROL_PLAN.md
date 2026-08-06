# Admin Control & Monitoring — Design & Implementation Plan

## Overview

Riksa currently has admin coverage for **content authoring** (lessons, categories) and **blog moderation**, but almost nothing for **safety, user discipline, gamification ops, or observability**. This plan closes those gaps in priority order, in 7 phases.

**Guiding principles:**
- Every admin action is audit-logged (Phase 1 first, everything else builds on it)
- Moderation is soft (hide + reason), never hard-delete — content stays recoverable
- `MODERATOR` handles content; `ADMIN` handles users, XP, and config
- Reuse existing patterns: `AdminEndpoints` constants, `@PreAuthorize`, `GlobalExceptionHandler` (IllegalArgumentException → 400), cursor pagination for lists

---

## Current State Inventory

| Capability | Status | Where |
|---|---|---|
| Role changes, user list | ✅ | `UserAdminController` |
| Blog approve/reject + reason | ✅ | `BlogAdminController` → `BlogModerationService` |
| Category CRUD | ✅ | `CategoryAdminController` |
| Lesson CRUD + MD upload | ✅ | `LessonAdminController` |
| Login lockout (auto, 5 attempts) | ✅ | `User.lockedUntil` |
| Content filter (hardcoded) | ✅ | `ContentFilterService` |
| Audit log | ❌ | — |
| User suspend/ban | ❌ | `User` has no suspension fields |
| Post/comment moderation | ❌ | `Post`/`Comment` have no status field |
| Report queue | ❌ | no reporting tables |
| Stats/analytics endpoints | ❌ | data exists (`exercise_attempts`, `xp_transactions`) but unexposed |
| XP adjustment / gamification CRUD | ❌ | seed-only via `V7__seed_gamification_data.sql` |

Roles available: `USER`, `MODERATOR`, `ADMIN` (DB enum `"Role"`).
Latest migration: `V15__backfill_user_gamification.sql` → new migrations start at **V16**.

---

## Permission Matrix (target state)

| Capability | MODERATOR | ADMIN |
|---|:---:|:---:|
| Blog moderation | ✅ | ✅ |
| Post/comment hide, report queue | ✅ | ✅ |
| Read stats dashboard | ✅ | ✅ |
| User suspend/unsuspend | ❌ | ✅ |
| Role changes | ❌ | ✅ |
| XP adjustment, achievement revoke | ❌ | ✅ |
| Gamification config CRUD | ❌ | ✅ |
| Audit log query | ❌ | ✅ |

---

## Phase 1 — Audit Log (foundation)

**Why first:** every later phase writes audit entries. Retrofitting is painful.

### Schema — `V16__admin_audit_log.sql`

```sql
CREATE TABLE admin_audit_logs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id    UUID NOT NULL REFERENCES users(id),
    action      VARCHAR(60) NOT NULL,          -- e.g. USER_SUSPEND, POST_HIDE, XP_ADJUST
    target_type VARCHAR(30) NOT NULL,          -- USER, POST, COMMENT, BLOG, LESSON, XP
    target_id   UUID,
    details     JSONB,                         -- free-form: reason, old/new values
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_actor ON admin_audit_logs (actor_id, created_at DESC);
CREATE INDEX idx_audit_target ON admin_audit_logs (target_type, target_id);
CREATE INDEX idx_audit_created ON admin_audit_logs (created_at DESC);
```

### Java

| Artifact | Notes |
|---|---|
| `entity/AdminAuditLog.java` | standard entity |
| `repository/AdminAuditLogRepository.java` | `findByTargetTypeAndTargetId`, cursor/offset list |
| `service/AuditLogService.java` | `record(UUID actorId, String action, String targetType, UUID targetId, Map<String,Object> details)` — fire-and-forget, never throws into caller |
| `GET /api/v1/admin/audit-logs?targetType=&targetId=&page=&size=` | ADMIN only |

**Design decision:** explicit `auditLogService.record(...)` calls inside admin services — not AOP. Simpler, testable, no magic; the ~10 call sites are all in this plan.

### Tests
- `AuditLogServiceTest` — record persists, details serialized, failure to log doesn't break caller flow.

---

## Phase 2 — User Suspension

### Schema — `V17__user_suspension.sql`

```sql
ALTER TABLE users
    ADD COLUMN suspended_until TIMESTAMPTZ,
    ADD COLUMN suspension_reason VARCHAR(255);
```

### Java changes

| Change | Where |
|---|---|
| `suspendedUntil`, `suspensionReason` + `isSuspended()` | `User.java` |
| Reject suspended users at login with 403 `Account suspended until <date>` | auth login flow (same place `isLocked()` is checked) |
| Reject suspended users' valid JWTs | security filter chain (check on token validation) |
| `POST /api/v1/admin/users/{id}/suspension` — body `{reason, until}` | `UserAdminController` (ADMIN) |
| `DELETE /api/v1/admin/users/{id}/suspension` — lift | same |
| Audit: `USER_SUSPEND` / `USER_UNSUSPEND` | via `AuditLogService` |

**Edge cases:**
- Suspending a MODERATOR/ADMIN → reject (400), prevent admin lockout wars
- `until` in the past → 400
- Repeated suspension overwrites (history lives in audit log)

### Tests
- Login blocked while suspended, allowed after expiry
- Cannot suspend staff roles
- Audit entry written

---

## Phase 3 — Post & Comment Moderation

### Schema — `V18__content_moderation_fields.sql`

```sql
CREATE TYPE "ContentStatus" AS ENUM ('VISIBLE', 'HIDDEN');

ALTER TABLE posts ADD COLUMN status "ContentStatus" NOT NULL DEFAULT 'VISIBLE',
                  ADD COLUMN hidden_reason VARCHAR(255),
                  ADD COLUMN hidden_by UUID REFERENCES users(id),
                  ADD COLUMN hidden_at TIMESTAMPTZ;
CREATE INDEX idx_posts_status ON posts (status) WHERE status = 'HIDDEN';

ALTER TABLE comments ADD COLUMN status "ContentStatus" NOT NULL DEFAULT 'VISIBLE',
                     ADD COLUMN hidden_reason VARCHAR(255),
                     ADD COLUMN hidden_by UUID REFERENCES users(id),
                     ADD COLUMN hidden_at TIMESTAMPTZ;
```

### Java changes

| Change | Where |
|---|---|
| `status`, `hiddenReason`, `hiddenBy`, `hiddenAt` | `Post.java`, `Comment.java` |
| **Hide from all public reads** — feed, post detail, comment lists | `PostService`, `CommentService`, repository queries (`WHERE status = 'VISIBLE'`) |
| `PATCH /api/v1/admin/posts/{id}/status` — `{status: HIDDEN|VISIBLE, reason}` | new `SocialAdminController` (MODERATOR/ADMIN) |
| `PATCH /api/v1/admin/comments/{id}/status` | same |
| Author sees own hidden content flagged as "under review" (optional, v2) | — |
| Audit: `POST_HIDE`, `POST_RESTORE`, `COMMENT_HIDE`, `COMMENT_RESTORE` | |

**Critical:** audit every public query path in `PostService`/`CommentService`/feed — one missed query leaks hidden content.

### Tests
- Hidden post absent from feed + detail, comment lists
- Restore makes content visible again
- Hidden comment's children handling (hide thread vs orphan children — decision: hide only the comment, children remain but lose parent context → set `parent_id` display fallback)

---

## Phase 4 — Report Queue

### Schema — `V19__content_reports.sql`

```sql
CREATE TYPE "ReportTargetType" AS ENUM ('POST', 'COMMENT', 'BLOG');
CREATE TYPE "ReportStatus" AS ENUM ('OPEN', 'RESOLVED', 'DISMISSED');

CREATE TABLE content_reports (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id  UUID NOT NULL REFERENCES users(id),
    target_type  "ReportTargetType" NOT NULL,
    target_id    UUID NOT NULL,
    reason       VARCHAR(60) NOT NULL,        -- SPAM, HARASSMENT, NSFW, MISINFORMATION, OTHER
    details      TEXT,
    status       "ReportStatus" NOT NULL DEFAULT 'OPEN',
    resolved_by  UUID REFERENCES users(id),
    resolved_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (reporter_id, target_type, target_id)   -- one report per user per target
);
CREATE INDEX idx_reports_open ON content_reports (status, created_at) WHERE status = 'OPEN';
```

### Endpoints

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `POST` | `/api/v1/reports` | USER | File a report `{targetType, targetId, reason, details}` |
| `GET` | `/api/v1/admin/reports?status=OPEN&targetType=&cursor=&limit=` | MODERATOR | Queue, cursor-paginated, oldest first |
| `PATCH` | `/api/v1/admin/reports/{id}` | MODERATOR | `{status: RESOLVED|DISMISSED, action: HIDE|NONE}` — optionally hides target in same call |

**Flow:** report RESOLVED + action HIDE → delegates to Phase 3 hide logic (single transaction).

### Tests
- Duplicate report → 409
- Reporting nonexistent target → 404
- Resolve-with-hide hides the post and audit-logs both

---

## Phase 5 — Stats Dashboard Endpoints

All read-only, `GET /api/v1/admin/stats/*`, MODERATOR/ADMIN. Plain SQL aggregates — no new tables.

| Endpoint | Metrics | Source |
|---|---|---|
| `/stats/overview` | signups (24h/7d), posts (24h/7d), comments (24h/7d), lesson completions (24h/7d), pending blogs, open reports | `users`, `posts`, `comments`, `lesson_completions`, `blog_posts`, `content_reports` |
| `/stats/exercises?minAttempts=10` | per exercise: attempts, correct %, **failure rate** — sorted worst-first | `exercise_attempts` GROUP BY `exercise_id` |
| `/stats/gamification` | XP granted/day by `source_type` (last 30d), active streaks histogram | `xp_transactions`, `user_gamification` |
| `/stats/security` | currently locked accounts, suspended accounts, failed-login total | `users` |

**Why exercise failure rate matters:** a question failed by 95% of students is a broken question (wrong `answer_key`), not 95% weak students. This endpoint finds them.

### Tests
- `AdminStatsServiceTest` with mocked repos — aggregation mapping, empty-state handling.

---

## Phase 6 — Gamification Ops

### 6a. Manual XP adjustment

`xp_transactions.source_type` is `VARCHAR(50)` — no DB enum change needed. Reserve value `ADMIN_ADJUSTMENT`.

| Endpoint | `POST /api/v1/admin/gamification/users/{id}/xp-adjust` (ADMIN) |
|---|---|
| Body | `{amount: int (negative allowed), reason: string (required, ≥10 chars)}` |
| Logic | Reuse XP pipeline: write `XpTransaction` + update `UserGamification` (optimistic lock, existing `@Version`), re-evaluate rank tier |
| Guard | balance can't go below 0 → clamp or 400 (decision: **400**, explicit over silent) |
| Audit | `XP_ADJUST` with `{amount, reason, balanceBefore, balanceAfter}` |

### 6b. Gamification config CRUD (ADMIN)

| Resource | Endpoints | Notes |
|---|---|---|
| `Achievement` | CRUD `/admin/achievements` | editing criteria doesn't retroactively award |
| `RankTier` | CRUD `/admin/rank-tiers` | changing `min_xp` triggers no backfill (document this) |
| `StreakMilestone` | CRUD `/admin/streak-milestones` | |
| `UserAchievement` revoke | `DELETE /admin/users/{id}/achievements/{achievementId}` | audit `ACHIEVEMENT_REVOKE` |

### Tests
- XP adjust: negative below zero → 400; rank re-evaluation on adjust; audit details correct
- Revoke: idempotent 404 on missing

---

## Phase 7 — Nice-to-Haves (backlog, unordered)

| Item | Effort | Notes |
|---|---|---|
| Editable content filter | M | `filtered_words` table + admin CRUD; `ContentFilterService` reads cache instead of hardcoded list |
| Lesson draft/publish state | S | add `published` bool to `lessons`; public `LessonService` filters published only |
| Announcements | M | `announcements` table + public GET + admin CRUD |
| User activity view | M | `GET /admin/users/{id}/activity` — posts, attempts, XP, reports about/by user (support tool) |
| Bulk MD lesson export | S | inverse of upload — for backup/migration |

---

## Migration Summary

| Version | File | Phase |
|---|---|---|
| V16 | `V16__admin_audit_log.sql` | 1 |
| V17 | `V17__user_suspension.sql` | 2 |
| V18 | `V18__content_moderation_fields.sql` | 3 |
| V19 | `V19__content_reports.sql` | 4 |

Phases 5–7 add no schema (except Phase 7 items).

**Note:** `ddl-auto: validate` — every column in these migrations needs matching entity fields before deploy.

---

## Endpoint Summary (all new)

| # | Method | Path | Auth | Phase |
|---|---|---|---|---|
| 1 | GET | `/admin/audit-logs` | ADMIN | 1 |
| 2 | POST | `/admin/users/{id}/suspension` | ADMIN | 2 |
| 3 | DELETE | `/admin/users/{id}/suspension` | ADMIN | 2 |
| 4 | PATCH | `/admin/posts/{id}/status` | MODERATOR | 3 |
| 5 | PATCH | `/admin/comments/{id}/status` | MODERATOR | 3 |
| 6 | POST | `/reports` | USER | 4 |
| 7 | GET | `/admin/reports` | MODERATOR | 4 |
| 8 | PATCH | `/admin/reports/{id}` | MODERATOR | 4 |
| 9 | GET | `/admin/stats/overview` | MODERATOR | 5 |
| 10 | GET | `/admin/stats/exercises` | MODERATOR | 5 |
| 11 | GET | `/admin/stats/gamification` | MODERATOR | 5 |
| 12 | GET | `/admin/stats/security` | MODERATOR | 5 |
| 13 | POST | `/admin/gamification/users/{id}/xp-adjust` | ADMIN | 6 |
| 14 | CRUD | `/admin/achievements`, `/admin/rank-tiers`, `/admin/streak-milestones` | ADMIN | 6 |
| 15 | DELETE | `/admin/users/{id}/achievements/{achievementId}` | ADMIN | 6 |

(All under `/api/v1`.)

---

## Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Hidden content leaks through a missed query | Phase 3 includes explicit audit of every public read path in `PostService`/`CommentService` + integration test per query |
| Audit log write failure breaks admin action | `AuditLogService.record` catches + logs, never rethrows |
| XP adjustment race condition | reuse existing optimistic locking (`UserGamification.version`) — 409 on conflict, retry |
| Moderator abuse of power | all actions audit-logged; only ADMIN can read audit log; staff suspension blocked |
| Report table spam | unique(reporter, target) + existing auth rate-limit |

---

## Suggested Build Order & Sizing

| Phase | Estimate | Depends on |
|---|---|---|
| 1 — Audit log | 0.5 day | — |
| 2 — Suspension | 1 day | 1 |
| 3 — Post/comment moderation | 1.5 days | 1 |
| 4 — Report queue | 1 day | 3 |
| 5 — Stats | 1 day | 4 (open-reports metric) |
| 6 — Gamification ops | 1.5 days | 1 |
| 7 — Backlog | as needed | — |
