# Riksa Blog API Documentation (Flutter Mobile)

Base URL: `http://localhost:8080`
All blog endpoints require a valid JWT: `Authorization: Bearer <accessToken>`
Access token lives 15 minutes — refresh via `POST /api/v1/auth/refresh`.

---

## Moderation Lifecycle

```
user submits post ──► PENDING ──► APPROVED   (visible to everyone)
                         │
                         └──────► REJECTED   (visible only to author, with reason)
```

Visibility rules:

- **Regular users** only ever see `APPROVED` posts in the feed and by id.
- **MODERATOR / ADMIN** can list any status via `?status=` and open non-approved posts by id.
- **Authors** see their own posts of any status via `/my-posts` (including `rejectionReason`).
- New posts always start as `PENDING`. Content is checked against a prohibited-word filter
  (400 `Content contains prohibited words` on violation).

---

## Data Types

### BlogStatus

`PENDING` | `APPROVED` | `REJECTED`

### BlogPostResponse

| Field | Type | Notes |
|---|---|---|
| `id` | UUID | |
| `authorId` | UUID | |
| `title` | String | max 200 chars |
| `content` | String | full article text |
| `coverUrl` | String? | max 2000 chars, nullable |
| `status` | BlogStatus | |
| `rejectionReason` | String? | set when REJECTED |
| `viewCount` | int | |
| `likeCount` | int | |
| `categoryId` | UUID | always present |
| `liked` | bool | whether **current user** liked the post |
| `createdAt` | ISO-8601 datetime | |
| `updatedAt` | ISO-8601 datetime | |

### BlogListResponse (paginated lists)

| Field | Type | Notes |
|---|---|---|
| `content` | BlogPostResponse[] | items of the current page |
| `page` | int | 0-based page index |
| `size` | int | page size |
| `totalElements` | long | |
| `totalPages` | int | |

### Error shape (all failures)

```json
{ "status": 400, "error": "Bad Request", "message": "...", "timestamp": "..." }
```

Common codes: `400` validation / prohibited words / not found, `401` missing or expired token,
`429` auth rate limit (5 logins per 60 s).

---

## Endpoints

### 1. List posts (feed)

```
GET /api/v1/blog?status=APPROVED&categoryId=<uuid>&page=0&size=20
```

| Param | Required | Notes |
|---|---|---|
| `status` | No | default `APPROVED`; **ignored for regular users** (always APPROVED) |
| `categoryId` | No | filter by category |
| `page` | No | 0-based, default 0 |
| `size` | No | default 20, clamped 1–100 |

Returns `BlogListResponse` ordered newest first.

### 2. Create post

```
POST /api/v1/blog
```

| Field | Type | Required | Validation |
|---|---|---|---|
| `title` | String | Yes | 1–200 chars |
| `content` | String | Yes | not blank |
| `coverUrl` | String | No | max 2000 chars |
| `categoryId` | UUID | Yes | must exist |

Returns the created `BlogPostResponse` with `status: "PENDING"`.

**400** if a prohibited word appears in title/content, or categoryId is missing/unknown.

### 3. My posts

```
GET /api/v1/blog/my-posts?status=<PENDING|APPROVED|REJECTED>&page=0&size=20
```

`status` optional — omit to get all of the author's posts. Use this for the
"my articles" screen; rejected items include `rejectionReason` for display.

### 4. Get post by id (detail screen)

```
GET /api/v1/blog/{id}
```

Returns `BlogPostResponse`. **Automatically records a view** for the current user.
Non-moderators requesting a non-approved post get `400 Blog post not found`.

### 5. Toggle like

```
POST /api/v1/blog/{id}/like
```

No body. Likes if not liked, unlikes if already liked.
Returns the updated `BlogPostResponse` with fresh `likeCount` and `liked`.

### 6. Track view (optional)

```
POST /api/v1/blog/{id}/view
```

Returns `200` with empty body. **You normally do NOT need this** — endpoint 4 already
counts a view on every detail open. Only use it if you render content without calling 4.

### 7. Categories (for filter chips / create form)

Categories are served over GraphQL, not REST:

```
POST /graphql        (also requires Bearer token)
{
  "query": "{ categories { id name slug description iconUrl memberCount joined } }"
}
```

Returns `{ "data": { "categories": [ ... ] } }`.

---

## Example Flow (curl)

```bash
# login
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"blogger2","password":"Passw0rd!"}' | jq -r .accessToken)

# submit a post -> PENDING
curl -s -X POST http://localhost:8080/api/v1/blog \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"title":"My first post","content":"Hello Riksa!","categoryId":"<categoryId>"}'

# feed (only APPROVED)
curl -s "http://localhost:8080/api/v1/blog?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"

# detail (counts a view)
curl -s http://localhost:8080/api/v1/blog/<postId> -H "Authorization: Bearer $TOKEN"

# like / unlike
curl -s -X POST http://localhost:8080/api/v1/blog/<postId>/like \
  -H "Authorization: Bearer $TOKEN"

# my posts incl. rejected + reason
curl -s "http://localhost:8080/api/v1/blog/my-posts" -H "Authorization: Bearer $TOKEN"
```

---

## Flutter Integration Notes

- Pagination is **0-based**; use `totalPages` / `totalElements` for infinite scroll.
- Like button: call toggle, then replace the cached post with the response
  (it contains the new `likeCount` + `liked`).
- Do not call `/view` when you already open the detail via `GET /{id}` (views would double-count).
- After submitting, show a "pending review" state — the post will NOT appear in the
  feed until a moderator approves it.
- On `REJECTED` items from `/my-posts`, surface `rejectionReason` to the author.
- Auth requests are rate-limited (5/min): cache tokens and refresh instead of re-logging in.
