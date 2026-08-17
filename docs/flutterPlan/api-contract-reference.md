# API Contract Reference (for the Flutter team)

> Source of truth for endpoints each lane consumes. Derived from the backend code.
> Base URL (local): `http://localhost:8080` · REST: `/api/v1/*` · GraphQL: `/graphql`
> All endpoints except `/api/v1/auth/**` and `/actuator/health` require `Authorization: Bearer <accessToken>`.

---

## Conventions

- **Auth header:** `Authorization: Bearer <accessToken>` on every protected call.
- **Token lifetimes:** access = 15 min, refresh = 30 days. `POST /auth/refresh` returns a *new pair* — persist both.
- **Cursor pagination (REST):** responses are `CursorPage<T> { edges: [T], hasNextPage: bool, endCursor: string }`. The cursor is an **opaque base64url string** — pass `endCursor` back as `?cursor=`; never build/parse it client-side.
- **Cursor pagination (GraphQL):** `cursor: UUID` argument + `pageInfo { hasNextPage, endCursor }`.
- **IDs:** UUID strings.
- **Timestamps:** ISO-8601 with offset (e.g. `2026-05-31T10:00:00+07:00`).

---

## AUTH — `/api/v1/auth` (Dev A) — REST

> Detailed audited contract: `docs/AUTH_API.md`. The two `/me` operations require
> a Bearer access token; the other auth operations are public.

| Method | Path | Body | Response |
|--------|------|------|----------|
| POST | `/register` | `{username, displayName, email, password, phoneNumber?, gender?, provinceId?}` | `AuthResponse` |
| POST | `/login` | `{username, password}` | `AuthResponse` |
| POST | `/refresh` | `{refreshToken}` | `AuthResponse` (new pair) |
| POST | `/logout` | `{refreshToken}` | `{message}` |
| POST | `/google` | `{idToken}` (Google) | `AuthResponse` |
| POST | `/otp/send` | `{identifier}` (Telegram chat ID) | `{message, channel: "TELEGRAM"}` |
| POST | `/otp/verify` | `{identifier, code}` | `{verified, message}` |
| POST | `/forgot-password` | `{identifier}` | `{message}` |
| POST | `/reset-password` | `{identifier, code, newPassword}` | `{message}` |
| POST | `/verify-email` | `{token}` | `{verified, ...}` |
| GET | `/me` | — (Bearer) | `UserProfileResponse` |
| PUT | `/me` | `{displayName?, bio?, gender?, provinceId?, avatarUrl?}` (Bearer) | `UserProfileResponse` |

**`AuthResponse`:** `{userId, accessToken, refreshToken, tokenType: "Bearer", username, role}`
**`UserProfileResponse`:** `{id, username, displayName, email, emailVerified, avatarUrl, bio, gender, phoneNumber, phoneVerified, role, countryId, createdAt}`. It currently omits province and `updatedAt`.

For `PUT /me`, `avatarUrl` is a preset asset path only: choose `/avatars/avatar-1.png` through `/avatars/avatar-30.png`. External URLs and other paths are rejected with `400 Bad Request`.

> OTP is Telegram-only: `identifier` is the user's Telegram chat ID and they
> must `/start` the bot first. The code is not returned in the response.

---

## GAMIFICATION — `/api/v1/gamification` (Dev C) — REST, Bearer

| Method | Path | Query | Response |
|--------|------|-------|----------|
| GET | `/me` | — | `GamificationProfileResponse` |
| GET | `/xp` | `?cursor=&limit=20` | `CursorPage<XpTransactionResponse>` |
| GET | `/achievements` | — | `List<AchievementResponse>` |
| GET | `/rank-history` | `?cursor=&limit=` | `CursorPage<RankHistoryResponse>` |
| GET | `/streak` | — | `StreakResponse` |
| GET | `/leaderboard` | `?cursor=&limit=20` | `CursorPage<LeaderboardEntryResponse>` |

- **`GamificationProfileResponse`:** `{userId, totalXp, currentStreak, highestStreak, currentTier, currentTierIcon, nextTier, xpToNext}`
- **`XpTransactionResponse`:** `{id, amount, sourceType, sourceId, runningBalance, createdAt}`
- **`AchievementResponse`:** `{id, name, description, iconUrl, earned, unlockedAt}`
- **`RankHistoryResponse`:** `{id, scope, oldRank, newRank, changedAt}`
- **`StreakResponse`:** `{currentStreak, highestStreak, currentMilestoneName, currentMilestoneIcon, nextMilestoneName, nextMilestoneMinStreak, daysToNext}`
- **`LeaderboardEntryResponse`:** `{rank, userId, displayName, avatarUrl, totalXp, tierName, tierIcon}`

---

## LEADERBOARD — `/api/v1/leaderboard` (Dev C) — REST, Bearer

| Method | Path | Query | Response |
|--------|------|-------|----------|
| GET | `/` | `?scope=COUNTRY&...` | `List<LeaderboardRowResponse>` |
| GET | `/me` | `?scopeId=` | `MyRankResponse` |

