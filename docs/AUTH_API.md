# Riksa Auth, Profile, and Identity Data API

> Audited against the working tree on **August 16, 2026**. This document is the
> frontend source of truth for the current implementation. It intentionally
> distinguishes working behavior from planned behavior.

> Telegram's bot deep-link signup/login flow is documented separately in
> [`FLUTTER_TELEGRAM_AUTH.md`](./FLUTTER_TELEGRAM_AUTH.md). That handoff is the
> source of truth for new Flutter Telegram work.

## Contract and schema version

| Item | Current value | Notes |
|---|---|---|
| REST API version | `v1` | All REST routes use `/api/v1/*` |
| Auth base path | `/api/v1/auth` | Older `/api/auth/*` examples are obsolete |
| Application version | `0.0.1-SNAPSHOT` | From `pom.xml` |
| Latest repository migration | `V20` | Adds Telegram chat identity and short-lived bot deep-link auth challenges |
| Auth schema migrations | `V1`-`V6`, `V15`, `V17` | Core users, refresh tokens, OTP, Telegram ID, lockout, email verification, gamification backfill, suspension |
| Profile concurrency version | None | `users` has no optimistic-lock `version`; profile updates are last-write-wins |
| Gamification concurrency version | `user_gamification.version` | Internal `@Version` field added by `V8`; it is not an API field |

Local origin: `http://localhost:8080`

Production clients must use the deployed HTTPS origin. Examples below show
paths relative to the API origin.

## What the frontend can safely build today

| Capability | Status | Frontend decision |
|---|---|---|
| Username/password register | Development-ready | Build now |
| Username/password login | Development-ready | Login identifier is **username**, not email |
| Load current user | Development-ready with caveat | Call `GET /api/v1/auth/me` with the access token |
| Update basic profile | Development-ready with caveat | Display name, bio, gender, and preset avatar work |
| Refresh token rotation | Development-ready | Replace **both** stored tokens after every refresh |
| Logout one refresh token | Development-ready | Clear local tokens even if the request fails |
| Google sign-in | Partially ready | Requires `GOOGLE_CLIENT_ID`; see security gaps below |
| Province picker/persistence | Blocked end to end | No province-list endpoint, no province seed migration, and profile responses omit province |
| Email verification | Blocked end to end | A token is stored, but no email provider delivers it |
| Telegram signup/login/linking | Development-ready | Use the bot deep-link challenge flow in `FLUTTER_TELEGRAM_AUTH.md`; never ask Flutter for a chat ID |
| Provincial leaderboard | Backend data exists only when a user has a province | Do not make it a required auth/onboarding step yet |
| Public showcase profile | Not implemented | There is only a self-profile endpoint; no `GET /users/{id}` composite profile |

The recommended development flow today is:

1. Register or log in with username and password.
2. Store the access and refresh tokens in platform secure storage.
3. Immediately call `GET /api/v1/auth/me` and use that response as the
   canonical signed-in user.
4. Allow editing `displayName`, `bio`, `gender`, and a preset `avatarUrl`.
5. Hide or locally mock province selection, email verification, password
   recovery, and public showcase fields until the backend gaps are fixed.

## Authentication model

The server is stateless for access-token authentication. It issues:

- Access token: default lifetime **15 minutes** (`900000` ms).
- Refresh token: default lifetime **30 days** (`2592000000` ms).
- Token type returned to clients: `Bearer`.

Both tokens are signed JWTs. Current claims are:

```json
{
  "jti": "random-uuid",
  "sub": "user-uuid",
  "username": "sophea",
  "role": "USER",
  "iat": 1786890000,
  "exp": 1786890900
}
```

Do not parse a JWT to populate the profile UI. JWT claims can become stale;
use `GET /api/v1/auth/me` for user data. The frontend must never log either
token.

### Route access

