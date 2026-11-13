# Flutter Web Admin Panel — Implementation Plan

> Based on the **riksa-api** backend (Spring Boot, JWT auth, role-based access).  
> Generated: Aug 6, 2026

---

## System Overview

The admin panel will be a **Flutter Web** application consuming the riksa-api REST API at `/api/v1/`. Authentication is JWT Bearer token-based. Role requirements:

| Role | Access Level |
|------|-------------|
| `ADMIN` | Full access — Users, Categories, Lessons, Blog Moderation |
| `MODERATOR` | Categories, Lessons, Blog Moderation (no User management) |

---

## Architecture Decisions (Recommended)

| Area | Choice |
|------|--------|
| State management | Riverpod (or Provider) |
| HTTP client | `dio` with interceptors for JWT refresh |
| Routing | `go_router` with redirect guards for auth + roles |
| UI framework | Material 3 |
| Serialization | `freezed` + `json_serializable` |
| Responsive layout | `LayoutBuilder` / `Breakpoint` for desktop-first |

---

## Phase 0 — Project Scaffold & Foundation (Day 1–2)

### 0.1 Flutter Project Setup
```bash
flutter create --org com.riksa --platforms web riksa_admin
```

### 0.2 Dependencies
```yaml
dependencies:
  dio: ^5.x
  flutter_riverpod: ^2.x
  go_router: ^14.x
  freezed_annotation: ^2.x
  json_annotation: ^4.x
  fl_chart: ^0.x          # for dashboard charts
  intl: ^0.x              # for date formatting
  flutter_secure_storage: ^9.x  # for JWT storage on web (uses IndexedDB)

dev_dependencies:
  build_runner: ^2.x
  freezed: ^2.x
  json_serializable: ^6.x
```

### 0.3 Folder Structure
```
lib/
├── main.dart
├── app.dart                          # MaterialApp + GoRouter
├── core/
│   ├── api/
│   │   ├── dio_client.dart           # Dio instance with interceptors
│   │   ├── api_constants.dart        # Base URL, endpoints
│   │   └── api_exception.dart        # Typed exceptions
│   ├── auth/
│   │   ├── auth_service.dart         # login, refresh, logout
│   │   ├── token_storage.dart        # JWT persist (secure_storage)
│   │   ├── auth_interceptor.dart     # Dio interceptor: attach Bearer, handle 401 refresh
│   │   └── auth_provider.dart        # Riverpod: auth state (user, role, logged-in)
│   ├── router/
│   │   ├── app_router.dart           # Route definitions
│   │   └── auth_guard.dart           # Redirect guard: no token → /login
│   └── theme/
│       └── app_theme.dart            # Material 3 theme (dark + light)
├── features/
│   ├── auth/
│   │   ├── screens/
│   │   │   └── login_screen.dart
│   │   └── widgets/
│   │       └── login_form.dart
│   ├── dashboard/
│   │   └── screens/
│   │       └── dashboard_screen.dart
│   ├── users/
│   │   ├── data/
│   │   │   └── user_repository.dart
│   │   └── screens/
│   │       ├── user_list_screen.dart
│   │       └── user_detail_screen.dart
│   ├── categories/
│   │   ├── data/
│   │   │   └── category_repository.dart
│   │   └── screens/
│   │       ├── category_list_screen.dart
│   │       └── category_form_screen.dart
│   ├── lessons/
│   │   ├── data/
│   │   │   └── lesson_repository.dart
│   │   └── screens/
│   │       ├── lesson_list_screen.dart
│   │       ├── lesson_form_screen.dart
│   │       └── lesson_bulk_screen.dart
│   └── blog/
│       ├── data/
│       │   └── blog_repository.dart
│       └── screens/
│           ├── blog_queue_screen.dart
│           └── blog_detail_screen.dart
└── shared/
    ├── models/                       # All DTO classes (freezed)
    │   ├── auth_models.dart
    │   ├── user_models.dart
    │   ├── category_models.dart
    │   ├── lesson_models.dart
    │   └── blog_models.dart
    └── widgets/
        ├── admin_shell.dart          # Scaffold + NavigationRail + AppBar
        ├── data_table_widget.dart    # Reusable paginated data table
        ├── loading_overlay.dart
        ├── confirm_dialog.dart
        └── error_banner.dart
```

