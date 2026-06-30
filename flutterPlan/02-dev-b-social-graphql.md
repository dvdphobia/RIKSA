# Dev B — Social / Community (GraphQL)

> **Backend status: ⚠️ PARTIAL.** The social module is the only GraphQL part of the API.
> The feed + basic mutations work, but several resolvers are broken or missing on the server.
> **You build UI against mocks first, then integrate as the backend team fixes resolvers.**

---

## Mission

Build the community experience: feed, post detail, create/edit posts, comments & replies, reactions, categories (join/leave), and sharing. This is the social heart of the app.

## Folder you own
- `features/social/`
- Co-own `core/network/graphql_client.dart` with Dev A.

---

## ⚠️ Critical: known backend gaps (coordinate before integrating)

The GraphQL schema (`social.graphqls`) promises more than the server delivers today. Plan around these:

| Schema field / op | Server status | Your workaround |
|-------------------|---------------|-----------------|
| `Query.feed` | ✅ Works | Integrate now |
| `Query.post(id)` | ✅ Works | Integrate now |
| `Query.categories` | ✅ Works | Integrate now |
| `Query.comments(postId,...)` | ❌ No resolver | Mock until backend adds it |
| `Mutation.createPost / updatePost / deletePost` | ✅ Works | Integrate now |
| `Mutation.createComment / updateComment / deleteComment` | ✅ Works | Integrate (but reading them back needs the comments query) |
| `Mutation.react / removeReaction` | ✅ Works | Integrate now |
| `Mutation.sharePost` | ❌ No resolver | Mock; backend must implement |
| `Mutation.joinCategory / leaveCategory` | ✅ Works | Integrate now |
| `Post.author` / `Comment.author` | ❌ Always null, but schema marks `User!` non-null → **query errors** | Don't select `author` until fixed; mock it |
| `Post.comments` / `Comment.replies` (nested) | ❌ No field resolver | Mock nested loading |
| `Post.myReaction` / `Comment.myReaction` | ⚠️ Always null | Treat as "no reaction" until fixed |

**Action:** File a backend ticket listing these (author resolution, comments query, nested comments/replies, sharePost, myReaction). Track which are fixed; flip each from mock → real behind a flag. Until `author` is resolved, **selecting `author` in any query will hard-error**, so omit it from live queries early on.

---

## Backend you consume (GraphQL at `/graphql`)

Single endpoint. Use `graphql_flutter`. The schema lives at `src/main/resources/graphql/social.graphqls` — keep a copy in the Flutter repo for codegen/reference. See `api-contract-reference.md` for the full schema and example operations.

Key types: `Post`, `Comment`, `Category`, `User`, `FeedPage`, `CommentPage`, `PageInfo`. Enums: `ReactionType { UPVOTE DOWNVOTE HAHA LOVE }`, `SharePlatform { TELEGRAM FACEBOOK COPY_LINK }`. Pagination is **cursor-based** (`cursor: UUID`, `pageInfo { hasNextPage, endCursor }`).

---

## Work breakdown

### Phase 1 — Feed UI on mocks (Week 1)
- [ ] `PostRepository` interface + `FakePostRepository` with canned posts/authors
- [ ] Feed screen: infinite scroll using cursor pagination, pull-to-refresh
- [ ] Post card widget (author, title, content, media, reaction/comment/share counts)
- [ ] Loading / empty / error states (shared `core/widgets`)

### Phase 2 — Post detail + create (Week 2)
- [ ] Post detail screen
- [ ] Create post screen (title, content, mediaUrl, categoryId) → `createPost`
- [ ] Edit + delete own post → `updatePost` / `deletePost`
- [ ] Wire `feed`, `post`, `createPost`, `updatePost`, `deletePost` to **real** GraphQL (these work) — but omit `author` selection until the backend fixes it

### Phase 3 — Comments & reactions (Week 3)
- [ ] Comments list under a post (mock the `comments` query until backend lands)
- [ ] Create/edit/delete comment → real mutations
- [ ] Nested replies UI (mock `replies` until backend lands)
- [ ] Reaction picker (UPVOTE/DOWNVOTE/HAHA/LOVE) → `react` / `removeReaction`
- [ ] Optimistic UI for reactions; reconcile `myReaction` once backend populates it

### Phase 4 — Categories & sharing (Week 4–5)
- [ ] Categories screen → `categories`, with member count + joined state
- [ ] Join / leave category → `joinCategory` / `leaveCategory`
- [ ] Category-filtered feed (`feed(categoryId:)`)
- [ ] Share sheet (Telegram / Facebook / Copy link) → `sharePost` (mock until backend lands; copy-link can work client-side immediately)

---

## GraphQL client notes
- The GraphQL client uses the same JWT as REST but **does not** share the dio auto-refresh interceptor. Surface auth errors and coordinate with Dev A on a shared refresh helper so a 15-min-expired token doesn't break the feed mid-scroll.
- Use `graphql_flutter` cache for the feed; invalidate on create/delete.
- Keep all queries/mutations in `features/social/data/graphql/*.graphql` files; generate typed bindings if you adopt `graphql_codegen`.

---

## Definition of Done (your lane)
Standard DoD (`README.md` §8) plus:
- [ ] Every mocked operation has a documented backend ticket and a single flag to flip to real
- [ ] Cursor pagination verified (no duplicate/skipped posts at page boundaries)
- [ ] No live query selects a field the server can't resolve (no `author`/nested until fixed)
