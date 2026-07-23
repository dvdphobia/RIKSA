# Dev C — Gamification & Leaderboard

> **Backend status: ✅ fully implemented** (profile, XP history, achievements, rank history, streak, leaderboard + the write pipeline). You are **not blocked** — build everything against the real API.

---

## Mission

Build the engagement layer: the gamification dashboard (XP, level/tier, streak), XP transaction history, achievements gallery, rank progression, and the leaderboard. This is what makes the app sticky — invest in delightful animations.

## Folders you own
- `features/gamification/`
- `features/leaderboard/`

---

## Backend you consume (REST, Bearer JWT)

### Gamification — `/api/v1/gamification`
| Screen | Endpoint | Returns | Paginated |
|--------|----------|---------|:--:|
| Dashboard / profile | `GET /me` | `GamificationProfileResponse` (totalXp, currentStreak, highestStreak, currentTier, currentTierIcon, nextTier, xpToNext) | — |
| XP history | `GET /xp?cursor=<base64>&limit=20` | `CursorPage<XpTransactionResponse>` | ✅ cursor |
| Achievements | `GET /achievements` | `List<AchievementResponse>` (earned + unlockedAt) | — |
| Rank history | `GET /rank-history` | `CursorPage<RankHistoryResponse>` | ✅ cursor |
| Streak | `GET /streak` | `StreakResponse` (current/next milestone, daysToNext) | — |
| Leaderboard (gamification) | `GET /leaderboard?cursor=<base64>&limit=20` | `CursorPage<LeaderboardEntryResponse>` | ✅ cursor |

### Leaderboard — `/api/v1/leaderboard`
| Screen | Endpoint | Returns |
|--------|----------|---------|
| Scoped leaderboard | `GET /?scope=COUNTRY&...` | `List<LeaderboardRowResponse>` |
| My rank | `GET /me?scopeId=<id>` | `MyRankResponse` |

> Two leaderboard surfaces exist: the gamification cursor leaderboard and the dedicated `/leaderboard` (scoped by COUNTRY etc., backed by a snapshot job). Confirm with backend which one is canonical for the main UI; you may use the scoped one for "global/country" and the gamification one for live XP ranking. See `api-contract-reference.md`.

### Cursor pagination format
Cursors are **base64url-encoded JSON** produced by the server. Treat them as opaque strings: read `endCursor` / `hasNextPage` from the response, pass `cursor` back unchanged. Never construct or parse them client-side.

---

## Work breakdown

### Phase 1 — Gamification dashboard (Week 1)
- [ ] `GamificationRepository` (interface + API impl, dio)
- [ ] Dashboard screen: total XP, current tier + icon, progress ring to `nextTier` using `xpToNext`
- [ ] Streak widget: current streak, flame visual, `daysToNext` to next milestone
- [ ] Loading / empty / error states

### Phase 2 — XP history & streak detail (Week 2)
- [ ] XP history list with cursor infinite scroll (`GET /xp`)
- [ ] Transaction row: amount (+/-), sourceType, runningBalance, createdAt
- [ ] Streak detail screen: current vs highest, milestone ladder (current + next milestone names/icons)

### Phase 3 — Achievements & rank history (Week 3)
- [ ] Achievements gallery: earned vs locked, icon, name, description, unlockedAt
- [ ] Lock/unlock visual treatment; "X of N unlocked" header
- [ ] Rank history timeline (`GET /rank-history`, cursor): oldRank → newRank, scope, changedAt

### Phase 4 — Leaderboard (Week 4)
- [ ] Leaderboard screen with scope tabs (e.g. Country / Global) → `/leaderboard?scope=`
- [ ] Rows: rank, avatar, displayName, totalXp, tier icon
- [ ] "My rank" sticky banner (`/leaderboard/me`)
- [ ] Live XP leaderboard via gamification cursor endpoint (infinite scroll)

### Phase 5 — Delight (Week 5)
- [ ] XP-gain animation (e.g. when returning to dashboard after activity)
- [ ] Tier-up / level-up celebration (confetti, badge reveal)
- [ ] Streak-milestone celebration
- [ ] Achievement-unlocked toast/modal

> The backend emits `XpGainedEvent`, `RankChangedEvent`, `StreakUpdatedEvent`, `AchievementUnlockedEvent` server-side, but there's **no push/websocket** to the client yet. For now, refresh gamification state when the user navigates back to these screens (or after a learning/social action completes). If real-time matters later, raise a backend ticket for SSE/websocket.

---

## Cross-feature dependency
Gamification state changes when users complete actions (lessons, posts). Expose a simple `refreshGamification()` on your controller so Dev D (learning) and Dev B (social) can trigger a refresh after a completion. Don't let them reach into your internals — give them one method.

---

## Definition of Done (your lane)
Standard DoD (`README.md` §8) plus:
- [ ] Cursors treated as opaque; pagination verified at boundaries
- [ ] Animations respect "reduce motion" accessibility setting
- [ ] Tier/streak/achievement icons handle missing/null `iconUrl` gracefully
