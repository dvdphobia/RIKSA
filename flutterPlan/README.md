# Riksa Flutter App — Team Build Plan

> Mobile client for the **Riksa Learning Platform** (Cambodian educational app).
> Backend: Spring Boot 4 — REST under `/api/v1/*` + GraphQL at `/graphql`.
> Team size: **4 Flutter developers** working in parallel.

---

## 1. How to use this plan

| File | Purpose | Owner |
|------|---------|-------|
| `README.md` (this file) | Overview, architecture, ground rules, timeline | Everyone |
| `00-foundation-shared.md` | Shared scaffolding everyone depends on (build first, together) | Dev A leads |
| `01-dev-a-auth-and-shell.md` | Auth, app shell, navigation, token lifecycle | Dev A |
| `02-dev-b-social-graphql.md` | Community feed, posts, comments, reactions (GraphQL) | Dev B |
| `03-dev-c-gamification-leaderboard.md` | XP, streaks, achievements, ranks, leaderboard | Dev C |
| `04-dev-d-blog-learning-profile.md` | Blog, learning, profile & settings | Dev D |
| `api-contract-reference.md` | Every endpoint each feature consumes (REST + GraphQL) | Everyone |

**Read order for the team:** this file → `00-foundation-shared.md` → your own dev file → `api-contract-reference.md`.

---

## 2. Backend reality check (read before estimating)

The backend is **partially complete**. Plan around this:

| Module | Backend status | Flutter impact |
|--------|----------------|----------------|
| Auth (REST) | ✅ Done | Build fully now |
| Blog (REST) | ✅ Done | Build fully now |
| Gamification (REST) | ✅ Done (profile, xp, achievements, rank, streak, leaderboard) | Build fully now |
| Leaderboard (REST) | ✅ Done | Build fully now |
| Social (GraphQL) | ⚠️ Partial — feed + basic mutations work, but `author` resolution, nested comments/replies, the `comments` query, `sharePost`, and `myReaction` are broken/missing on the server | Build UI now, but coordinate with backend; some screens will be blocked until resolvers are fixed |
| Learning (REST) | ❌ Stub — only a `Lesson` entity exists, no endpoints | Build UI against mocks; wire up when backend lands |
| AI Chat | ❌ Stub — empty | Out of scope for v1 |
| Admin | ❌ Stub — likely a separate web panel, not mobile | Out of scope |

**Implication:** Dev B (social) and Dev D (learning) have backend dependencies. They start with UI + mock data and integrate as the server catches up. Dev A and Dev C have fully working backends and are not blocked.

---

## 3. Architecture & stack (agreed defaults)

Keep the stack identical across all four feature areas so code is portable between developers.

| Concern | Choice | Notes |
|---------|--------|-------|
| State management | **Riverpod** (`flutter_riverpod` + `riverpod_generator`) | Compile-safe, testable, no `BuildContext` coupling |
| Navigation | **go_router** | Declarative, deep-link ready; single shared router |
| REST client | **dio** | Interceptors for auth + refresh + logging |
| GraphQL client | **graphql_flutter** | Social module only |
| Models / DTOs | **freezed** + **json_serializable** | Immutable models, generated (de)serialization |
| Secure storage | **flutter_secure_storage** | Access/refresh tokens |
| Local cache | **shared_preferences** (settings) + optional **hive/drift** (offline) | Decide per feature |
| Localization | **flutter_localizations** + **intl** (`.arb`) | English + Khmer (ខ្មែរ) from day one |
| Theming | Central `AppTheme` (Material 3) | Owned in foundation |
| Testing | `flutter_test`, `mocktail`, `golden_toolkit` | Unit + widget + golden |
| Lint | `flutter_lints` (or `very_good_analysis`) | Enforced in CI |

### Folder structure (feature-first → enables parallel ownership)

```
lib/
  main.dart
  app/                      # [FOUNDATION] app shell, router, theme, DI
    app.dart
    router/app_router.dart
    theme/
  core/                     # [FOUNDATION] shared infra, no feature logic
    network/                # dio client, graphql client, interceptors
    storage/                # secure storage, prefs
    error/                  # failures, exception mapping
    widgets/                # shared buttons, loaders, empty/error states
    localization/
  features/
    auth/                   # Dev A
    social/                 # Dev B
    gamification/           # Dev C
    leaderboard/            # Dev C
    blog/                   # Dev D
    learning/               # Dev D
    profile/                # Dev D
```

Each `features/<name>/` follows the same internal layout so devs can read each other's code:

```
features/<name>/
  data/         # datasources (api), dtos, repositories impl
  domain/       # entities, repository interfaces, usecases (optional)
  presentation/ # screens, widgets, controllers (riverpod providers)
```

**Ownership rule:** a developer only edits files inside their assigned `features/*` folders plus their assigned slices of `core`/`app`. Shared files (router, theme, dio client) have a single owner (Dev A) — others request changes via PR review, never edit directly.

---

## 4. Team roles & ownership map

| Dev | Role | Owns (folders) | Backend dependency |
|-----|------|----------------|--------------------|
| **Dev A** | Tech Lead | `app/`, `core/`, `features/auth/` | None (auth done) |
| **Dev B** | Social/Community | `features/social/` + `core/network/graphql_client.dart` | ⚠️ partial GraphQL |
| **Dev C** | Engagement | `features/gamification/`, `features/leaderboard/` | None (done) |
| **Dev D** | Content & Profile | `features/blog/`, `features/learning/`, `features/profile/` | ⚠️ learning is a stub |

