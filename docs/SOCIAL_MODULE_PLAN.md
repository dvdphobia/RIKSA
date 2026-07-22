# Social Module — GraphQL Implementation Plan

## Status: Empty Package — DB Tables Already Done

| Layer | Status |
|-------|--------|
| DB tables (`posts`, `comments`, `reactions`, `shares`, `user_category_memberships`, `categories`) | ✅ V1 migration |
| DB enums (`ReactionType`: UPVOTE/DOWNVOTE/HAHA/LOVE, `SharePlatform`: TELEGRAM/FACEBOOK/COPY_LINK) | ✅ Done |
| `User` entity (id, username, displayName, avatarUrl, bio, role) | ✅ Done in `auth` package |
| `JwtService` for auth extraction | ✅ Done in `auth/security` |
| Social Java package (`com.riksa.social`) | ❌ Empty (`.gitkeep` only) |
| GraphQL deps in `pom.xml` | ❌ Not added yet |

---

## Feature Scope: What's In / What's Out

This is a Reddit-style community app with these constraints:

| Feature | In scope? | Notes |
|---------|:---:|---|
| Communities (categories) | ✅ | Dev-seeded only. Users join/leave. |
| Posts (title + content + media) | ✅ | STEM-filtered. Creator owns the post. |
| Comments (top-level + nested replies) | ✅ | Threading via `parent_id`. |
| Reactions (UPVOTE/DOWNVOTE/HAHA/LOVE) | ✅ | One reaction per user per target. Upsert. |
| Feed (cursor infinite scroll) | ✅ | Two sort modes: NEW and HOT. |
| Search posts | ✅ | ILIKE on title + content, per community. |
| Share (Telegram/Facebook/Copy Link) | ✅ | Increments `share_count` on post. |
| User profile (posts + comments) | ✅ | `userPosts` and `userComments` queries. |
| Pin/unpin posts | ✅ | MODERATOR or ADMIN only. |
| User-created communities | ❌ | Dev team only. No mutation exposed. |
| Cross-community "r/all" feed | ❌ | `categoryId` is required on feed. |
| Bookmarking / saving posts | ❌ | No DB table. Add later if needed. |
| Report / flag content | ❌ | No DB table. Add later if needed. |
| Direct messaging | ❌ | Separate module (chat package). |
| Image upload | ❌ | `mediaUrl` is a URL string. Upload service is separate. |
| Notifications | ❌ | `PostCreatedEvent` fires; listeners in separate module. |

---

## Product Rules

- **Communities (categories) are created by the dev team only.** The DB is seeded by you. No user-facing `createCategory`/`deleteCategory` mutations exist on the API. Users can only **join** or **leave** existing communities.
- **The feed is always scoped to one community.** The user picks a community in Flutter, then the feed loads posts for that community only. There is no "all communities" feed.
- **STEM-only content.** Every post and comment is validated against a Khmer/English STEM keyword list. No general chat.

---

## Why GraphQL for Social (Not REST)

Flutter needs a post with author profile, comments, reactions, and share counts in one request. REST would need 3-4 round-trips. GraphQL lets the client declare exactly the shape it needs:

```graphql
# Flutter sends ONE request and gets everything to render a post card:
query PostCard($id: UUID!) {
  post(id: $id) {
    title
    content
    reactionCount
    commentCount
    author { displayName avatarUrl }
    myReaction
    comments(limit: 3) {
      edges { content author { displayName } }
      pageInfo { hasNextPage }
    }
  }
}
```

On slow Cambodian networks, fewer round-trips = faster UI.

---

## Step 1 — Add GraphQL to `pom.xml`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-graphql</artifactId>
</dependency>
```

Spring Boot 4.0.6 auto-configures everything. Schema files go in `src/main/resources/graphql/`. No extra config class needed.

---

## Step 2 — Enums (`com.riksa.social.enums/`)

### `ReactionType.java`
```java
package com.riksa.social.enums;

public enum ReactionType {
    UPVOTE, DOWNVOTE, HAHA, LOVE
}
```

### `SharePlatform.java`
```java
package com.riksa.social.enums;