### 0.4 Data Models (freezed)

Generate Dart classes for every request/response DTO listed below. Use `@JsonSerializable` and `freezed`:

**Auth DTOs:**
- `LoginRequest` (username, password)
- `AuthResponse` (userId, accessToken, refreshToken, tokenType, username, role)
- `RefreshTokenRequest` (refreshToken)
- `UserProfileResponse` (id, username, displayName, email, emailVerified, avatarUrl, bio, gender, phoneNumber, phoneVerified, role, countryId, createdAt)
- `MessageResponse` (message)

**Admin DTOs:**
- `RoleUpdateRequest` (role: "MODERATOR" | "ADMIN")
- `CategoryRequest` (name: required, slug: required, description?, iconUrl?)
- `CategoryResponse` (id, name, slug, description, iconUrl, createdAt, updatedAt)
- `BlogPostResponse` (id, authorId, title, content, coverUrl, status, viewCount, likeCount, categoryId, liked, createdAt, updatedAt)
- `BlogStatus` enum (PENDING, APPROVED, REJECTED)
- `ModerationRequest` (status: "APPROVED" | "REJECTED", reason?)
- `LessonCreateRequest` (title, description?, difficulty: "EASY"|"MEDIUM"|"HARD", baseXp: 10, categoryId?, exercises: List<ExerciseRequest>)
- `LessonUpdateRequest` (same fields, all optional; exercises if present → full replacement)
- `LessonResponse` (id, title, description, difficulty, baseXp, categoryId, exercises, createdAt, updatedAt)
- `ExerciseRequest` (question, answerKey, difficulty, points: 5)
- `ExerciseResponse` (id, question, difficulty, points)
- `BulkLessonResult` (successCount, failureCount, errors: List<String>)

---

## Phase 1 — Authentication & Shell (Day 3–5)

### 1.1 Login Screen
- **Route:** `/login` (public, no shell)
- Form: username + password → `POST /api/v1/auth/login`
- On success: store `AuthResponse` (accessToken, refreshToken, userId, username, role) → redirect to `/`
- On failure: show error snackbar
- Redirect already-logged-in users away from `/login`

### 1.2 Token Management (`AuthService` + `AuthInterceptor`)
- Dio interceptor attaches `Authorization: Bearer <accessToken>` to every request
- On **401** response: attempt silent refresh via `POST /api/v1/auth/refresh` with stored refresh token
- If refresh succeeds: retry original request with new token
- If refresh fails: clear tokens, redirect to `/login`
- On logout: `POST /api/v1/auth/logout` then clear storage

### 1.3 Admin Shell Layout (`AdminShell`)
- **NavigationRail** (left sidebar, collapses to icons on narrow screens):
  - Dashboard (`/`) — icon: dashboard
  - Users (`/users`) — icon: people — **visible only if role == ADMIN**
  - Categories (`/categories`) — icon: category
  - Lessons (`/lessons`) — icon: school
  - Blog Queue (`/blog`) — icon: article
- **AppBar**: app title, current user displayName, logout button
- **Body**: `child` from GoRouter (nested navigation)
- Responsive: NavigationRail on desktop, bottom NavigationBar on mobile

### 1.4 Route Guards
- `AuthGuard`: no token → redirect to `/login`
- `AdminGuard`: route `/users` requires role `ADMIN`, else redirect to `/` + show "Access denied" snackbar

### 1.5 Role-Aware UI
- `authProvider` exposes `isAdmin`, `isModerator` getters
- Sidebar items filtered by role
- Action buttons (e.g., "Change Role", "Delete") gated by role checks

---

## Phase 2 — Dashboard (Day 6–7)

### 2.1 Dashboard Screen (`/`)
- Welcome card: "Hello, {displayName}" with role badge
- Quick stats cards (placeholder — backend may add summary endpoints later):
  - Total registered users (from GET /users page count)
  - Lessons pending review
  - Blog posts in PENDING moderation queue
- Quick actions:
  - "Create Lesson" → `/lessons/new`
  - "Moderate Blog" → `/blog`
  - "Manage Users" → `/users` (admin only)
- Recent activity feed (placeholder for future endpoint)

---

