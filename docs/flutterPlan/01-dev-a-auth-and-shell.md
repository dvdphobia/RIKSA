# Dev A — Tech Lead: Auth, App Shell & Foundation

> **Backend status: ✅ fully implemented.** You are not blocked by the backend.
> You own the seams everyone else depends on, so you ship foundation first.

---

## Mission

Own the **app skeleton** (shell, navigation, theme, network, auth) and the entire **authentication experience**. You are also the **integrator**: you compose feature routes and run the weekly integration build.

## Folders you own

- `app/` — app entry, router, theme, DI
- `core/` — network, storage, error, shared widgets, localization
- `features/auth/`

> Changes to `app/router/app_router.dart`, `core/theme/`, and `core/network/dio_client.dart` go through **you**. Other devs request changes via PR.

---

## Backend you consume (REST, `/api/v1/auth`)

All auth endpoints are public (no token). See `api-contract-reference.md` for payloads.

| Screen / action | Endpoint |
|-----------------|----------|
| Register | `POST /api/v1/auth/register` |
| Login | `POST /api/v1/auth/login` |
| Refresh token | `POST /api/v1/auth/refresh` |
| Logout | `POST /api/v1/auth/logout` |
| Google sign-in | `POST /api/v1/auth/google` |
| Send OTP (SMS/Telegram) | `POST /api/v1/auth/otp/send` |
| Verify OTP | `POST /api/v1/auth/otp/verify` |
| Forgot password | `POST /api/v1/auth/forgot-password` |
| Reset password | `POST /api/v1/auth/reset-password` |
| Verify email | `POST /api/v1/auth/verify-email` |
| Current user | `GET /api/v1/auth/me` (Bearer) |
| Update profile | `PUT /api/v1/auth/me` (Bearer) |

Token lifetimes: **access 15 min, refresh 30 days**. Refresh returns a *new pair*.

---

## Work breakdown

### Phase 0 — Foundation (Week 0, with the team)
Deliver everything in `00-foundation-shared.md`. This is your top priority; the other three are partly blocked until the seams exist.

### Phase 1 — Core auth (Week 1)
- [ ] `AuthRepository` (interface + API impl) wrapping the endpoints above
- [ ] `authControllerProvider` with `AuthState` (loading/authenticated/unauthenticated)
- [ ] Login screen (username + password) with validation + error states
- [ ] Register screen (username, displayName, email, password, optional phone/gender/province)
- [ ] Token persistence + auto-load session on app start (`loadCurrentUser`)
- [ ] Router redirect logic (logged-out → `/auth/login`)

### Phase 2 — App shell + social/OAuth login (Week 2)
- [ ] `AppShell` with bottom navigation (Feed · Learn · Leaderboard · Blog · Profile)
- [ ] Google sign-in (`google_sign_in` → send id token to `POST /auth/google`)
- [ ] Harden the dio refresh interceptor (single-flight lock; tested under concurrent 401s)
- [ ] Province picker (fetch provinces — confirm endpoint with backend; Cambodian provinces)

### Phase 3 — Recovery flows (Week 3)
- [ ] Forgot password → OTP/email code → reset password flow
- [ ] Email verification flow
- [ ] OTP flow UI (SMS + Telegram channels; Telegram requires chat ID — show helper text)
- [ ] Deep link handling (password reset / email verify links)

### Phase 4 — Handoff + polish (Weeks 4–6)
- [ ] Expose a clean `AuthUser` model + `authControllerProvider` for Dev D's profile screen
- [ ] Settings shell (logout, language toggle KM/EN, delete session)
- [ ] Localization pass across auth + shared widgets
- [ ] Release: app icons, splash, signing, store metadata (all-hands in Week 6)

---

## Integrator duties (ongoing)

- Review every PR that touches `app/`, `core/network`, or `core/theme`.
- Compose feature route lists into the root router (features hand you a `List<RouteBase>`).
- Run and publish the **weekly integration build** on `develop`; host the demo.
- Keep the dependency-injection wiring (`ProviderScope` overrides, fake↔real switch) coherent.

---

## Definition of Done (your lane)
Standard DoD (`README.md` §8) plus:
- [ ] Tokens never appear in logs or widget code — only in `core`
- [ ] Refresh interceptor covered by a test simulating concurrent 401s
- [ ] Every other dev can register a route and read auth state without editing your files