| Method | Path | Intended access | Current behavior |
|---|---|---|---|
| `POST` | `/register` | Public | Public |
| `POST` | `/login` | Public | Public |
| `POST` | `/refresh` | Public with refresh token in body | Public |
| `POST` | `/logout` | Public with refresh token in body | Public |
| `POST` | `/google` | Public | Public |
| `POST` | `/otp/send` | Public | Public |
| `POST` | `/otp/verify` | Public | Public |
| `POST` | `/forgot-password` | Public | Public |
| `POST` | `/reset-password` | Public | Public |
| `POST` | `/verify-email` | Public | Public |
| `GET` | `/me` | Bearer access token | The whole `/auth/**` tree is permitted by Spring Security, but this controller manually parses a required Bearer token |
| `PUT` | `/me` | Bearer access token | Same caveat as `GET /me` |

All `/api/v1/auth/**` requests currently share one in-memory, per-IP rate
limit: **5 requests per 60 seconds** by default. A `429` can therefore be
triggered by a short sequence such as register, profile load, profile update,
and retries.

## Shared response objects

### `AuthResponse`

Returned by register, login, refresh, and Google sign-in.

```json
{
  "userId": "a1b2c3d4-e5f6-4890-abcd-ef1234567890",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "username": "sophea",
  "role": "USER"
}
```

`AuthResponse` is session data, not a full user profile. It does not include
display name, email, avatar, province, verification state, XP, streak, or rank.

### `UserProfileResponse`

This exact shape is returned by `GET /me` and `PUT /me`:

```json
{
  "id": "a1b2c3d4-e5f6-4890-abcd-ef1234567890",
  "username": "sophea",
  "displayName": "Sok Sophea",
  "email": "sophea@example.com",
  "emailVerified": false,
  "avatarUrl": "/avatars/avatar-17.png",
  "bio": "Learning STEM and technology.",
  "gender": "female",
  "phoneNumber": "+85512345678",
  "phoneVerified": false,
  "role": "USER",
  "countryId": "KH",
  "createdAt": "2026-08-16T20:30:00+07:00"
}
```

Nullable fields can be returned as `null`: `email`, `avatarUrl`, `bio`,
`gender`, and `phoneNumber`. Google accounts may have different nullability
from password accounts.

The current response does **not** include:

- `provinceId` or province name;
- `updatedAt`;
- `telegramId`;
- access or refresh tokens;
- XP, streak, achievements, tier, or leaderboard rank.

### `MessageResponse`

```json
{
  "message": "Logged out successfully"
}
```

### Standard application error

Controller and validation errors generally use:

```json
{
  "timestamp": "2026-08-16T20:30:00.123+07:00",
  "status": 400,
  "error": "Bad Request",
  "message": "username: Username is required"
}
```

Rate-limit responses are a smaller JSON object without `timestamp`:

```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Try again later."
}
```

## Endpoint reference

All request bodies use `Content-Type: application/json`.

### Register

`POST /api/v1/auth/register`

Creates, in one database transaction:

1. one `users` row;
2. one zero-valued `user_gamification` row;
3. one 24-hour `email_verification_tokens` row;
4. one `refresh_tokens` row.

The access token is returned but is not stored in PostgreSQL.

Request:

| Field | Type | Required | Current validation/behavior |
|---|---|---|---|
| `username` | string | Yes | 3-50 characters; exact-case uniqueness check |
| `displayName` | string | Yes | Non-blank; at most 100 characters |
| `email` | string | Yes | Bean Validation email; exact-case uniqueness check |
| `password` | string | Yes | At least 8 characters with uppercase, lowercase, and a digit |
| `phoneNumber` | string | No | No format validation; database unique if non-null |
| `gender` | string | No | No enum/allow-list validation; database length is 20 |
| `provinceId` | UUID string | No | Looked up if supplied; an unknown UUID is currently silently ignored |

Example:

```json
{
  "username": "sophea",
  "displayName": "Sok Sophea",
  "email": "sophea@example.com",
  "password": "StrongPass1",
  "phoneNumber": "+85512345678",
  "gender": "female"
}
```

Success: `200 OK` with `AuthResponse`.

