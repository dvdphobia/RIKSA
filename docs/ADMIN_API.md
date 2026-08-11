# Riksa Admin API — Complete Reference for the Flutter Web Panel

> Everything you need to build the admin web interface. No backend knowledge required —
> every endpoint, parameter, request body, response shape, enum and error case is listed here.

**Base URL:** `http://<host>:8080/api/v1/admin`
(All paths below are relative to `/api/v1/admin` unless stated otherwise.)

---

## Table of Contents

1. [Authentication & Authorization](#1-authentication--authorization)
2. [Error Format](#2-error-format)
3. [Pagination](#3-pagination)
4. [Enum Reference](#4-enum-reference)
5. [Dashboard Stats](#5-dashboard-stats)
6. [User Management](#6-user-management)
7. [Lessons](#7-lessons)
8. [Categories](#8-categories)
9. [Report Moderation](#9-report-moderation)
10. [Blog Moderation](#10-blog-moderation)
11. [Social Moderation (Posts & Comments)](#11-social-moderation-posts--comments)
12. [Audit Logs](#12-audit-logs)
13. [Flutter Implementation Notes](#13-flutter-implementation-notes)
14. [Suggested Screen Map](#14-suggested-screen-map)

---

## 1. Authentication & Authorization

All admin endpoints require a JWT access token:

```
Authorization: Bearer <accessToken>
```

Get tokens via the auth module (see `docs/AUTH_API.md`):

```
POST /api/v1/auth/login     { "username": "...", "password": "..." }
                            → { accessToken, refreshToken, ... }
POST /api/v1/auth/refresh   → new token pair when access token expires (15 min lifetime)
```

> The logged-in account must have role `ADMIN` or `MODERATOR`. If a `USER` account calls any
> endpoint below, the API returns `403 Forbidden`.

### Permission matrix

| Module | Base path | Required role |
|---|---|---|
| Dashboard stats | `/stats` | MODERATOR or ADMIN |
| Lessons | `/lessons` | MODERATOR or ADMIN |
| Categories | `/categories` | MODERATOR or ADMIN |
| Reports | `/reports` | MODERATOR or ADMIN |
| Blog moderation | `/blog` | MODERATOR or ADMIN |
| Social moderation | `/posts`, `/comments` | MODERATOR or ADMIN |
| User management | `/users` | **ADMIN only** |
| Audit logs | `/audit-logs` | **ADMIN only** |

**Flutter tip:** hide the *Users* and *Audit Logs* nav items when the logged-in user's role is `MODERATOR`.

---

## 2. Error Format

Every error — regardless of cause — uses this JSON shape:

```json
{
  "timestamp": "2026-08-06T13:45:00.123456Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Suspension end date must be in the future"
}
```

| HTTP status | When |
|---|---|
| `400` | Validation failure, malformed body, invalid enum value, business-rule violation (message explains it — safe to show in a snackbar) |
| `401` | Missing/expired/invalid token → trigger refresh flow, then re-login |
| `403` | Authenticated but wrong role |
| `404` | Resource not found (`ResourceNotFoundException`) |
| `409` | Conflict — e.g. blog post already moderated, concurrent modification |
| `500` | Unexpected server error |

Invalid enum values specifically:
- In a **JSON body** → `400` with message `"Malformed request body"`
- In a **query param** → `400` with message `"Invalid value for parameter '<name>'"`

---

## 3. Pagination

Two pagination styles exist. Both are needed.

### 3.1 Offset pagination — Spring `Page<T>`

Used by: users, lessons, posts, comments, audit logs.

Request params: `page` (0-based, default `0`) and `size` (default `20`, clamped to `1..100`).

Response shape (fields you care about in **bold**):

```json
{
  "content": [ ... ],            // the rows
  "number": 0,                   // current page index (0-based)
  "size": 20,
  "totalElements": 137,          // total rows across all pages
  "totalPages": 7,
  "first": true,
  "last": false,
  "empty": false
  // plus: pageable, sort, numberOfElements — ignore these
}
```

Dart model needs only: `content`, `number`, `size`, `totalElements`, `totalPages`, `first`, `last`, `empty`.
This maps directly to Flutter `PaginatedDataTable` needs.

### 3.2 Cursor pagination — `CursorPage<T>`

Used by: reports list.

Request params: `cursor` (opaque string, omit on first page) and `limit` (default `20`, clamped `1..50`).

```json
{
  "edges": [ ... ],              // the rows
  "hasNextPage": true,
  "endCursor": "eyJ0Ijoi..."     // ABSENT (not null) when there is no next page
}
```

Loop: keep the last received `endCursor`, pass it as `cursor` on the next request, stop when `hasNextPage == false`. Never decode or build cursors yourself — treat them as opaque strings.

### 3.3 Custom blog list shape

`GET /blog` returns a simpler custom wrapper (see section 10):

```json
{ "content": [...], "page": 0, "size": 20, "totalElements": 5, "totalPages": 1 }
```

---

## 4. Enum Reference

All enums serialize as **UPPERCASE strings** in JSON (both directions).

| Enum | Values | Used by |
|---|---|---|
| `Role` | `USER`, `MODERATOR`, `ADMIN` | user role filter / role update |
| `DifficultyLevel` | `EASY`, `MEDIUM`, `HARD`, `DIFFICULT` | lessons & exercises |
| `ContentStatus` | `VISIBLE`, `HIDDEN` | post/comment moderation |
| `BlogStatus` | `PENDING`, `APPROVED`, `REJECTED` | blog moderation |
| `ReportStatus` | `OPEN`, `RESOLVED`, `DISMISSED` | reports |
| `ReportAction` | `NONE`, `HIDE` | report resolution |
| `ReportTargetType` | `POST`, `COMMENT`, `BLOG` | reports |
| `ReportReason` | `SPAM`, `HARASSMENT`, `NSFW`, `MISINFORMATION`, `OTHER` | reports (read-only for you) |

All timestamps are **ISO-8601 with offset**, e.g. `2026-08-06T13:45:00.123456Z` or `+07:00`.
`DateTime.parse()` handles them. All IDs are UUID strings.

---

## 5. Dashboard Stats

Build the dashboard from these four calls (fire them in parallel).

### 5.1 `GET /stats/overview`

```json
{
  "signups24h": 12, "signups7d": 64,
  "posts24h": 30, "posts7d": 198,
  "comments24h": 87, "comments7d": 540,
  "lessonCompletions24h": 45, "lessonCompletions7d": 310,
  "pendingBlogs": 3,
  "openReports": 7
}
```

All fields are `long`. `pendingBlogs` / `openReports` are great "action needed" badges that
deep-link to the blog queue and reports queue.

### 5.2 `GET /stats/exercises?minAttempts=10`

| Param | Default | Notes |
|---|---|---|
| `minAttempts` | `10` | only exercises attempted at least this many times (min 1) |

Returns a JSON **array** (not paginated), sorted by `correctRate` ascending (hardest first):

```json
[
  {
    "exerciseId": "uuid",
    "question": "What is a variable?",
    "difficulty": "EASY",
    "attempts": 120,
    "correctCount": 45,
    "correctRate": 37.5,
    "failureRate": 62.5
  }
]
```

`question`/`difficulty` can be `null` if the exercise was deleted. Rates are percentages (0–100, 2 decimals).

### 5.3 `GET /stats/gamification`

```json
{
  "xpByDay": [
    { "date": "2026-08-05", "sourceType": "LESSON", "totalXp": 1450, "transactionCount": 96 }
  ],
  "streakHistogram": [
    { "bucket": "0", "userCount": 210 },
    { "bucket": "1-2", "userCount": 145 },
    { "bucket": "3-6", "userCount": 88 },
    { "bucket": "7-13", "userCount": 41 },
    { "bucket": "14-29", "userCount": 17 },
    { "bucket": "30+", "userCount": 5 }
  ]
}
```

- `xpByDay`: last 30 days, one row per (day, sourceType). `date` is `yyyy-MM-dd` (no time). Group by `sourceType` for a stacked chart.
- `streakHistogram`: always exactly 6 buckets in the order above (missing buckets are filled with 0).

### 5.4 `GET /stats/security`

```json
{ "lockedAccounts": 2, "suspendedAccounts": 5, "totalFailedLoginAttempts": 341 }
```

---

## 6. User Management

**ADMIN only.** Base path: `/users`

### 6.1 `GET /users` — list & search

| Param | Type | Default | Notes |
|---|---|---|---|
| `query` | string | — | case-insensitive partial match on username, email, displayName |
| `role` | Role | — | filter by role |
| `suspended` | bool | — | `true` = only currently suspended, `false` = only not suspended |
| `page` / `size` | int | `0` / `20` | offset pagination |

Returns `Page<AdminUserResponse>`:

```json
{
  "content": [
    {
      "id": "uuid",
      "username": "john_doe",
      "displayName": "John Doe",
      "email": "john@example.com",
      "emailVerified": true,
      "avatarUrl": "https://...",
      "role": "USER",
      "suspendedUntil": "2026-08-20T00:00:00Z",
      "suspensionReason": "spam",
      "lockedUntil": null,
      "createdAt": "2026-01-15T08:30:00Z"
    }
  ],
  "totalElements": 137, "totalPages": 7, "number": 0, "size": 20
}
```

`suspendedUntil`, `suspensionReason`, `lockedUntil`, `avatarUrl`, `email` may be `null`.
A user is *currently* suspended when `suspendedUntil != null && suspendedUntil > now`.

### 6.2 `POST /users/{id}/role` — promote

```json
{ "role": "MODERATOR" }
```

- Allowed values: `MODERATOR`, `ADMIN`. Sending `USER` → `400 "Cannot set role to USER via admin endpoint"` (demotion is not supported).
- Returns the updated `AdminUserResponse`.

### 6.3 `POST /users/{id}/suspension` — suspend

```json
{ "reason": "Repeated spam posting", "until": "2026-09-01T00:00:00Z" }
```

- `reason`: required, non-blank. `until`: required, must be in the future (else `400`).
- Staff accounts (`MODERATOR`/`ADMIN`) cannot be suspended → `400`.
- Re-suspending overwrites the previous suspension (previous reason is kept in the audit log).
- Returns the updated `AdminUserResponse`.

### 6.4 `DELETE /users/{id}/suspension` — unsuspend

No body. Returns the updated `AdminUserResponse`. `400 "User is not suspended"` if there is nothing to lift (expired suspensions count as not suspended).

---

## 7. Lessons

Base path: `/lessons`

`LessonResponse` shape (used by all endpoints below):

```json
{
  "id": "uuid",
  "title": "Introduction to Variables",
  "description": "Lesson body markdown...",
  "difficulty": "EASY",
  "baseXp": 10,
  "categoryId": "uuid-or-null",
  "reflectionPrompt": "What surprised you?",
  "exercises": [
    { "id": "uuid", "question": "What is a variable?", "difficulty": "EASY", "points": 5 }
  ],
  "createdAt": "2026-08-01T10:00:00Z",
  "updatedAt": "2026-08-01T10:00:00Z"
}
```

> Note: `answerKey` is intentionally **not** returned by the API. The edit screen cannot
> pre-fill answer keys — treat exercises as replace-all on edit (see 7.4).

### 7.1 `GET /lessons?page=0&size=20` — list

Returns `Page<LessonResponse>`, newest first.

### 7.2 `GET /lessons/{id}` — detail

Returns `LessonResponse`. `404` if not found. Use this to populate the edit form.

### 7.3 `POST /lessons` — create

Request body:

```json
{
  "title": "Introduction to Variables",
  "description": "Markdown body (optional)",
  "difficulty": "EASY",
  "baseXp": 10,
  "categoryId": "uuid (optional)",
  "reflectionPrompt": "What surprised you? (optional)",
  "exercises": [
    {
      "question": "What is a variable?",
      "answerKey": "A named storage location",
      "difficulty": "EASY",
      "points": 5
    }
  ]
}
```

| Field | Required | Validation |
|---|---|---|
| `title` | yes | non-blank |
| `difficulty` | yes | DifficultyLevel |
| `exercises` | yes | non-empty list |
| `exercise.question` / `answerKey` | yes | non-blank |
| `exercise.difficulty` | yes | DifficultyLevel |
| `exercise.points` | no | ≥ 1, default `5` |
| `baseXp` | no | ≥ 0, default `10` |
| `categoryId` | no | must exist, else `400 "Category not found: ..."` |

Success: **`201 Created`** with `Location` header and the `LessonResponse` body.

### 7.4 `PUT /lessons/{id}` — update

All fields optional — `null`/absent means "don't change":

```json
{ "title": "New title", "baseXp": 20 }
```

**Exercises are all-or-nothing:** if `exercises` is present (non-empty) it *replaces all
existing exercises*; if absent, exercises are untouched. There is no single-exercise edit —
send the full list every time.

Returns the updated `LessonResponse`. `400` if lesson/category not found.

### 7.5 `DELETE /lessons/{id}` — delete

Returns **`204 No Content`** (empty body).
⚠️ Hard delete: cascades to exercises **and erases users' completion records** for that lesson.
Show a strong confirmation dialog.

### 7.6 `POST /lessons/bulk` — bulk create (JSON)

Body: a JSON **array** of `LessonCreateRequest` (same shape as 7.3).
Always returns `200` with a summary — individual failures don't fail the request:

```json
{
  "successCount": 8,
  "failureCount": 2,
  "errors": ["Item 3: Category not found: ...", "Item 5: title: must not be blank"]
}
```

### 7.7 `POST /lessons/upload` — single markdown upload

`multipart/form-data` with one file field named **`file`** (`.md` or `.markdown` only).
Returns `201` + `LessonResponse`.

### 7.8 `POST /lessons/upload/bulk` — multiple markdown upload

`multipart/form-data` with field **`files`** (repeatable). Returns `BulkLessonResult` (see 7.6);
error entries are prefixed with the filename.

### Markdown lesson format

```markdown
---
title: Introduction to Variables
difficulty: EASY
baseXp: 10
categoryId: 550e8400-e29b-41d4-a716-446655440000
reflectionPrompt: What surprised you?
exercises:
  - question: What is a variable?
    answerKey: A named storage location
    difficulty: EASY
    points: 5
---

Everything below the frontmatter becomes the lesson description (markdown allowed).
```

- `title` and at least one exercise are required; `difficulty` defaults to `EASY`, `baseXp` to `10`, `points` to `5`.
- Parse errors come back as `400` with a precise message.

---

## 8. Categories

Base path: `/categories`

`CategoryResponse`:

```json
{
  "id": "uuid",
  "name": "Programming",
  "slug": "programming",
  "description": "All about coding",
  "iconUrl": "https://...",
  "createdAt": "2026-01-01T00:00:00Z",
  "updatedAt": "2026-01-01T00:00:00Z"
}
```

### 8.1 `GET /categories` — list all

Returns a plain JSON **array** sorted by name ascending (no pagination — the table is small).

### 8.2 `GET /categories/{id}` — detail

Returns `CategoryResponse`. `404` if not found.

### 8.3 `POST /categories` — create

```json
{ "name": "Programming", "slug": "programming", "description": "...", "iconUrl": "..." }
```

`name` and `slug` are required and unique → duplicates return `400 "Category name already exists: ..."` / `"Category slug already exists: ..."`.
Success: **`201`** + `Location` header + `CategoryResponse`.

### 8.4 `PUT /categories/{id}` — update

Same body as create — **all fields required** (full replacement, not partial). Uniqueness rules apply. Returns `CategoryResponse`.

### 8.5 `DELETE /categories/{id}` — delete

`204 No Content`. Lessons in the category are not deleted; their `categoryId` becomes `null`.

---

## 9. Report Moderation

Base path: `/reports`. Users report posts/comments/blogs; moderators resolve them here.

`ReportResponse`:

```json
{
  "id": "uuid",
  "reporterId": "uuid",
  "targetType": "POST",
  "targetId": "uuid",
  "reason": "SPAM",
  "details": "free-text the reporter wrote (nullable)",
  "status": "OPEN",
  "resolvedBy": null,
  "resolvedAt": null,
  "createdAt": "2026-08-05T09:12:00Z"
}
```

### 9.1 `GET /reports` — queue (cursor pagination)

| Param | Default | Notes |
|---|---|---|
| `status` | `OPEN` | `OPEN` / `RESOLVED` / `DISMISSED` |
| `targetType` | — | `POST` / `COMMENT` / `BLOG` |
| `cursor` | — | omit on first page |
| `limit` | `20` | clamped `1..50` |

Returns `CursorPage<ReportResponse>` (see §3.2), newest first.

> The reported content itself is not embedded. To show it, fetch the target from the regular
> module APIs using `targetType` + `targetId`, or display IDs only in v1.

### 9.2 `GET /reports/{id}` — detail

Returns `ReportResponse`. `404` if not found.

### 9.3 `PATCH /reports/{id}` — resolve

```json
{ "status": "RESOLVED", "action": "HIDE" }
```

| Field | Required | Notes |
|---|---|---|
| `status` | yes | `RESOLVED` or `DISMISSED` (`OPEN` → `400`) |
| `action` | no | `HIDE` or `NONE`, default `NONE` |

Behavior:
- `RESOLVED` + `HIDE`: automatically hides the reported post/comment (with an audit trail).
  If the target is already hidden or deleted, the report still resolves (outcome recorded in audit log).
- `HIDE` on a `BLOG` target → `400` (blogs have their own moderation flow, section 10).
- Already-resolved/dismissed reports → `400 "Report is already RESOLVED"`.

Returns the updated `ReportResponse`.

---

## 10. Blog Moderation

Base path: `/blog`

`BlogPostResponse`:

```json
{
  "id": "uuid",
  "authorId": "uuid",
  "title": "My learning journey",
  "content": "Full markdown content...",
  "coverUrl": "https://...",
  "status": "PENDING",
  "viewCount": 0,
  "likeCount": 0,
  "categoryId": "uuid-or-null",
  "liked": false,
  "createdAt": "2026-08-05T14:00:00Z",
  "updatedAt": "2026-08-05T14:00:00Z"
}
```

### 10.1 `GET /blog` — moderation queue

| Param | Default | Notes |
|---|---|---|
| `status` | `PENDING` | `PENDING` / `APPROVED` / `REJECTED` |
| `page` / `size` | `0` / `20` | size clamped `1..100` |

Returns the custom wrapper (newest first):

```json
{ "content": [ BlogPostResponse... ], "page": 0, "size": 20, "totalElements": 3, "totalPages": 1 }
```

### 10.2 `PATCH /blog/{id}/status` — approve or reject

```json
{ "status": "APPROVED" }
```
or
```json
{ "status": "REJECTED", "reason": "Off-topic promotional content" }
```

- `status`: required, `APPROVED` or `REJECTED` (`PENDING` → `400`).
- `reason`: strongly recommended for rejections (shown to the author).
- Only `PENDING` posts can be moderated → otherwise `409 Conflict` (`"Post already moderated: APPROVED"`).
- Returns the updated `BlogPostResponse`.

---

## 11. Social Moderation (Posts & Comments)

Base paths: `/posts` and `/comments`

### 11.1 `GET /posts` — post queue

| Param | Default | Notes |
|---|---|---|
| `status` | `HIDDEN` | `HIDDEN` (moderation queue) or `VISIBLE` |
| `page` / `size` | `0` / `20` | |

Returns `Page<PostModerationResponse>` (newest first):

```json
{
  "id": "uuid",
  "title": "Post title",
  "authorId": "uuid",
  "categoryId": "uuid",
  "status": "HIDDEN",
  "hiddenReason": "Reported as SPAM: buy now",
  "hiddenBy": "uuid",
  "hiddenAt": "2026-08-05T11:00:00Z",
  "createdAt": "2026-08-04T09:30:00Z"
}
```

### 11.2 `GET /comments` — comment queue

Same params. Returns `Page<CommentModerationResponse>`:

```json
{
  "id": "uuid",
  "content": "The comment text",
  "postId": "uuid",
  "authorId": "uuid",
  "status": "HIDDEN",
  "hiddenReason": "...",
  "hiddenBy": "uuid",
  "hiddenAt": "2026-08-05T11:00:00Z",
  "createdAt": "2026-08-04T09:30:00Z"
}
```

### 11.3 `PATCH /posts/{id}/status` — hide / restore post

```json
{ "status": "HIDDEN", "reason": "Violates community guidelines" }
```
or
```json
{ "status": "VISIBLE" }
```

- `reason` is **required when hiding** (`400` if blank); ignored when restoring.
- Setting the status it already has → `400 "Post is already HIDDEN"`.

Returns:

```json
{
  "id": "uuid",
  "status": "HIDDEN",
  "reason": "Violates community guidelines",
  "hiddenBy": "uuid",
  "hiddenAt": "2026-08-06T12:00:00Z"
}
```

(`reason`/`hiddenBy`/`hiddenAt` are `null` after restoring.)

### 11.4 `PATCH /comments/{id}/status` — hide / restore comment

Identical contract to 11.3 (error message says "Comment is already ...").

---

## 12. Audit Logs

**ADMIN only.** Base path: `/audit-logs`. Every admin action above is recorded here.

### `GET /audit-logs`

| Param | Notes |
|---|---|
| `targetType` | e.g. `USER`, `REPORT`, `POST`, `COMMENT` |
| `targetId` | UUID of the affected entity |
| `actorId` | UUID of the staff member who acted |
| `action` | see table below |
| `page` / `size` | defaults `0` / `20` |

All filters optional; combined with AND. Returns `Page<AuditLogResponse>` (newest first):

```json
{
  "id": "uuid",
  "actorId": "uuid",
  "action": "USER_SUSPEND",
  "targetType": "USER",
  "targetId": "uuid",
  "details": { "reason": "spam", "until": "2026-09-01T00:00:00Z" },
  "createdAt": "2026-08-06T12:00:00Z"
}
```

`details` is a free-form JSON object (varies per action; may be `null`). Render it as key/value rows.

Known `action` values:

| Action | Meaning |
|---|---|
| `USER_ROLE_CHANGE` | role changed (`details`: oldRole, newRole) |
| `USER_SUSPEND` / `USER_UNSUSPEND` | suspension toggled |
| `REPORT_RESOLVED` / `REPORT_DISMISSED` | report closed (`details`: targetType, targetId, action) |
| `POST_HIDE` / `POST_RESTORE` | post hidden/restored |
| `COMMENT_HIDE` / `COMMENT_RESTORE` | comment hidden/restored |

---

## 13. Flutter Implementation Notes

**HTTP client** — use `dio` with:
- an interceptor that adds `Authorization: Bearer <token>`;
- on `401`, call `POST /api/v1/auth/refresh`, store the new pair, retry the request once;
- on `403`, show "insufficient permissions".

**Models** — generate with `freezed`/`json_serializable`. Key type mappings:

| JSON | Dart |
|---|---|
| UUID string | `String` |
| timestamp | `DateTime` (`DateTime.parse`) |
| enum string | Dart `enum` with `@JsonValue("HIDDEN")` etc. (values are uppercase) |
| `Page<T>` | generic class with `content`, `number`, `size`, `totalElements`, `totalPages`, `first`, `last`, `empty` |
| `CursorPage<T>` | `edges`, `hasNextPage`, `endCursor` (nullable) |

**Gotchas:**
- Page numbers are **0-based**.
- `204` responses (deletes) have **no body** — don't try to parse them.
- `201` responses (creates) include a `Location` header; the body already contains the created object.
- Multipart uploads: `FormData.fromMap({'file': await MultipartFile.fromFile(path)})` — field names are exactly `file` / `files`.
- `endCursor` is **absent**, not `null`, on the last cursor page — model it as `String?`.
- The lesson edit form cannot pre-fill `answerKey`s (never returned by the API). Either require re-entering exercises on edit, or keep a local draft.
- Error `message` strings are user-safe — show them directly in snackbars.
- Reports default to `status=OPEN`; remember to pass `status=RESOLVED`/`DISMISSED` explicitly for history tabs.

---

## 14. Suggested Screen Map

| Screen | Endpoints |
|---|---|
| Dashboard | 5.1–5.4 (parallel) |
| Users (ADMIN) | 6.1 table with search/role/suspended filters → row actions: promote (6.2), suspend dialog (6.3), unsuspend (6.4) |
| Lessons | 7.1 table → detail/edit (7.2, 7.4), create form (7.3), delete confirm (7.5), bulk import dialog (7.6–7.8) |
| Categories | 8.1 table → create (8.3), edit (8.4), delete (8.5) |
| Reports queue | 9.1 with status/targetType tabs → resolve dialog (9.3), detail (9.2) |
| Blog queue | 10.1 (`PENDING` tab default, plus `APPROVED`/`REJECTED` history) → approve/reject (10.2) |
| Hidden content | 11.1 + 11.2 tabs → restore/hide (11.3, 11.4) |
| Audit logs (ADMIN) | 12 with filter bar |

---

*Generated from the implemented backend on 2026-08-06. Source of truth: `src/main/java/com/riksa/admin/`.*
