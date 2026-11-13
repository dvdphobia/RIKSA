# Historical auth API audit

> Preserved from the former end of AUTH_API.md. This August 2026 backlog predates later fixes and is retained for context; do not treat its findings or checklist as current without checking the source.

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