Common errors:

| Status | Message/condition |
|---|---|
| `400` | `Username is already taken` |
| `400` | `Email is already taken` |
| `400` | DTO validation errors |
| `429` | Shared auth rate limit exceeded |
| `500` | A database constraint fails outside the service's explicit checks, for example a duplicate phone number |

Important frontend rule: do not send an empty string for optional
`phoneNumber`. Send `null` or omit the field. Multiple empty strings collide
with the database unique constraint.

### Login

`POST /api/v1/auth/login`

Request:

```json
{
  "username": "sophea",
  "password": "StrongPass1"
}
```

The endpoint accepts **username only**. Email login is not implemented.

Success: `200 OK` with a new `AuthResponse` and a newly persisted refresh
token.

| Status | Message/condition |
|---|---|
| `400` | `Invalid username or password` |
| `400` | `Account is temporarily locked. Try again later.` |
| `403` | Account is suspended |
| `429` | Shared auth rate limit exceeded |

The code intends to lock an account after five failed attempts for 15 minutes.
The current transaction rolls back the failed-attempt update when it throws the
login error, so lockout persistence must be fixed before it is relied on for
security.

### Refresh session

`POST /api/v1/auth/refresh`

Request:

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

Success: `200 OK` with a completely new `AuthResponse`. On a successful
transaction, the supplied refresh-token row is marked revoked and a new row is
created.

Frontend requirements:

- overwrite both access and refresh tokens atomically;
- allow only one refresh request at a time;
- queue concurrent protected requests behind that refresh;
- retry each failed protected request at most once;
- if refresh fails, delete both local tokens and move to signed-out state.

Expected errors are `400` for an invalid, expired, missing, or already-revoked
refresh token and `403` for a suspended user.

### Logout

`POST /api/v1/auth/logout`

Request:

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

Success is always:

```json
{
  "message": "Logged out successfully"
}
```

The server revokes the matching active refresh-token row if it exists. It does
not revoke other device sessions, and the access token remains valid until its
expiry. The frontend should clear local session data in a `finally` path.

### Current user

`GET /api/v1/auth/me`

Header:

```http
Authorization: Bearer <accessToken>
```

Success: `200 OK` with `UserProfileResponse`.

Use this request:

- after register/login;
- on app startup when a locally stored session exists;
- after a successful refresh if profile freshness matters;
- after returning to the foreground when another device/admin may have changed
  the account.

Do not send a refresh token as the Bearer value, even though the current backend
does not technically distinguish token types. That is a security defect, not a
supported contract.

### Update current user

`PUT /api/v1/auth/me`

Header:

```http
Authorization: Bearer <accessToken>
```

All fields are optional. Omitted or JSON `null` fields mean “leave unchanged.”

| Field | Type | Current behavior |
|---|---|---|
| `displayName` | string | Updates directly; DTO currently has no blank/length validation |
| `bio` | string | Updates directly; database maximum is 160 characters |
| `gender` | string | Updates directly; database maximum is 20 characters |
| `provinceId` | UUID string | Existing province is assigned; unknown UUID is silently ignored |
| `avatarUrl` | string | Must exactly match `/avatars/avatar-1.png` through `/avatars/avatar-30.png` |

Example:

```json
{
  "displayName": "Sok Sophea",
  "bio": "Learning every day.",
  "gender": "female",
  "avatarUrl": "/avatars/avatar-17.png"
}
```

Success: `200 OK` with the full `UserProfileResponse`.

The endpoint currently cannot clear a province because `null` means unchanged.
There is also no optimistic concurrency/version field for profile editing.

### Avatar assets

Preset avatars are public static assets:

```text
GET /avatars/avatar-1.png
...
GET /avatars/avatar-30.png
```

A relative stored value such as `/avatars/avatar-17.png` should be rendered as:

```text
<api-origin>/avatars/avatar-17.png
```