## Phase 3 — User Management (Day 8–10) [ADMIN only]

### 3.1 User List Screen (`/users`)
- **Endpoint:** `GET /api/v1/admin/users?page={page}&size=20`
- Paginated `DataTable`:
  - Columns: Avatar, Username, Display Name, Email, Role, Email Verified, Created At
  - Role shown as colored chip (ADMIN=red, MODERATOR=orange, USER=grey)
  - Click row → user detail dialog
- Search/filter: client-side (the API uses simple offset pagination)
- Page controls: Previous / Next buttons with current page indicator (max size 100 per spec)

### 3.2 Change Role Dialog
- **Endpoint:** `POST /api/v1/admin/users/{userId}/role`
- Triggered from user list row or detail dialog
- Dropdown: "MODERATOR" / "ADMIN" (USER role cannot be set back — enforced by backend)
- Confirmation dialog: "Promote {username} to {role}?"
- On success: refresh user list, show success snackbar

---

## Phase 4 — Category Management (Day 11–13)

### 4.1 Category List Screen (`/categories`)
- **No dedicated GET endpoint for listing categories?**  
  _Check: The categories are also used in blog & lessons. Use `GET /api/v1/blog/categories` or a general purpose list endpoint if available. Otherwise note this as a backend gap._
- Grid/List view of categories with icon, name, slug, description
- FAB: "Add Category"

### 4.2 Category Form (`/categories/new`, `/categories/{id}/edit`)
- **Create:** `POST /api/v1/admin/categories` — Body: `CategoryRequest`
- **Edit:** `PUT /api/v1/admin/categories/{id}` — Body: `CategoryRequest`
- Form fields:
  - Name (required, `@NotBlank`)
  - Slug (required, `@NotBlank` — auto-generate from name on blur)
  - Description (optional, multiline)
  - Icon URL (optional, with preview)

### 4.3 Delete Category
- **Endpoint:** `DELETE /api/v1/admin/categories/{id}`
- Confirmation dialog: "Delete category {name}? Related lessons will keep their category set to NULL."
- On success: remove from list, show snackbar

---

## Phase 5 — Lesson Management (Day 14–18)

### 5.1 Lesson List Screen (`/lessons`)
- **Endpoint:** `GET /api/v1/admin/lessons?page={page}&size=20`
- Paginated `DataTable`:
  - Columns: Title, Difficulty (color badge), XP Points, Exercises Count, Category, Updated At
  - Click row → edit lesson
- FAB: "Add Lesson"
- Secondary action: "Bulk Import"