> `scope` defaults to `COUNTRY`. This surface is backed by a snapshot batch job. Confirm with backend whether this or the gamification `/leaderboard` is canonical for the main UI.

---

## BLOG — `/api/v1/blog` (Dev D) — REST, Bearer

| Method | Path | Notes | Response |
|--------|------|-------|----------|
| GET | `/` | `?status=APPROVED&...` | `BlogListResponse` |
| POST | `/` | `BlogPostRequest` {title, content, coverUrl} | `BlogPostResponse` |
| GET | `/{id}` | — | `BlogPostResponse` |
| POST | `/{id}/like` | toggle | `BlogPostResponse` |
| POST | `/{id}/view` | track view (auth optional) | `204` |
| PATCH | `/admin/{id}/status` | **MODERATOR role only** | `BlogPostResponse` |

`status` values include `APPROVED` (and others for moderation queue). Gate the moderation call on `role == MODERATOR` from `AuthResponse`/profile.

---

## SOCIAL — GraphQL `/graphql` (Dev B)

Single endpoint. Full schema: `src/main/resources/graphql/social.graphqls`. Keep a copy in the Flutter repo.

### Queries
```graphql
feed(categoryId: UUID, cursor: UUID, limit: Int = 20): FeedPage!
post(id: UUID!): Post
categories: [Category!]!
comments(postId: UUID!, cursor: UUID, limit: Int = 20): CommentPage!   # ❌ no server resolver yet
```

### Mutations
```graphql
createPost(input: CreatePostInput!): Post!         # ✅
updatePost(input: UpdatePostInput!): Post!         # ✅
deletePost(id: UUID!): Boolean!                    # ✅
createComment(input: CreateCommentInput!): Comment! # ✅
updateComment(input: UpdateCommentInput!): Comment! # ✅
deleteComment(id: UUID!): Boolean!                  # ✅
react(input: ReactInput!): Post!                   # ✅
removeReaction(postId: UUID, commentId: UUID): Post! # ✅
sharePost(input: SharePostInput!): Post!           # ❌ no server resolver yet
joinCategory(categoryId: UUID!): Category!         # ✅
leaveCategory(categoryId: UUID!): Category!         # ✅
```

### Types (abridged)
```graphql
type Post {
  id: UUID!  title: String!  content: String!  mediaUrl: String
  reactionCount: Int!  commentCount: Int!  shareCount: Int!
  author: User!            # ❌ server returns null → selecting this ERRORS until fixed
  category: Category!      # ✅
  myReaction: ReactionType # ⚠️ always null until fixed
  createdAt: DateTime!  updatedAt: DateTime!
  comments(cursor: UUID, limit: Int = 10): CommentPage!  # ❌ no field resolver yet
}
type Comment {
  id: UUID!  content: String!  postId: UUID!  parentId: UUID
  author: User!            # ❌ same issue as Post.author
  myReaction: ReactionType # ⚠️ always null
  createdAt: DateTime!  updatedAt: DateTime!
  replies(cursor: UUID, limit: Int = 5): CommentPage!    # ❌ no field resolver yet
}
type Category { id: UUID!  name: String!  slug: String!  description: String  iconUrl: String  memberCount: Int!  joined: Boolean! }
type User { id: UUID!  username: String!  displayName: String!  avatarUrl: String! }
type FeedPage { edges: [Post!]!  pageInfo: PageInfo! }
type CommentPage { edges: [Comment!]!  pageInfo: PageInfo! }
type PageInfo { hasNextPage: Boolean!  endCursor: UUID }

enum ReactionType { UPVOTE DOWNVOTE HAHA LOVE }
enum SharePlatform { TELEGRAM FACEBOOK COPY_LINK }
```

### ⚠️ Social integration rules
- **Do not select `author` or nested `comments`/`replies` in live queries** until the backend adds resolvers — non-null `author: User!` returning null makes the whole query error.
- `comments` query and `sharePost` mutation have **no server resolver** — mock them.
- `myReaction` is always null — treat as "no reaction" for now.
- File one backend ticket covering all of the above and track the fixes.

---

## LEARNING — ❌ not implemented on backend (Dev D)

No endpoints exist. Only a `Lesson` entity (baseXp, difficulty) + `LessonCompletedEvent` (which the gamification pipeline already consumes server-side).

**Proposed contract** to negotiate with the backend team (build Flutter against this as a fake):
```
GET  /api/v1/learning/courses              -> List<CourseResponse>
GET  /api/v1/learning/lessons/{id}         -> LessonResponse
POST /api/v1/learning/lessons/{id}/complete -> {xpAwarded, ...}   # emits LessonCompletedEvent
```
Ship learning behind a feature flag until these land.

---

## Out of scope for v1
- **AI Chat** (`chatAi`) — backend empty.
- **Admin** (`admin`) — backend empty; likely a separate web panel, not the mobile app.