Google sign-in can currently store an external Google picture URL instead of a
preset path. A frontend avatar resolver therefore needs to support both an
absolute HTTPS URL and a server-relative `/avatars/...` path.

### Google sign-in

`POST /api/v1/auth/google`

The field name is `idToken`, not `googleIdToken`.

```json
{
  "idToken": "google-id-token",
  "username": "sophea",
  "displayName": "Sok Sophea",
  "provinceId": "optional-province-uuid"
}
```

Only `idToken` is required. The server verifies its signature/audience using
`GOOGLE_CLIENT_ID`, then finds a user by Google subject or automatically links
an existing user with the same email. New Google users also receive a
`user_gamification` row.

Success: `200 OK` with `AuthResponse`.

Current caveats:

- the server does not check Google's `email_verified` claim before linking by
  email;
- an existing suspended Google user is not checked before tokens are issued;
- Google picture URLs bypass the preset-avatar policy;
- the local `emailVerified` field is not marked true for a verified Google
  identity.

Treat Google sign-in as development-only until these are fixed.

### Send Telegram OTP

`POST /api/v1/auth/otp/send`

Current request:

```json
{
  "identifier": "1081239995"
}
```

There is no `channel` field. The only Java enum value is currently `TELEGRAM`,
even though the PostgreSQL enum still contains the older `SMS` value.

Success:

```json
{
  "message": "OTP sent successfully",
  "channel": "TELEGRAM"
}
```

The code is not returned in the API response. It is sent to the Telegram chat
ID. The user must have started the bot, and the backend needs a valid
`TELEGRAM_BOT_TOKEN`.

OTP defaults: six digits, five-minute expiry, maximum three wrong attempts.

### Verify Telegram OTP

`POST /api/v1/auth/otp/verify`

```json
{
  "identifier": "1081239995",
  "code": "931293"
}
```

Valid code:

```json
{
  "verified": true,
  "message": "Verification successful"
}
```

A wrong code returns `200 OK` with `verified: false`. Expired, missing, or
over-attempted codes return `400`.

If a `users.telegram_id` row already matches the identifier, successful OTP
verification sets `phone_verified = true`. No auth endpoint currently attaches
that Telegram ID to the signed-in user, so this does not complete a normal
phone/Telegram verification flow.

### Forgot password

`POST /api/v1/auth/forgot-password`

```json
{
  "identifier": "sophea"
}
```

Lookup order is username, email, then Telegram ID. Password recovery only works
if the found user already has `telegram_id` populated.

Nominal success:

```json
{
  "message": "If an account exists with that identifier, an OTP has been sent"
}
```

Despite that generic text, the service currently returns distinguishable `400`
errors for an unknown account and for an account without Telegram. This allows
account enumeration and means the normal registration flow cannot recover a
password. Do not expose this screen as a finished production flow.

### Reset password

`POST /api/v1/auth/reset-password`

```json
{
  "identifier": "sophea",
  "code": "931293",
  "newPassword": "NewStrongPass1"
}
```

The new password follows the registration password rule. Success:

```json
{
  "message": "Password has been reset successfully"
}
```

Existing refresh tokens are not revoked after a password reset. This is a
production security blocker.

### Verify email

`POST /api/v1/auth/verify-email`

```json
{
  "token": "verification-token"
}
```

Success:

```json
{
  "verified": true,
  "message": "Email verified successfully"
}
```

Registration creates a 24-hour verification token, but delivery is a stub. The
token is neither emailed nor returned, so a newly registered user cannot reach
this endpoint through a normal frontend flow.

## PostgreSQL identity data model

### `users`

This is the canonical identity/profile table.