public enum SharePlatform {
    TELEGRAM, FACEBOOK, COPY_LINK
}
```

---

## Step 3 — Entity Classes (`com.riksa.social.entity/`)

Follow the existing convention: `@Getter @Setter @NoArgsConstructor`, `@CreationTimestamp` on `createdAt`. Map one entity per DB table. Hibernate handles the `uuid_generate_v4()` default.

### `Category.java`
| Field | Type | DB Column |
|-------|------|-----------|
| id | UUID (PK, auto-gen) | id |
| name | String | name |
| slug | String | slug |
| description | String | description |
| iconUrl | String | icon_url |
| createdAt | OffsetDateTime | created_at |
| updatedAt | OffsetDateTime | updated_at |

### `Post.java`
| Field | Type | DB Column |
|-------|------|-----------|
| id | UUID (PK, auto-gen) | id |
| title | String | title |
| content | String | content |
| mediaUrl | String | media_url |
| reactionCount | int (default 0) | reaction_count |
| commentCount | int (default 0) | comment_count |
| shareCount | int (default 0) | share_count |
| isPinned | boolean (default false) | is_pinned |
| authorId | UUID (FK→users) | author_id |
| categoryId | UUID (FK→categories) | category_id |
| createdAt | OffsetDateTime | created_at |
| updatedAt | OffsetDateTime | updated_at |

**Note:** `isPinned` column must be added to the posts table via a new Flyway migration (`V8__add_post_pinned.sql`):
```sql
ALTER TABLE "posts" ADD COLUMN is_pinned BOOLEAN NOT NULL DEFAULT false;
```

### `Comment.java`
| Field | Type | DB Column |
|-------|------|-----------|
| id | UUID (PK, auto-gen) | id |
| content | String | content |
| authorId | UUID (FK→users) | author_id |
| postId | UUID (FK→posts) | post_id |
| parentId | UUID (FK→comments, nullable) | parent_id |
| createdAt | OffsetDateTime | created_at |
| updatedAt | OffsetDateTime | updated_at |

### `Reaction.java`
| Field | Type | DB Column |
|-------|------|-----------|
| id | UUID (PK, auto-gen) | id |
| type | ReactionType (enum) | type |
| userId | UUID (FK→users) | user_id |
| postId | UUID (FK→posts, nullable) | post_id |
| commentId | UUID (FK→comments, nullable) | comment_id |
| createdAt | OffsetDateTime | created_at |

Unique constraint: `(user_id, post_id, comment_id)` — already on the DB, also add `@Table(uniqueConstraints=...)` on the entity.

### `Share.java`
| Field | Type | DB Column |
|-------|------|-----------|
| id | UUID (PK, auto-gen) | id |
| platform | SharePlatform (enum) | platform |
| userId | UUID (FK→users) | user_id |
| postId | UUID (FK→posts) | post_id |
| createdAt | OffsetDateTime | created_at |

### `UserCategoryMembership.java`
| Field | Type | DB Column |
|-------|------|-----------|
| id | UUID (PK, auto-gen) | id |
| userId | UUID (FK→users) | user_id |
| categoryId | UUID (FK→categories) | category_id |
| joinedAt | OffsetDateTime | joined_at |

Unique constraint: `(user_id, category_id)`.

---

## Step 4 — Repositories (`com.riksa.social.repository/`)

All extend `JpaRepository`. Only non-trivial query methods are listed below.

### `PostRepository.java`
```java
// Feed: pinned posts first, then by chosen sort order
// NEW sort  → cursor on (createdAt, id)
// HOT sort  → cursor on (reactionCount, id)  — votes + reactions = post popularity

@Query("""
    SELECT p FROM Post p
    WHERE p.categoryId = :categoryId
      AND (:cursorCreatedAt IS NULL OR p.createdAt < :cursorCreatedAt
           OR (p.createdAt = :cursorCreatedAt AND p.id < :cursorId))
    ORDER BY p.isPinned DESC, p.createdAt DESC, p.id DESC
""")
List<Post> findFeedNew(@Param("categoryId") UUID categoryId,
                       @Param("cursorCreatedAt") OffsetDateTime cursorCreatedAt,
                       @Param("cursorId") UUID cursorId,
                       Pageable pageable);

@Query("""
    SELECT p FROM Post p
    WHERE p.categoryId = :categoryId
      AND p.createdAt >= :since  -- only posts from last N days for "hot"
      AND (:cursorReactions IS NULL OR p.reactionCount < :cursorReactions
           OR (p.reactionCount = :cursorReactions AND p.id < :cursorId))
    ORDER BY p.isPinned DESC, p.reactionCount DESC, p.id DESC
""")
List<Post> findFeedHot(@Param("categoryId") UUID categoryId,
                       @Param("since") OffsetDateTime since,
                       @Param("cursorReactions") Integer cursorReactions,
                       @Param("cursorId") UUID cursorId,
                       Pageable pageable);

