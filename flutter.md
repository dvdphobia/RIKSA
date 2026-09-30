# Riksa Flutter App — Development Guide

> Mobile client for the **Riksa Learning Platform** (Cambodian educational app).
> Backend: Spring Boot 4 — REST under `/api/v1/*` + GraphQL at `/graphql`.
> Base URL (local): `http://localhost:8080` · Android emulator: `http://10.0.2.2:8080` · iOS sim: `http://localhost:8080`.

This file is the single working guide for building the Flutter app against this backend.
A more detailed team plan (4 devs, parallel lanes) lives in `flutterPlan/` — read `flutterPlan/README.md` first if you're on a team.

---

## 1. Backend reality check

Build against what's actually implemented:

| Module | Surface | Status | Build approach |
|--------|---------|--------|----------------|
| Auth | REST `/api/v1/auth/**` | ✅ Done | Build fully now |
| Blog | REST `/api/v1/blog/**` | ✅ Done | Build fully now |
| Gamification | REST `/api/v1/gamification/**` | ✅ Done | Build fully now |
| Leaderboard | REST `/api/v1/leaderboard/**` | ✅ Done | Build fully now |
| Social | GraphQL `/graphql` | ⚠️ Partial | UI now; some fields broken (see §6) |
| Learning | REST | ❌ Stub (only `Lesson` entity) | Build against mocks; wire when backend lands |
| AI Chat | — | ❌ Empty | Out of scope for v1 |
| Admin | — | ❌ Empty (separate web panel) | Out of scope |

All endpoints except `/api/v1/auth/**` and `/actuator/health` require `Authorization: Bearer <accessToken>`.

---

## 2. Stack & conventions

| Concern | Choice |
|---------|--------|
| State | Riverpod (`flutter_riverpod` + `riverpod_generator`) |
| Navigation | go_router (declarative, deep-link ready) |
| REST | dio + interceptors (auth, refresh, logging) |
| GraphQL | `graphql_flutter` (social module only) |
| Models | `freezed` + `json_serializable` |
| Secure storage | `flutter_secure_storage` (access/refresh tokens) |
| Settings | `shared_preferences` |
| i18n | `flutter_localizations` + `intl` `.arb` (English + Khmer from day one) |
| Theming | Material 3, central `AppTheme` |
| Testing | `flutter_test`, `mocktail`, `golden_toolkit` |
| Lint | `very_good_analysis` (or `flutter_lints`) |

### Folder structure (feature-first)

```
lib/
  main.dart
  app/                 # app shell, router, theme, DI
    app.dart
    router/app_router.dart
    theme/
  core/                # shared infra, no feature logic
    network/           # dio client, graphql client, interceptors
    storage/           # secure storage, prefs
    error/             # failures, exception mapping
    widgets/           # shared buttons, loaders, empty/error states
    localization/
  features/
    auth/
    social/
    gamification/
    leaderboard/
    blog/
    learning/
    profile/
      data/            # datasources (api), dtos, repository impls
      domain/          # entities, repository interfaces, usecases
      presentation/    # screens, widgets, riverpod providers
```

---

## 3. First-day setup

1. Start the backend:
   ```bash
   docker compose up -d postgres
   set -a && source .env && set +a
   ./mvnw spring-boot:run
   curl http://localhost:8080/actuator/health   # {"status":"UP"}
   ```
2. Scaffold the Flutter app:
   ```bash
   flutter create riksa_app --org com.riksa
   cd riksa_app
   flutter pub add flutter_riverpod riverpod_generator go_router dio graphql_flutter \
       freezed json_serializable flutter_secure_storage shared_preferences \
       intl flutter_localizations
   flutter pub add dev:build_runner dev:freezed dev:json_serializable dev:mocktail dev:very_good_analysis
   ```
3. Codegen helper — add to `pubspec.yaml` scripts or run:
   ```bash
   dart run build_runner build --delete-conflicting-outputs
   ```