| Column | Type | Null? | Used by frontend/features |
|---|---|---:|---|
| `id` | UUID | No | Stable user ID, JWT subject, all cross-domain foreign keys |
| `username` | varchar(50), unique | No | Login and public handle |
| `display_name` | varchar(100) | No | Profile, social author, live leaderboard |
| `avatar_url` | text | Yes | Profile, social author, live leaderboard |
| `bio` | varchar(160) | Yes | Self profile; no public profile API yet |
| `gender` | varchar(20) | Yes | Self profile |
| `role` | `Role` enum | No | `USER`, `MODERATOR`, or `ADMIN` |
| `phone_number` | varchar(20), unique | Yes | Self profile; not used by current OTP implementation |
| `phone_verified` | boolean | No | Self profile; currently conflated with Telegram OTP verification |
| `telegram_id` | text, unique | Yes | Telegram recovery; not exposed by profile API |
| `password` | text | Yes | BCrypt hash for password accounts; null is possible for Google-only accounts |
| `google_id` | text, unique | Yes | Google account link |
| `email` | text, unique | Yes | Required by normal register; possible null for some OAuth data |
| `email_verified` | boolean | No | Self profile |
| `country_id` | varchar(2) | Yes | Defaults to `KH` |
| `province_id` | UUID FK | Yes | Province leaderboard membership |
| `failed_login_attempts` | integer | No | Intended lockout state |
| `locked_until` | timestamptz | Yes | Intended login lockout |
| `suspended_until` | timestamptz | Yes | Admin suspension |
| `suspension_reason` | varchar(255) | Yes | Admin-only data |
| `created_at` | timestamptz | Yes | Returned by self profile |
| `updated_at` | timestamptz | Yes | Maintained by JPA/database trigger, not returned by self profile |

PostgreSQL uniqueness is case-sensitive with the current schema. The service
does not normalize usernames, emails, or phone numbers, so `User@Example.com`
and `user@example.com` may become different rows.

### Auth/session tables

| Table | Purpose | Retention/current behavior |
|---|---|---|
| `refresh_tokens` | Server-side refresh-token allow/revoke list | Full JWT stored in plaintext; expired rows deleted daily at 03:00 server time |
| `otp_codes` | Telegram OTP state and attempt count | Raw code stored; expired rows deleted daily |
| `email_verification_tokens` | Email verification state | Raw token stored; expired rows deleted daily |
| `user_gamification` | XP/streak aggregate for every registered user | Created during normal and Google registration; `V15` backfills older users |

The repository has no migration that inserts province rows. A fresh database
therefore has an empty `provinces` table unless it is populated manually.

## How auth/profile data feeds leaderboard and showcase

Identity and gamification are intentionally separate:

```text
users.id
  ├── user_gamification.user_id     -> XP, streak, live rank input
  ├── leaderboard_snapshots.user_id -> country/province snapshot ranks
  ├── user_achievements.user_id     -> achievement showcase
  ├── lesson_completions.user_id    -> lessons completed
  └── posts/comments/...            -> social profile content
```

### Live XP leaderboard

`GET /api/v1/gamification/leaderboard?cursor=&limit=20`

Response entries:

```json
{
  "rank": 1,
  "userId": "a1b2c3d4-e5f6-4890-abcd-ef1234567890",
  "displayName": "Sok Sophea",
  "avatarUrl": "/avatars/avatar-17.png",
  "totalXp": 5550,
  "tierName": "GOLD",
  "tierIcon": "/icons/gold.png"
}
```

This endpoint joins `user_gamification` with `users`, so auth profile edits to
display name/avatar are visible immediately. It is cursor-paginated and ordered
by total XP. It has no province, no score-with-streak, and no public profile
details. Registered zero-XP users and suspended users are currently included.

### Snapshot country/province leaderboard

`GET /api/v1/leaderboard?scope=COUNTRY&scopeId=CAMBODIA&date=2026-08-16`

or:

`GET /api/v1/leaderboard?scope=PROVINCE&scopeId=<province-uuid>&date=2026-08-16`

Snapshot score is:

```text
total_xp + (current_streak * 5)
```

The scheduled job refreshes the current date every five minutes. Province rows
exist only for users with a non-null `users.province_id`.

Current row response:

```json
{
  "rank": 1,
  "userId": "a1b2c3d4-e5f6-4890-abcd-ef1234567890",
  "score": 5615,
  "scope": "PROVINCE",
  "scopeId": "province-uuid",
  "rankingTier": "GOLD",
  "snapshoDate": "2026-08-16"
}
```

`snapshoDate` is the current misspelled wire field. More importantly, snapshot
rows do not contain `displayName`, `avatarUrl`, username, or province name, and
there is no bulk public-user endpoint to resolve those IDs. This surface cannot
currently render the designed leaderboard rows by itself.

`GET /api/v1/leaderboard/me?scopeId=<province-uuid>` returns:

```json
{
  "countryRank": 15,
  "countryScore": 5615,
  "countryTier": "GOLD",
  "provinceRank": 4,
  "provinceScore": 5615,
  "provinceTier": "GOLD"
}
```

When `scopeId` is omitted, all province fields are `null`. Missing country data
uses `0`/`null` rather than `404`.

### Self profile composition for today's UI

For the signed-in user's own profile, compose:

| UI section | Endpoint |
|---|---|
| Identity/avatar/bio/join date | `GET /api/v1/auth/me` |
| XP/streak/current tier | `GET /api/v1/gamification/me` |
| Achievement showcase | `GET /api/v1/gamification/achievements` |
| Country/province rank | `GET /api/v1/leaderboard/me` |
| User posts | GraphQL `feed(authorId: <self-user-id>)` |
| XP history | `GET /api/v1/gamification/xp` |

Province name, followers/following, banner image, public lessons list, public XP
history, and a public achievement showcase are not available in a complete
profile contract.

For another user's showcase page, the backend needs a new public composite API;
do not call self-only endpoints with a leaderboard user's ID because they do not
accept one.

## Frontend session contract

### Minimum persisted session

Persist only what is needed to restore authentication:

```text
accessToken
refreshToken
```

Keep the current `UserProfileResponse` in memory/application state and refresh
it from `/me`. If it is cached locally for fast startup, treat it as stale and
replace it after the network request.

### Bootstrap state machine

```text
app starts
  ├── no tokens -> signed out
  └── tokens exist
       ├── GET /auth/me succeeds -> signed in
       ├── request is unauthorized -> refresh once
       │    ├── refresh succeeds -> store pair -> retry /me -> signed in
       │    └── refresh fails -> clear tokens -> signed out
       └── offline/timeout -> keep a distinct recoverable/offline state
```

Do not interpret every network failure as logout. Only an authentication or
refresh failure should destroy the stored session.

### Refresh pseudocode

```text
on protected request failure:
  if this request was already retried:
    fail
  acquire single refresh lock
  if another request already replaced the access token:
    retry with the new token
  else:
    POST /api/v1/auth/refresh with the stored refresh token
    securely replace accessToken and refreshToken
  release lock
  retry original request once
```

### Recommended frontend user model

Model exactly the current response and keep future/proposed fields separate:

```text
AuthUser
  id: UUID/string
  username: string
  displayName: string
  email: string?
  emailVerified: bool
  avatarUrl: string?
  bio: string?
  gender: string?
  phoneNumber: string?
  phoneVerified: bool
  role: USER | MODERATOR | ADMIN
  countryId: string?
  createdAt: offset-aware timestamp?
```

Do not add `provinceId` or `updatedAt` as required JSON fields yet; the backend
does not return them.

## Audit findings and required backend work

### P0 — fix before production authentication

1. **Separate access and refresh token types.** Current JWTs have identical
   claims apart from expiry, and the auth filter accepts any valid signed JWT.
   A refresh token can therefore authenticate as a Bearer token, even after its
   database row is revoked.
2. **Require a production JWT secret.** The application has a known development
   fallback. Production startup should fail if `JWT_SECRET` is absent/weak.
3. **Protect `/auth/me` through Spring Security.** Permit only the genuinely
   public auth routes and use the authenticated principal instead of manually
   parsing the header.
4. **Persist failed-login attempts outside the rolled-back login transaction.**
   Current lockout updates roll back with the thrown credential error.