// Search posts by title/content within a category (ILIKE for case-insensitive)
@Query("""
    SELECT p FROM Post p
    WHERE p.categoryId = :categoryId
      AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%')))
      AND (:cursorCreatedAt IS NULL OR p.createdAt < :cursorCreatedAt
           OR (p.createdAt = :cursorCreatedAt AND p.id < :cursorId))
    ORDER BY p.createdAt DESC, p.id DESC
""")
List<Post> searchPosts(@Param("categoryId") UUID categoryId,
                       @Param("query") String query,
                       @Param("cursorCreatedAt") OffsetDateTime cursorCreatedAt,
                       @Param("cursorId") UUID cursorId,
                       Pageable pageable);

// User's own posts (for profile screen)
@Query("""
    SELECT p FROM Post p
    WHERE p.authorId = :userId
      AND (:cursorCreatedAt IS NULL OR p.createdAt < :cursorCreatedAt
           OR (p.createdAt = :cursorCreatedAt AND p.id < :cursorId))
    ORDER BY p.createdAt DESC, p.id DESC
""")
List<Post> findByAuthorId(@Param("userId") UUID userId,
                          @Param("cursorCreatedAt") OffsetDateTime cursorCreatedAt,
                          @Param("cursorId") UUID cursorId,
                          Pageable pageable);
```
**Cursor design:** Each cursor is a base64-encoded JSON string with two keys (e.g. `{"createdAt":"2026-06-08T10:00:00Z","id":"abc123"}`). The WHERE clause uses the two-column approach: `(createdAt < cursorCreatedAt) OR (createdAt = cursorCreatedAt AND id < cursorId)`. This handles ties when two posts have the same timestamp or same reaction count.


### `CommentRepository.java`
```java
// Top-level comments on a post
List<Comment> findByPostIdAndParentIdIsNullOrderByCreatedAtAsc(UUID postId, Pageable pageable);

// Replies to a specific comment
List<Comment> findByParentIdOrderByCreatedAtAsc(UUID parentId, Pageable pageable);

int countByPostId(UUID postId);
```

### `ReactionRepository.java`
```java
Optional<Reaction> findByUserIdAndPostIdAndCommentId(UUID userId, UUID postId, UUID commentId);

boolean existsByUserIdAndPostIdAndType(UUID userId, UUID postId, ReactionType type);
```
**Reaction upsert logic:** The DB has `UNIQUE(user_id, post_id, comment_id)`. When a user reacts, try to `save()`. If `DataIntegrityViolationException` is caught, the reaction already exists — load it and change the type instead. Or use a native `INSERT ... ON CONFLICT DO UPDATE` query.

### `CategoryRepository.java`
```java
List<Category> findAllByOrderByNameAsc();
```

### `UserCategoryMembershipRepository.java`
```java
boolean existsByUserIdAndCategoryId(UUID userId, UUID categoryId);
void deleteByUserIdAndCategoryId(UUID userId, UUID categoryId);
int countByCategoryId(UUID categoryId);
List<UserCategoryMembership> findByUserId(UUID userId);
```

### `ShareRepository.java`
```java
int countByPostId(UUID postId);  // not strictly needed since counter is on Post entity
```

---

## Step 5 — Services (`com.riksa.social.service/`)

### `ContentFilterService.java`
Hold a `Set<String>` of STEM-relevant Khmer/English keywords (programming, math, science, engineering terms). Before saving any post or comment, check if content contains at least one keyword. Throw `IllegalArgumentException("Content must be STEM-related")` if not.

```java
@Service
public class ContentFilterService {
    private static final Set<String> STEM_KEYWORDS = Set.of(
        "math", "physics", "chemistry", "biology", "code", "programming",
        "python", "java", "algorithm", "engineering", "science",
        "គណិត", "រូបវិទ្យា", "គីមី", "ជីវវិទ្យា", "កូដ"
        // expand as needed
    );