4. Add `.gitignore` entries for `*.g.dart` / `*.freezed.dart` if you don't commit generated files (recommended — avoids merge conflicts).

---

## 4. Core seams (build these first)

Everything else depends on these. They live in `core/` and `app/`.

### 4.1 Auth state provider

`authControllerProvider` exposes `Stream<AuthState>`:
- `unauthenticated`
- `authenticated(user)`
- `loading`

Every feature reads this; only `features/auth` writes it.

### 4.2 Authenticated dio client

`dioProvider` is pre-wired with:
- `AuthInterceptor` — attaches `Authorization: Bearer <accessToken>` to every protected call.
- `RefreshInterceptor` — on 401, calls `POST /auth/refresh` **once** (single-flight lock), retries the original request; on failure, emits `unauthenticated`.
- `LoggingInterceptor` — dev only.

Features never touch tokens directly. They just `ref.read(dioProvider)`.

### 4.3 GraphQL client

`graphqlClientProvider` — only `features/social` uses this. Same bearer token injection via dio's stored token.

### 4.4 Router registration

Each feature exports `List<RouteBase>` (e.g. `authRoutes`, `blogRoutes`). `app_router.dart` composes them. Features never edit the root router.

### 4.5 Repository pattern

Each feature defines a repo interface in `domain/`, backs it with:
1. `Fake<Repo>` — canned data (lets UI work start before backend is wired).
2. `Api<Repo>` — real dio/graphql calls.

Swap with a Riverpod provider override. Learning uses `Fake` until backend lands.

---

## 5. REST contracts

Conventions:
- **Auth header:** `Authorization: Bearer <accessToken>`.
- **Tokens:** access = 15 min, refresh = 30 days. `POST /auth/refresh` returns a **new pair** — persist both.
- **Cursor pagination:** `CursorPage<T> { edges: [T], hasNextPage: bool, endCursor: string }`. `endCursor` is an **opaque base64url string** — pass back as `?cursor=`; never parse it.
- **IDs:** UUID strings. **Timestamps:** ISO-8601 with offset.

### AUTH — `/api/v1/auth` (public)

| Method | Path | Body | Response |
|--------|------|------|----------|
| POST | `/register` | `{username, displayName, email, password, phoneNumber?, gender?, provinceId?}` | `AuthResponse` |
| POST | `/login` | `{username, password}` | `AuthResponse` |
| POST | `/refresh` | `{refreshToken}` | `AuthResponse` |
| POST | `/logout` | `{refreshToken}` | `{message}` |
| POST | `/google` | `{idToken}` | `AuthResponse` |
| POST | `/otp/send` | `{identifier, channel: "SMS"\|"TELEGRAM"}` | `{message, channel, code?}` |
| POST | `/otp/verify` | `{identifier, channel, code}` | `{verified, message}` |
| POST | `/forgot-password` | `{identifier}` | `{message}` |
| POST | `/reset-password` | `{identifier, code, newPassword}` | `{message}` |
| POST | `/verify-email` | `{token}` | `{verified, ...}` |
| GET | `/me` | — | `UserProfileResponse` |
| PUT | `/me` | `UpdateProfileRequest` | `UserProfileResponse` |

- `AuthResponse`: `{userId, accessToken, refreshToken, tokenType:"Bearer", username, role}`
- `UserProfileResponse`: `{id, username, displayName, avatarUrl, bio, gender, role, phoneNumber, phoneVerified, email, countryId, provinceId, createdAt, updatedAt}`
- Telegram OTP: `identifier` = user's Telegram chat ID (must `/start` the bot first). SMS provider not connected — `code` returned in response for dev.

### GAMIFICATION — `/api/v1/gamification` (Bearer)

| Method | Path | Query | Response |
|--------|------|-------|----------|
| GET | `/me` | — | `GamificationProfileResponse` |
| GET | `/xp` | `?cursor=&limit=20` | `CursorPage<XpTransactionResponse>` |
| GET | `/achievements` | — | `List<AchievementResponse>` |
| GET | `/rank-history` | `?cursor=&limit=` | `CursorPage<RankHistoryResponse>` |
| GET | `/streak` | — | `StreakResponse` |
| GET | `/leaderboard` | `?cursor=&limit=20` | `CursorPage<LeaderboardEntryResponse>` |