Dev A is also the **integrator**: owns the router, merges feature routes, and runs the weekly integration build.

---

## 5. Parallelization strategy

The whole point: minimize blocking. We do this with a short shared sprint, then independent feature lanes that meet at well-defined seams.

```
Week 0  ── FOUNDATION SPRINT (all 4 together) ────────────────────────────
            Scaffold, stack, CI, design system, auth plumbing, fake repos.
            Nobody builds features until the seams below exist.

Week 1+ ── PARALLEL LANES ─────────────────────────────────────────────────
  Dev A: Auth screens ─► App shell/nav ─► Profile handoff to D
  Dev B: Social UI (mock) ─► integrate GraphQL as resolvers land
  Dev C: Gamification dashboard ─► Leaderboard ─► achievements
  Dev D: Blog (full) ─► Learning (mock) ─► Profile/Settings

         Weekly: integration build + demo on `develop`.
```

### The seams (contracts that unblock parallel work)
1. **Auth state provider** — `authControllerProvider` exposes `Stream<AuthState>` (unauthenticated / authenticated(user) / loading). Every feature reads it; nobody implements it but Dev A. Available end of Week 0.
2. **Authenticated network clients** — `dioProvider` (REST) and `graphqlClientProvider` (GraphQL) come pre-wired with the bearer token + auto-refresh. Features never touch tokens directly. Available end of Week 0.
3. **Router registration** — each feature exports a `List<RouteBase>` that Dev A composes into the root router. Features never edit the root router file.
4. **Repository interfaces** — each feature defines its repo interface in `domain/` first, backs it with a `Fake<Repo>` returning canned data, then swaps in the real `Api<Repo>`. This lets UI work start before the backend call is wired.

---

## 6. Timeline (suggested, ~6 weeks to v1)

| Week | Dev A | Dev B | Dev C | Dev D |
|------|-------|-------|-------|-------|
| 0 | Foundation sprint (everyone): scaffold, CI, theme, network, auth seam, fakes |
| 1 | Login/Register/OTP UI | Feed UI (mock) | Gamification dashboard | Blog list + detail |
| 2 | Google login, token refresh hardening, app shell + bottom nav | Post detail + create | XP history + streak | Blog create + like, moderation view |
| 3 | Forgot/reset password, deep links | Comments + reactions (as backend lands) | Achievements + rank history | Learning UI (mock) |
| 4 | Profile edit handoff, settings shell | Categories + join/leave | Leaderboard + my-rank | Profile screen + avatar |
| 5 | Localization pass (KM/EN), error states | sharePost + polish | Animations (XP gain, level up) | Settings, learning integration |
| 6 | Hardening, release build, store prep — **all hands** |

Adjust to your real velocity; the dependency order matters more than the exact weeks.

---

## 7. Git & collaboration workflow

- **Branches:** `main` (releases) ← `develop` (integration) ← `feat/<area>/<short-desc>` (per dev).
- **Branch naming by area:** `feat/auth/login-screen`, `feat/social/feed`, `feat/gamification/streak-widget`, `feat/blog/detail`.
- **PRs:** small and focused, target `develop`. At least **1 review** (Dev A reviews shared-code changes; peers review feature PRs).
- **Protected `develop`:** CI must pass (analyze + test + build) before merge.
- **No direct edits to shared files** (`app_router.dart`, `app_theme.dart`, `dio_client.dart`) without Dev A review — these are merge-conflict hotspots.
- **Daily 15-min standup**, **weekly integration build + demo** on `develop`.
- **Codegen:** commit generated files (`*.g.dart`, `*.freezed.dart`) is optional — agree as a team. Recommended: gitignore them and run `dart run build_runner build` in CI + a documented setup step, to avoid generated-file merge conflicts.

---

## 8. Definition of Done (every feature)

- [ ] Screens implemented against the repository interface (not direct API calls in widgets)
- [ ] Loading / empty / error states handled with shared `core/widgets`
- [ ] Khmer + English strings (no hardcoded user-facing text)
- [ ] Riverpod providers unit-tested; key widgets have widget tests
- [ ] Works on Android + iOS; respects safe areas and dark mode
- [ ] No analyzer warnings; PR reviewed and merged to `develop`
- [ ] Accessibility: semantic labels on interactive elements, adequate contrast, scalable text

---

## 9. Risks & mitigations

| Risk | Mitigation |
|------|------------|
| Social GraphQL resolvers incomplete (author/comments/sharePost broken) | Dev B builds against mock repo + a local schema; backend team fixes resolvers in parallel; integrate behind a feature flag |
| Learning module is a backend stub | Dev D builds UI against fakes; ship behind a flag until endpoints exist |
| Shared-file merge conflicts | Single owner (Dev A) + route/theme registration pattern (no direct edits) |
| Token refresh race conditions | Centralized dio interceptor with a single-flight refresh lock (Dev A owns) |
| Two API styles (REST + GraphQL) confusing the team | Clear seam: only `features/social` touches GraphQL; everything else is dio/REST |

---

## 10. First-day checklist (Week 0, all 4)

1. Agree Flutter SDK version, create the repo, add `.gitignore`, set up `develop`.
2. Dev A scaffolds the project per Section 3 and merges the foundation PR.
3. Confirm each dev can run the app against a local backend (`http://localhost:8080`, GraphQL at `/graphql`).
4. Each dev creates their feature folder with a `Fake<Repo>` and one placeholder screen wired into the router.
5. Read `00-foundation-shared.md` and your assigned dev file.

> See `api-contract-reference.md` for the exact endpoints and payloads each lane consumes.