    public void validate(String content) {
        String lower = content.toLowerCase();
        boolean hasKeyword = STEM_KEYWORDS.stream().anyMatch(lower::contains);
        if (!hasKeyword) {
            throw new IllegalArgumentException("Content must be STEM-related");
        }
    }
}
```

### `CategoryService.java`
```java
// listCategories(userId) → build CategoryResponse list with memberCount + joined
// joinCategory(userId, categoryId) → insert membership
// leaveCategory(userId, categoryId) → delete membership
```

### `PostService.java`
```java
// createPost(userId, input)    → validate with ContentFilterService, save, fire PostCreatedEvent, return Post
// updatePost(userId, input)    → check ownership, update, return Post
// deletePost(userId, postId)   → check ownership, delete (cascade removes comments/reactions/shares)
// getFeed(categoryId, sort, cursor, limit) → call PostRepository.findFeedNew/findFeedHot, build FeedPage
// searchPosts(categoryId, query, cursor, limit) → call PostRepository.searchPosts()
// getUserPosts(userId, cursor, limit) → call PostRepository.findByAuthorId()
// getPost(postId)              → find by id, throw 404 if not found
// pinPost(moderatorId, postId) → set isPinned=true (requires MODERATOR or ADMIN role)
// unpinPost(moderatorId, postId) → set isPinned=false
```

### `CommentService.java`
```java
// createComment(userId, input)    → validate, save, increment post.commentCount, return Comment
// updateComment(userId, input)    → check ownership, update
// deleteComment(userId, commentId) → check ownership, delete cascade, decrement post.commentCount
// getComments(postId, cursor, limit) → top-level comments with cursor pagination
// getReplies(commentId, cursor, limit) → nested replies
// getUserComments(userId, cursor, limit) → comments made by a user (for profile screen)
```

### `ReactionService.java`
```java
// react(userId, postId, commentId, type)  → upsert, adjust post counter, return full Post
// removeReaction(userId, postId, commentId) → delete, adjust post counter, return full Post
```
**Counter logic:** When reacting to a post, increment `post.reactionCount`. When removing, decrement. When changing from UPVOTE to LOVE, the count stays the same (just type changes). The `removeReaction` mutation always decrements. Run all this inside `@Transactional` so counters never drift.

### `FeedService.java`
```java
// buildFeed(categoryId, sort, cursorStr, limit, userId) →
//   1. Decode cursor: base64 → JSON → {createdAt/reactionCount, id}
//   2. Fetch limit+1 rows from PostRepository.findFeedNew() or findFeedHot() depending on sort
//   3. If list.size() > limit: hasNextPage=true, endCursor=encodeCursor(lastPost), trim to limit
//   4. For each post, resolve author (UserRepository.findById batch), category, myReaction
//   5. Return FeedPage with edges + pageInfo
//
// buildSearchResults(categoryId, query, cursor, limit, userId) →
//   Same as feed but uses PostRepository.searchPosts()
//
// buildUserFeed(userId, cursor, limit, currentUserId) →
//   Same as feed but uses PostRepository.findByAuthorId(), no categoryId filter
```

---

## Step 6 — GraphQL Controllers / Data Fetchers (`com.riksa.social.controller/`)

These use `@Controller` (NOT `@RestController`). Spring GraphQL auto-wires methods annotated with `@QueryMapping` and `@MutationMapping`.

### `FeedController.java`
```java
@Controller
@RequiredArgsConstructor
public class FeedController {
    private final FeedService feedService;

    @QueryMapping("feed")
    public FeedPage feed(@Argument UUID categoryId,
                         @Argument FeedSort sort,
                         @Argument String cursor,
                         @Argument int limit) {
        UUID userId = getCurrentUserId();
        limit = limit > 0 ? Math.min(limit, 30) : 20;
        return feedService.getFeed(categoryId, sort, cursor, limit, userId);
    }

    @QueryMapping("searchPosts")
    public FeedPage searchPosts(@Argument UUID categoryId,
                                @Argument String query,
                                @Argument String cursor,
                                @Argument int limit) {
        UUID userId = getCurrentUserId();
        limit = limit > 0 ? Math.min(limit, 30) : 20;
        return feedService.searchPosts(categoryId, query, cursor, limit, userId);
    }