- `GamificationProfileResponse`: `{userId, totalXp, currentStreak, highestStreak, currentTier, currentTierIcon, nextTier, xpToNext}`
- `XpTransactionResponse`: `{id, amount, sourceType, sourceId, runningBalance, createdAt}`
- `AchievementResponse`: `{id, name, description, iconUrl, earned, unlockedAt}`
- `StreakResponse`: `{currentStreak, highestStreak, currentMilestoneName, currentMilestoneIcon, nextMilestoneName, nextMilestoneMinStreak, daysToNext}`
- `LeaderboardEntryResponse`: `{rank, userId, displayName, avatarUrl, totalXp, tierName, tierIcon}`

### LEADERBOARD — `/api/v1/leaderboard` (Bearer)

| Method | Path | Query | Response |
|--------|------|-------|----------|
| GET | `/` | `?scope=COUNTRY&...` | `List<LeaderboardRowResponse>` |
| GET | `/me` | `?scopeId=` | `MyRankResponse` |

> Backed by a snapshot batch job. Confirm with backend whether this or gamification `/leaderboard` is canonical for the main UI.

### BLOG — `/api/v1/blog` (Bearer)

| Method | Path | Notes | Response |
|--------|------|-------|----------|
| GET | `/` | `?status=APPROVED&...` | `BlogListResponse` |
| POST | `/` | `{title, content, coverUrl}` | `BlogPostResponse` |
| GET | `/{id}` | — | `BlogPostResponse` |
| POST | `/{id}/like` | toggle | `BlogPostResponse` |
| POST | `/{id}/view` | track view (auth optional) | `204` |
| PATCH | `/admin/{id}/status` | **MODERATOR role only** | `BlogPostResponse` |

Gate the moderation call on `role == MODERATOR` from `AuthResponse`/profile.

---

## 6. GraphQL — Social module

Single endpoint `/graphql`. Full schema: `src/main/resources/graphql/social.graphqls` — keep a copy in the Flutter repo.

### Queries
```graphql
feed(categoryId: UUID, cursor: UUID, limit: Int = 20): FeedPage!
post(id: UUID!): Post
categories: [Category!]!
comments(postId: UUID!, cursor: UUID, limit: Int = 20): CommentPage!   # ❌ no resolver yet
```

### Mutations
```graphql
createPost(input: CreatePostInput!): Post!         # ✅
updatePost(input: UpdatePostInput!): Post!         # ✅
deletePost(id: UUID!): Boolean!                    # ✅
createComment(input: CreateCommentInput!): Comment!# ✅
updateComment(input: UpdateCommentInput!): Comment!# ✅
deleteComment(id: UUID!): Boolean!                # ✅
react(input: ReactInput!): Post!                   # ✅
removeReaction(postId: UUID, commentId: UUID): Post! # ✅
sharePost(input: SharePostInput!): Post!          # ❌ no resolver yet
joinCategory(categoryId: UUID!): Category!        # ✅
leaveCategory(categoryId: UUID!): Category!        # ✅
```

### Types (abridged)
```graphql
type Post {
  id: UUID!  title: String!  content: String!  mediaUrl: String
  reactionCount: Int!  commentCount: Int!  shareCount: Int!
  author: User!            # ❌ null → selecting this ERRORS
  category: Category!      # ✅
  myReaction: ReactionType # ⚠️ always null until fixed
  createdAt: DateTime!  updatedAt: DateTime!
  comments(cursor: UUID, limit: Int = 10): CommentPage!  # ❌ no field resolver
}
type Category { id: UUID!  name: String!  slug: String!  description: String  iconUrl: String  memberCount: Int!  joined: Boolean! }
enum ReactionType { UPVOTE DOWNVOTE HAHA LOVE }
enum SharePlatform { TELEGRAM FACEBOOK COPY_LINK }
```