### 5.2 Lesson Create/Edit Form (`/lessons/new`, `/lessons/{id}/edit`)
- **Create:** `POST /api/v1/admin/lessons` — Body: `LessonCreateRequest`
- **Edit:** `PUT /api/v1/admin/lessons/{id}` — Body: `LessonUpdateRequest` (only changed fields sent)
- Tabs/sections:

  **Lesson Details:**
  - Title (required)
  - Description (optional, rich text / multiline)
  - Difficulty dropdown (EASY / MEDIUM / HARD) — required
  - Base XP (number, min 0, default 10)
  - Category dropdown (load from categories list)

  **Exercises (dynamic list):**
  - Each exercise card shows:
    - Question (required, multiline)
    - Answer Key (required, single line)
    - Difficulty dropdown (EASY / MEDIUM / HARD) — required
    - Points (number, min 1, default 5)
  - "Add Exercise" / "Remove" buttons
  - **Important for Edit:** The backend treats exercises in `LessonUpdateRequest` as full replacement — if null, exercises are untouched. If present (even empty list), all existing exercises are deleted and replaced. Handle carefully:
    - Track `exercisesModified` flag
    - If user never touches the exercises tab → send null (don't touch)
    - If user modifies exercises → send the new full list (replaces all)

### 5.3 Delete Lesson
- **Endpoint:** `DELETE /api/v1/admin/lessons/{id}`
- Confirmation dialog: "Delete lesson '{title}'? This will also delete all {n} exercises."
- On success: remove from list, show snackbar

### 5.4 Bulk Import Lessons (`/lessons/bulk`)
- **Endpoint:** `POST /api/v1/admin/lessons/bulk` — Body: `List<LessonCreateRequest>`
- UI Options:
  - **JSON editor:** Paste a JSON array of `LessonCreateRequest` objects (for tech-savvy moderators)
  - **CSV upload:** Parse CSV with columns mapping to fields, convert to request objects
  - **Template download:** Provide an empty template CSV/JSON
- Submit → show `BulkLessonResult`:
  - Success count (green)
  - Failure count (red)
  - Error list (expandable, each error message on a line)
- After bulk import: navigate back to lesson list (refresh)

---

## Phase 6 — Blog Moderation (Day 19–22)

### 6.1 Blog Moderation Queue (`/blog`)
- **Endpoint:** `GET /api/v1/blog?status=PENDING&page={page}&size=20`
  - _Note:_ The `/api/v1/blog` endpoint allows moderators to filter by `BlogStatus`. Non-moderators are forced to `APPROVED`. The admin panel should default to `PENDING` filter.
- Tabs/filter chips: **Pending** (default) | **Approved** | **Rejected**
- Paginated list of blog posts:
  - Cards: Title, Author ID, Category, Cover Image (thumbnail), Content preview (first 150 chars), Created At
  - Status badge: PENDING=yellow, APPROVED=green, REJECTED=red
  - Click card → blog detail view

### 6.2 Blog Detail & Moderation (`/blog/{id}`)
- **Endpoint:** `GET /api/v1/blog/{id}` — moderators can view posts in any status
- Full content display, cover image, author info, date, view count, like count
- **Moderation action buttons** (only shown for PENDING posts):
  - **Approve:** `PATCH /api/v1/admin/blog/{id}/status` — Body: `{"status": "APPROVED"}`
  - **Reject:** `PATCH /api/v1/admin/blog/{id}/status` — Body: `{"status": "REJECTED", "reason": "..."}`
- Reject dialog: mandatory reason text field
- After moderation: navigate back to queue, show "Post {status}" snackbar

---

## Phase 7 — Error Handling & UX Polish (Day 23–25)

### 7.1 Global Error Handling
- Dio interceptor maps HTTP status codes to user-friendly messages:
  - 400 → validation error (show field-level errors if API returns them)
  - 401 → session expired, redirect to login
  - 403 → "You don't have permission" (role issue)
  - 404 → "Resource not found"
  - 409 → "Conflict — resource already exists" (e.g., duplicate category slug)
  - 500 → "Server error, please try again"
- `ErrorBanner` widget: slide-down banner for non-form errors
- Form errors: shown inline under each field

### 7.2 Loading States
- Skeleton loaders for lists/tables while data loads
- `LoadingOverlay` for form submissions (disable buttons, show spinner)
- Pull-to-refresh on list screens

### 7.3 Empty States
- Empty list illustrations with helpful messages:
  - "No users found" — Users tab
  - "No lessons yet. Create your first lesson!" — Lessons tab
  - "No blog posts in moderation queue" — Blog tab
  - "No categories yet" — Categories tab

### 7.4 Responsive Design
- Desktop (≥ 1200px): NavigationRail + Sidebar + Table view
- Tablet (600–1199px): NavigationRail (icons only) + compact table
- Mobile (< 600px): Bottom NavigationBar + stacked card layout instead of tables

### 7.5 Dark Mode
- Toggle in AppBar / settings
- Persist preference in `SharedPreferences`

---

## Phase 8 — Testing & Deployment (Day 26–28)

### 8.1 Unit Tests
- Model serialization (JSON → Dart → JSON roundtrip)
- AuthService token refresh logic (mock Dio)
- Repository methods (mock Dio)

### 8.2 Widget Tests
- Login form validation
- AdminShell layout with different roles
- DataTable pagination

### 8.3 Integration Tests
- Login → redirect to dashboard
- Create → edit → delete category flow
- Create lesson with exercises → verify in list

### 8.4 Deployment
- `flutter build web` → output in `build/web/`
- Deploy to Firebase Hosting / Vercel / Nginx
- Configure base URL via environment variable or `.env` file
- CORS: ensure the backend's `cors.allowed-origins` includes the Flutter web origin

---

## API Endpoint Reference

### Auth
| Method | Path | Request Body | Response |
|--------|------|-------------|----------|
| POST | `/api/v1/auth/login` | `LoginRequest` | `AuthResponse` |
| POST | `/api/v1/auth/refresh` | `RefreshTokenRequest` | `AuthResponse` |
| POST | `/api/v1/auth/logout` | `RefreshTokenRequest` | `MessageResponse` |
| GET | `/api/v1/auth/me` | — | `UserProfileResponse` |

### Admin — Users (ADMIN only)
| Method | Path | Query | Body | Response |
|--------|------|-------|------|----------|
| GET | `/api/v1/admin/users` | `page`, `size` (max 100) | — | `List<UserProfileResponse>` |
| POST | `/api/v1/admin/users/{id}/role` | — | `RoleUpdateRequest` | `UserProfileResponse` |

### Admin — Categories (MODERATOR/ADMIN)
| Method | Path | Body | Response |
|--------|------|------|----------|
| POST | `/api/v1/admin/categories` | `CategoryRequest` | `CategoryResponse` (201) |
| PUT | `/api/v1/admin/categories/{id}` | `CategoryRequest` | `CategoryResponse` (200) |
| DELETE | `/api/v1/admin/categories/{id}` | — | 204 |

### Admin — Lessons (MODERATOR/ADMIN)
| Method | Path | Query | Body | Response |
|--------|------|-------|------|----------|
| GET | `/api/v1/admin/lessons` | `page`, `size` (max 100) | — | `List<LessonResponse>` |
| POST | `/api/v1/admin/lessons` | — | `LessonCreateRequest` | `LessonResponse` (201) |
| PUT | `/api/v1/admin/lessons/{id}` | — | `LessonUpdateRequest` | `LessonResponse` (200) |
| DELETE | `/api/v1/admin/lessons/{id}` | — | — | 204 |
| POST | `/api/v1/admin/lessons/bulk` | — | `List<LessonCreateRequest>` | `BulkLessonResult` |

### Admin — Blog Moderation (MODERATOR/ADMIN)
| Method | Path | Query | Body | Response |
|--------|------|-------|------|----------|
| GET | `/api/v1/blog` | `status`, `page`, `size` | — | `List<BlogPostResponse>` |
| GET | `/api/v1/blog/{id}` | — | — | `BlogPostResponse` |
| PATCH | `/api/v1/admin/blog/{id}/status` | — | `ModerationRequest` | `BlogPostResponse` (200) |

---

## Priorities & Time Estimates

| Phase | Feature | Est. Days | Priority |
|-------|---------|-----------|----------|
| 0 | Scaffold + Models + API Client | 2 | P0 |
| 1 | Auth + Shell + Routing | 3 | P0 |
| 2 | Dashboard | 2 | P2 |
| 3 | User Management | 3 | P1 |
| 4 | Category Management | 3 | P1 |
| 5 | Lesson Management | 5 | P1 |
| 6 | Blog Moderation | 4 | P1 |
| 7 | Error Handling & Polish | 3 | P2 |
| 8 | Testing & Deployment | 3 | P3 |

**Total: ~28 days** (single developer, full-time)

### Critical Path (MVP): Phase 0 → 1 → 5 → 4 → 6 → 7

---

## Backend Gaps / Notes

1. **Category list endpoint missing:** There is no dedicated `GET /api/v1/admin/categories` endpoint. The frontend needs a way to list all categories. Either add this endpoint or reuse an existing public endpoint like `/api/v1/blog/categories`.

2. **Dashboard stats endpoint missing:** The admin dashboard would benefit from a `GET /api/v1/admin/stats` endpoint returning counts (total users, pending blogs, total lessons, etc.). Currently these must be derived from paginated list endpoints.

3. **Blog author display name:** `BlogPostResponse` only has `authorId` (UUID). A follow-up call to get the author's `displayName` is needed for the moderation UI. Consider adding `authorName` to `BlogPostResponse` on the backend side.

4. **Bulk lesson operations are non-transactional:** Each lesson in bulk create is independent; failures in some do not roll back others. The frontend should clearly display per-item errors from `BulkLessonResult.errors`.

5. **`LessonUpdateRequest.exercises` has special semantics:** If null → untouched. If provided (even empty list) → full replacement of all exercises. The frontend must carefully track whether exercises were edited.