    @QueryMapping("userPosts")
    public FeedPage userPosts(@Argument UUID userId,
                              @Argument String cursor,
                              @Argument int limit) {
        UUID currentUserId = getCurrentUserId();
        limit = limit > 0 ? Math.min(limit, 30) : 20;
        return feedService.getUserPosts(userId, cursor, limit, currentUserId);
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(auth.getName());
    }
}
```

### `FeedPage.java` (response DTO)
```java
public record FeedPage(List<PostResponse> edges, PageInfo pageInfo) {}
public record PageInfo(boolean hasNextPage, UUID endCursor) {}
```

### `PostController.java`
```java
@Controller
@RequiredArgsConstructor
public class PostController {
    @QueryMapping("post")
    public PostResponse post(@Argument UUID id) { ... }

    @MutationMapping("createPost")
    public PostResponse createPost(@Argument CreatePostInput input) { ... }

    @MutationMapping("updatePost")
    public PostResponse updatePost(@Argument UpdatePostInput input) { ... }

    @MutationMapping("deletePost")
    public boolean deletePost(@Argument UUID id) { ... }

    @MutationMapping("pinPost")
    public PostResponse pinPost(@Argument UUID id) { ... }     // MOD/ADMIN only

    @MutationMapping("unpinPost")
    public PostResponse unpinPost(@Argument UUID id) { ... }   // MOD/ADMIN only
}
```

### `CommentController.java`
```java
@Controller
@RequiredArgsConstructor
public class CommentController {
    @MutationMapping("createComment")
    public CommentResponse createComment(@Argument CreateCommentInput input) { ... }

    @MutationMapping("updateComment")
    public CommentResponse updateComment(@Argument UpdateCommentInput input) { ... }

    @MutationMapping("deleteComment")
    public boolean deleteComment(@Argument UUID id) { ... }

    @QueryMapping("userComments")
    public CommentPage userComments(@Argument UUID userId,
                                    @Argument String cursor,
                                    @Argument int limit) { ... }
}
```

### `ReactionController.java`
```java
@Controller
@RequiredArgsConstructor
public class ReactionController {
    @MutationMapping("react")
    public PostResponse react(@Argument ReactInput input) { ... }

    @MutationMapping("removeReaction")
    public PostResponse removeReaction(@Argument UUID postId, @Argument UUID commentId) { ... }
}
```

### `CategoryController.java`
```java
@Controller
@RequiredArgsConstructor
public class CategoryController {
    @QueryMapping("categories")
    public List<CategoryResponse> categories() { ... }

    @MutationMapping("joinCategory")
    public CategoryResponse joinCategory(@Argument UUID categoryId) { ... }

    @MutationMapping("leaveCategory")
    public CategoryResponse leaveCategory(@Argument UUID categoryId) { ... }

    // ⚠️ NO createCategory or deleteCategory — categories are seeded by dev team only.
    // Admin dashboard mutates the DB directly via SQL or a separate admin REST endpoint.
}
```

---

## Step 7 — GraphQL Schema File

Create: `src/main/resources/graphql/social.graphqls`

```graphql
# ══════════════════════════════════════════════════════
# Social Module — GraphQL Schema
# ══════════════════════════════════════════════════════

scalar DateTime
scalar UUID

# ── Types ────────────────────────────────────────────
type Category {
  id:          UUID!
  name:        String!
  slug:        String!
  description: String
  iconUrl:     String
  memberCount: Int!
  joined:      Boolean!
  # ⚠️ Categories are created by dev team (DB seed/migration). No user-facing create/delete mutation.
}

type User {
  id:          UUID!
  username:    String!
  displayName: String!
  avatarUrl:   String!
}

type Post {
  id:            UUID!
  title:         String!
  content:        String!
  mediaUrl:      String
  reactionCount:  Int!
  commentCount:   Int!
  shareCount:     Int!
  isPinned:      Boolean!
  author:        User!
  category:      Category!
  myReaction:    ReactionType
  createdAt:     DateTime!
  updatedAt:     DateTime!
  comments(cursor: String, limit: Int = 10): CommentPage!
}

type Comment {
  id:         UUID!
  content:     String!
  author:     User!
  postId:     UUID!
  parentId:   UUID
  myReaction: ReactionType
  createdAt:  DateTime!
  updatedAt:  DateTime!
  replies(cursor: String, limit: Int = 5): CommentPage!
}

# ── Enums ────────────────────────────────────────────
enum ReactionType { UPVOTE DOWNVOTE HAHA LOVE }
enum SharePlatform { TELEGRAM FACEBOOK COPY_LINK }
enum FeedSort { NEW HOT }