5. **Fix Google linking.** Require Google's verified-email claim and reject
   suspended users before issuing tokens.
6. **Revoke all user refresh tokens after password reset.**
7. **Make forgot-password non-enumerating.** Return the same public result for
   existing and missing accounts and move diagnostic details to secure logs.
8. **Store hashes of refresh/verification tokens rather than raw secrets.**

### P0 — fix before province onboarding/leaderboard

1. Add and seed a province catalog migration.
2. Add a public/readable `GET /api/v1/provinces` contract returning ID, Khmer
   name, and Latin name.
3. Reject unknown `provinceId` with `400`; do not silently ignore it.
4. Return `provinceId` and province names from `UserProfileResponse`.
5. Add an explicit way to clear/change province and define what happens to
   historical leaderboard snapshots.

### P1 — fix for leaderboard and showcase

1. Choose and document one canonical main leaderboard. The live XP and snapshot
   score leaderboards currently rank users differently.
2. Enrich snapshot rows with display name, avatar, username, and province, or
   provide a bulk public-user lookup.
3. Add cursor/page limits to snapshot leaderboards; the current endpoint returns
   an unbounded list.
4. Correct `snapshoDate` to `snapshotDate` with a versioned compatibility plan.
5. Use `long`/PostgreSQL `BIGINT` for snapshot score because XP is already
   `BIGINT` while snapshot score is still `INTEGER`/Java `int`.
6. Define whether suspended users should remain visible; both leaderboard paths
   currently include them.
7. Add a public profile/showcase endpoint that returns intentionally public
   identity plus gamification summary. Never reuse the self profile response,
   which contains email and phone number.

### P1 — data quality and contract cleanup

1. Normalize username/email/phone values and enforce case-insensitive uniqueness
   where appropriate.
2. Validate optional phone, gender, profile display name, and bio at the DTO
   boundary so database errors do not become `500` responses.
3. Return `409 Conflict` for uniqueness conflicts instead of generic `400`/`500`.
4. Add profile `updatedAt` and an optimistic version/ETag if concurrent profile
   edits matter.
5. Complete either Telegram attachment/verification or email-based recovery;
   hide unfinished alternatives from the production client.
6. Scope rate limits by endpoint and account as well as trusted client IP. The
   current filter trusts `X-Forwarded-For`, is per-process, and applies the same
   small budget to every auth route.

## Frontend acceptance checklist

- [ ] All paths use `/api/v1/auth`, never `/api/auth`.
- [ ] Login form says username, not email.
- [ ] Registration password UI enforces uppercase + lowercase + digit + 8 chars.
- [ ] Empty optional values are omitted/null rather than sent as empty strings.
- [ ] Access and refresh tokens use platform secure storage and never logs.
- [ ] Register/login/Google/refresh responses replace the entire local session.
- [ ] `/me` is fetched after authentication; JWT claims are not the profile.
- [ ] A single-flight refresh lock prevents multiple simultaneous rotations.
- [ ] Each protected request is retried no more than once.
- [ ] Logout clears local state even when the network request fails.
- [ ] Relative avatar paths are resolved against the API origin.
- [ ] Province, email verification, recovery, and public showcase features are
      feature-gated until their backend blockers are resolved.

## Implementation sources

- Auth routes: `src/main/java/com/riksa/auth/controller/AuthController.java`
- Auth behavior: `src/main/java/com/riksa/auth/service/AuthService.java`
- Google behavior: `src/main/java/com/riksa/auth/service/OAuth2Service.java`
- Request/response fields: `src/main/java/com/riksa/auth/dto/`
- JWTs and security: `src/main/java/com/riksa/auth/security/`
- User schema mapping: `src/main/java/com/riksa/auth/entity/User.java`
- Flyway schema: `src/main/resources/db/migration/`
- Live XP leaderboard: `src/main/java/com/riksa/gamification/service/GamificationService.java`
- Snapshot leaderboard: `src/main/java/com/riksa/leaderboard/`