### ⚠️ Social integration rules
- **Do not select `author` or nested `comments`/`replies` in live queries** — non-null `author: User!` returning null errors the whole query. Use `categoryId`/`post.authorId` workarounds or resolve author via REST `/me`-style lookup until backend fixes resolvers.
- `comments` query and `sharePost` mutation have **no server resolver** — mock client-side.
- `myReaction` always null — treat as "no reaction" for now.
- File one backend ticket covering all of the above.

---

## 7. Learning — not implemented (build against fakes)

Only a `Lesson` entity (baseXp, difficulty) + `LessonCompletedEvent` (consumed server-side by gamification). No endpoints.

**Proposed contract to negotiate with backend (build Flutter against this as a fake):**
```
GET  /api/v1/learning/courses              -> List<CourseResponse>
GET  /api/v1/learning/lessons/{id}         -> LessonResponse
POST /api/v1/learning/lessons/{id}/complete -> {xpAwarded, ...}   # emits LessonCompletedEvent
```
Ship learning behind a feature flag until these land.

---

## 8. Suggested build order (solo or team)

```
Week 0  FOUNDATION: scaffold, CI, theme, dio+refresh interceptor, secure storage,
         authControllerProvider, fake repos, router registration pattern.
         Nobody builds features until the seams exist.

Week 1+  PARALLEL LANES:
  - Auth screens → app shell + bottom nav → deep links
  - Social feed (mock) → integrate as resolvers land
  - Gamification dashboard → streak → achievements → leaderboard
  - Blog list/detail → blog create/like → learning (mock) → profile/settings
```

Dependency order matters more than exact weeks.

---

## 9. Definition of Done (every feature)

- [ ] Screens call the repository interface, not dio/graphql directly
- [ ] Loading / empty / error states via shared `core/widgets`
- [ ] Khmer + English strings (no hardcoded user-facing text)
- [ ] Riverpod providers unit-tested; key widgets have widget tests
- [ ] Works on Android + iOS; respects safe areas and dark mode
- [ ] No analyzer warnings; PR reviewed
- [ ] Semantic labels on interactive elements, adequate contrast, scalable text

---

## 10. Git workflow

- Branches: `main` (releases) ← `develop` (integration) ← `feat/<area>/<short-desc>`
- PRs target `develop`; small and focused; at least 1 review.
- Shared files (`app_router.dart`, `app_theme.dart`, `dio_client.dart`) have a single owner — others request changes via PR, never edit directly.
- CI must pass: `flutter analyze` + `flutter test` + `flutter build` before merge.
- Commit generated files? Recommended **no** (gitignore `*.g.dart`, `*.freezed.dart`); run `build_runner` in CI + document the setup step.

---

## 11. Risks & mitigations

| Risk | Mitigation |
|------|------------|
| Social GraphQL resolvers incomplete | Mock repo + local schema; integrate behind a feature flag as backend fixes land |
| Learning module is a backend stub | Fake repo; feature flag |
| Token refresh race conditions | Single-flight refresh lock in `RefreshInterceptor` |
| Two API styles (REST + GraphQL) | Only `features/social` touches GraphQL; everything else is dio/REST |

---

## 12. Out of scope for v1

- **AI Chat** (`chatAi`) — backend empty.
- **Admin** (`admin`) — backend empty; separate web panel.

---

## Reference

- Full team plan (4 devs, parallel lanes, per-dev files): `flutterPlan/README.md` and `flutterPlan/*.md`
- Backend API docs: `docs/AUTH_API.md`, `docs/LEARNING_API_PLAN.md`, `docs/GAMIFICATION_PLAN.md`, `docs/SOCIAL_MODULE_PLAN.md`, `docs/ADMIN_PANEL_GUIDE.md`
- GraphQL schema: `src/main/resources/graphql/social.graphqls`
- DB migrations: `src/main/resources/db/migration/`