# ── Pagination ───────────────────────────────────────
type FeedPage {
  edges:    [Post!]!
  pageInfo: PageInfo!
}

type CommentPage {
  edges:    [Comment!]!
  pageInfo: PageInfo!
}

type PageInfo {
  hasNextPage: Boolean!
  endCursor:   String    # base64-encoded JSON cursor: {"createdAt":"...","id":"..."}
}

# ── Inputs ───────────────────────────────────────────
input CreatePostInput {
  title:      String!
  content:    String!
  mediaUrl:   String
  categoryId: UUID!
}

input UpdatePostInput {
  id:      UUID!
  title:   String
  content: String
}

input CreateCommentInput {
  postId:   UUID!
  content:  String!
  parentId: UUID
}

input UpdateCommentInput {
  id:      UUID!
  content: String!
}

input ReactInput {
  postId:    UUID
  commentId: UUID
  type:      ReactionType!
}

input SharePostInput {
  postId:   UUID!
  platform: SharePlatform
}

# ── Query ────────────────────────────────────────────
type Query {
  # Feed: posts in ONE community (categoryId required). Sort by NEW or HOT.
  feed(categoryId: UUID!, sort: FeedSort = NEW, cursor: String, limit: Int = 20): FeedPage!

  # Search posts within a community (ILIKE on title + content)
  searchPosts(categoryId: UUID!, query: String!, cursor: String, limit: Int = 20): FeedPage!

  # Single post with full nested comments
  post(id: UUID!): Post

  # All communities the user can browse/join
  categories: [Category!]!

  # Comments on a post (used by Flutter's comment sheet)
  comments(postId: UUID!, cursor: String, limit: Int = 20): CommentPage!

  # User profile: get a user's posts
  userPosts(userId: UUID!, cursor: String, limit: Int = 20): FeedPage!

  # User profile: get a user's comments
  userComments(userId: UUID!, cursor: String, limit: Int = 20): CommentPage!
}

# ── Mutation ─────────────────────────────────────────
type Mutation {
  createPost(input: CreatePostInput!): Post!
  updatePost(input: UpdatePostInput!): Post!
  deletePost(id: UUID!): Boolean!

  pinPost(id: UUID!): Post!       # MODERATOR or ADMIN role only
  unpinPost(id: UUID!): Post!     # MODERATOR or ADMIN role only

  createComment(input: CreateCommentInput!): Comment!
  updateComment(input: UpdateCommentInput!): Comment!
  deleteComment(id: UUID!): Boolean!

  react(input: ReactInput!): Post!
  removeReaction(postId: UUID, commentId: UUID): Post!

  sharePost(input: SharePostInput!): Post!

  joinCategory(categoryId: UUID!): Category!
  leaveCategory(categoryId: UUID!): Category!
}
```

---

## Step 8 — Event (`com.riksa.social.event/`)

### `PostCreatedEvent.java`
Simple Spring event record. Other modules (gamification, notifications) can listen to it later.

```java
package com.riksa.social.event;

import java.util.UUID;

public record PostCreatedEvent(UUID postId, UUID authorId, UUID categoryId) {}
```

Fire it in `PostService.createPost()`:
```java
applicationEventPublisher.publishEvent(new PostCreatedEvent(post.getId(), userId, post.getCategoryId()));
```

---

## Build Order (Execute Top to Bottom)

```
1.  pom.xml                                                — add spring-boot-starter-graphql
2.  db/migration/V8__add_post_pinned.sql                    — ALTER TABLE posts ADD is_pinned
3.  social/enums/ReactionType.java                          — 1 file
4.  social/enums/SharePlatform.java                         — 1 file
5.  social/enums/FeedSort.java                              — 1 file
6.  social/entity/Category.java                             — 1 file
7.  social/entity/Post.java                                 — 1 file
8.  social/entity/Comment.java                              — 1 file
9.  social/entity/Reaction.java                             — 1 file
10. social/entity/Share.java                                — 1 file
11. social/entity/UserCategoryMembership.java                — 1 file
12. social/repository/CategoryRepository.java                — 1 file
13. social/repository/PostRepository.java                    — 1 file
14. social/repository/CommentRepository.java                 — 1 file
15. social/repository/ReactionRepository.java                — 1 file
16. social/repository/ShareRepository.java                   — 1 file
17. social/repository/UserCategoryMembershipRepository.java  — 1 file
18. social/service/ContentFilterService.java                 — 1 file
19. social/service/CategoryService.java                      — 1 file
20. social/service/PostService.java                          — 1 file
21. social/service/CommentService.java                       — 1 file
22. social/service/ReactionService.java                      — 1 file
23. social/service/FeedService.java                          — 1 file (handles sort/search/userFeed)
24. social/controller/CategoryController.java                — 1 file
25. social/controller/FeedController.java                    — 1 file
26. social/controller/PostController.java                    — 1 file
27. social/controller/CommentController.java                 — 1 file
28. social/controller/ReactionController.java                — 1 file
29. social/event/PostCreatedEvent.java                       — 1 file
30. src/main/resources/graphql/social.graphqls               — 1 file
```

**Total: ~30 files**

---

## Key Design Decisions

1. **Community-scoped feed (required `categoryId`)** — there is no cross-community "r/all" feed. The user must pick a community first. This keeps each community focused on its STEM topic and prevents a noisy undifferentiated feed.

2. **Categories are dev-team only** — seeded via Flyway migration or admin SQL. Users can join/leave but never create, rename, or delete communities. This prevents category spam and keeps the community taxonomy clean.

3. **Cursor pagination with composite keys** — cursors are base64-encoded JSON (e.g. `{"createdAt":"2026-06-08T10:00:00Z","id":"abc"}`). The WHERE clause uses `(createdAt < cursorCreatedAt) OR (createdAt = cursorCreatedAt AND id < cursorId)` to handle ties correctly. Always fetch `limit + 1`; the extra row tells you `hasNextPage` and provides `endCursor`.

4. **Two sort modes** — NEW (cursor on `createdAt, id`) for the default chronological view. HOT (cursor on `reactionCount, id`) for popular posts within last 7 days. Pinned posts always sort first regardless of mode.

5. **Counter denormalization** — `posts.reaction_count`, `comment_count`, `share_count` live on the Post entity. Always increment/decrement inside `@Transactional` with the insert/delete. Never recompute from scratch at read time.

6. **Reaction upsert via UNIQUE constraint** — `reactions` has `UNIQUE(user_id, post_id, comment_id)`. First attempt `save()`. Catch `DataIntegrityViolationException` → load existing row → update type. Run inside `@Transactional`.

7. **N+1 on nested comments** — the `comments` field on Post resolves top-level comments. Each comment's `replies` field calls the DB again. For typical shallow threads (depth ≤ 3), this is fine. If threads get deep, add a `@BatchMapping` DataLoader later.

8. **Same JWT auth** — reuse `JwtService` from `com.riksa.auth.security`. In GraphQL, get user from `SecurityContextHolder.getContext().getAuthentication()`. No new auth model needed.

9. **STEM content filter** — `ContentFilterService.validate()` is called before any `Post` or `Comment` save. Throws if no STEM keyword found. This prevents the social feed from becoming a general chat room.

10. **Pin/unpin is MOD/ADMIN only** — check `user.role == MODERATOR || user.role == ADMIN` before calling `setPinned(true)`.

---

## Flutter Usage Notes

- **GraphQL endpoint:** `POST /api/graphql` (single endpoint for all queries/mutations)
- **Auth header:** Same `Authorization: Bearer <jwt>` as REST endpoints
- **Community flow:** Flutter calls `categories` to show community list → user taps one → call `feed(categoryId: "abc", sort: NEW, limit: 20)` → scroll down → use `pageInfo.endCursor` as `cursor` for next page. Stop when `hasNextPage == false`.
- **Sort toggle:** Flutter sends `sort: NEW` or `sort: HOT`. Cursor is opaque — Flutter treats it as a black box string, never parses it.
- **Search flow:** User types in Flutter search bar → debounce 300ms → call `searchPosts(categoryId: "abc", query: "python", limit: 20)`. Cursor pagination works the same as feed.
- **Profile flow:** Tap a user avatar → call `userPosts(userId: "...")` and `userComments(userId: "...")` to populate the profile screen.
- **Reaction optimistic update:** Flutter should update the reaction button immediately, then fire `react` mutation. If the server returns a different count, reconcile.
# Deprecated design plan

> This document is retained for historical context and may not match the current implementation. Use [PROJECT_GUIDE.md](PROJECT_GUIDE.md) as the source of truth.
