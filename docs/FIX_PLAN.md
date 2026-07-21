# RIKSA Backend — Complete Fix Plan

*Generated Jul 20, 2026. Items marked [DONE] were fixed in the same session.*

---

## A. CRITICAL — Will Break at Runtime

### A1. [DONE] Duplicate V9 Flyway migration
- Renamed V9__add_blog_likes_created_at.sql -> V12

### A2. [DONE] UserGamification row not created on registration
- AuthService.register() and OAuth2Service.googleAuth() now create the row
- V14 migration backfills existing users

### A3. [DONE] Entity/DB BIGINT type mismatches
- UserGamification, XpTransaction, StreakMilestone entities updated from int to long to match V9 BIGINT columns
- All downstream DTOs, services, events, repos, tests updated

### A4. [DONE] BlogModerationService used jakarta.transaction.Transactional
- Changed to org.springframework.transaction.annotation.Transactional

### A5. [DONE] UserResponse typo: avatartUrl -> avatarUrl
- Fixed in UserResponse.java record

### A6. [DONE] CommentRepository wrong Pageable import
- Changed from java.awt.print.Pageable to org.springframework.data.domain.Pageable

---

## B. HIGH — Performance / Edge Cases

### B1. N+1 query explosion in SocialResponseResolver.resolvePosts()
**Problem:** For every post in the feed, resolvePost() makes 3 separate DB calls:
- resolveUser(authorId) -> userRepository.findById()
- resolveCategory(categoryId) -> categoryService.listCategories() then streams+filters (loads ALL categories every time)
- resolveMyReaction() -> reactionRepository.findByUserIdAnd...

A feed of 20 posts = 60 DB queries. On Cambodian 3G this will be painfully slow.

**Fix:** Batch-load in resolvePosts():
1. Collect all authorIds, fetch all Users in one findAllById() call, build a Map<UUID, UserResponse>
2. Collect all categoryIds, fetch categories once (already loaded by listCategories), build a Map<UUID, Category>
3. Collect all postIds, batch-query reactions for the current user in one query: reactionRepository.findByUserIdAndPostIdIn(userId, postIds). Build a Map<UUID, ReactionType>
4. Assemble PostResponses from the maps — zero per-post queries

Requires adding: `ReactionRepository.findByUserIdAndPostIdIn(UUID, List<UUID>)`

### B2. Feed cursor: no tie-breaking on createdAt
**Problem:** PostRepository.findFeed() uses:
```
p.createdAt < (SELECT p2.createdAt FROM Post p2 WHERE p2.id = :cursor)
```
If two posts share the same createdAt (common under load), the cursor skips posts at that timestamp. The gamification module uses composite-key tie-breaking (createdAt + id), but social doesn't.

**Fix:** Rewrite the JPQL to composite-key pattern:
```jpql
WHERE (:cursor IS NULL
  OR p.createdAt < (SELECT p2.createdAt FROM Post p2 WHERE p2.id = :cursor)
  OR (p.createdAt = (SELECT p2.createdAt FROM Post p2 WHERE p2.id = :cursor)
      AND p.id < :cursor))
ORDER BY p.createdAt DESC, p.id DESC
```

### B3. Event pipeline: all listeners run synchronously in the same transaction
**Problem:** completeLesson() is @Transactional. It publishes LessonCompletedEvent. XpLedgerListener handles it synchronously (also @Transactional, joins the SAME transaction). It then publishes XpGainedEvent. RankListener and AchievementListener both handle that synchronously too. If any listener throws, the entire lesson completion rolls back — but the listeners catch exceptions and swallow them, so the user sees success while XP was never awarded.

Two sub-issues:
- a) XpLedgerListener catches and swallows exceptions — if the XP grant fails, the lesson completion still succeeds but the user gets no XP. There's no retry, no compensation.
- b) StreakListener and XpLedgerListener both listen to LessonCompletedEvent. Spring calls them sequentially in the same thread. Both use PESSIMISTIC_WRITE on the same user_gamification row. The second listener will block until the first releases the lock.

**Fix (recommended):** Make XpLedgerListener and StreakListener @Async with a TaskExecutor. Use @TransactionalEventListener(phase = AFTER_COMMIT) instead of @EventListener so listeners only fire if the completion tx commits successfully. XP and streak updates happen after the lesson completion transaction commits.

### B4. StreakService.resetStaleStreaks() is never scheduled
**Problem:** The method exists but no @Scheduled annotation calls it. Users who stop using the app will have their streak frozen indefinitely instead of resetting to 0 at midnight.

**Fix:** Add a scheduled task:
```java
@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Phnom_Penh")
public void resetStaleStreaks() { streakService.resetStaleStreaks(); }
```

### B5. PostCreatedEvent is a plain record, not a DomainEvent
**Problem:** PostService publishes PostCreatedEvent using ApplicationEventPublisher.publishEvent(). But PostCreatedEvent is a record that does NOT extend DomainEvent. Other events all extend DomainEvent and are published via EventPublisher. This inconsistency means any listener that filters on DomainEvent won't catch it.

**Fix:** Change PostCreatedEvent from a record to a class that extends DomainEvent. Update PostService to use EventPublisher instead of ApplicationEventPublisher.

---

## C. MEDIUM — Correctness / Consistency

### C1. LessonListResponse missing "completed" and "exerciseCount" fields
**Problem:** The LEARNING_API_PLAN.md spec says LessonListResponse should have `completed` (boolean — has the current user completed this lesson?) and `exerciseCount`. The DTO has exerciseCount field but it's never populated by LessonService (always 0). The `completed` field is missing entirely.

**Fix:**
1. Add `boolean completed` field to LessonListResponse
2. In LessonService.listLessons(), batch-query LessonCompletionRepository to find which lessonIds the current user has completed. Build a Set<UUID> of completed lesson IDs, set the field
3. For exerciseCount: batch-query exerciseRepository.countByLessonIdIn(lessonIds) and set it in the mapper

### C2. CompletionResponse missing completionId and passed fields
**Problem:** The spec says the response should include `{completionId, lessonId, xpEarned, score, passed}`. The actual DTO is missing `completionId` and `passed`.

**Fix:**
1. Add `UUID completionId` and `boolean passed` to CompletionResponse
2. In LessonCompletionService.toResponse(), set completionId = completion.getId() and passed = true

### C3. LessonService.listLessons() doesn't pass userId for completion status
**Problem:** listLessons() takes (categoryId, difficulty, page, size) but not userId. It can't set the `completed` field because it doesn't know who the user is.

**Fix:** Change the method signature to include UUID userId, pass it from LessonController (which already extracts it from the JWT).

### C4. Duplicate blog moderation endpoints with different contracts
**Problem:** Two endpoints do the same thing:
- PATCH /api/v1/blog/admin/{id}/status?status=APPROVED (BlogController, query param, no moderatorId)
- PATCH /api/v1/admin/blog/{id}/status body {status, reason} (BlogAdminController, body param, passes moderatorId)

**Fix:** Remove the one on BlogController. Keep only BlogAdminController's version which passes moderatorId for audit events. Update PROJECT_GUIDE.md.

---

## D. LOW — Cleanup / Hygiene

### D1. Stale .gitkeep files in 23 directories that now have real Java files
**Fix:** Delete all .gitkeep files from directories that contain at least one .java file. Keep them only in truly empty directories (chatAi/*, common/dto, common/entity, common/repository, admin/entity, admin/repository).

### D2. No unit tests for new Learning module
**Fix:** Create:
- LessonServiceTest: mock repos, verify list filtering + detail
- LessonCompletionServiceTest: verify completion flow, XP calc, duplicate prevention, all-exercises-correct check, event publish
- ExerciseAttemptServiceTest: verify answer comparison, upsert, wrong lesson exercise rejection
- SimpleReflectionValidatorTest: verify min length, blank rejection, scoring thresholds

### D3. No unit tests for SocialResponseResolver
**Fix:** Create SocialResponseResolverTest:
- resolvePost: verify author, category, myReaction all resolved
- resolvePosts: verify batch behavior
- resolveComments: verify pagination, correct mapping
- resolveReplies: verify nested comment loading

### D4. No unit tests for ShareService
**Fix:** Create ShareServiceTest:
- sharePost: verify share count incremented, share record saved
- sharePost with null platform: defaults to COPY_LINK
- sharePost non-existent post: throws

### D5. RIKSA_BUILD_GUIDE.md is stale
Already marked deprecated with a pointer to PROJECT_GUIDE.md. Consider deleting entirely or keeping as historical reference.

### D6. SOCIAL_MODULE_PLAN.md and GAMIFICATION_PLAN.md are stale
**Fix:** Either update them to match current state or add deprecation notices pointing to PROJECT_GUIDE.md.

### D7. No integration test for the full hot path
**Fix:** Create a @SpringBootTest integration test that:
1. Registers a user (creates UserGamification row)
2. Creates a lesson with exercises (admin)
3. Submits correct exercise attempts
4. Calls completeLesson
5. Verifies XP transaction was created
6. Verifies streak was updated
7. Verifies rank history if tier changed
Requires a test DB (Testcontainers or H2).

### D8. chatAi package uses wrong name
**Problem:** Build guide says `com.riksa.chat` but actual package is `com.riksa.chatAi`. Package is empty (.gitkeep only).

**Fix:** Either rename to `com.riksa.chat` (matching docs) or update all docs. Low priority since module is out of scope for v1.

---

## E. Implementation Order (recommended)

| Phase | Items | Focus |
|-------|-------|-------|
| 1 — Safety | B3, B4, B5 | Fix event pipeline, schedule streak reset, align PostCreatedEvent |
| 2 — Performance | B1, B2 | Batch-load feed resolvers, fix cursor tie-breaking |
| 3 — Correctness | C1, C2, C3, C4 | Lesson completion fields, remove duplicate endpoint |
| 4 — Tests | D2, D3, D4, D7 | Learning tests, resolver tests, share tests, integration test |
| 5 — Cleanup | D1, D5, D6, D8 | Delete .gitkeep, deprecate stale docs, rename chatAi |

---

## F. Things NOT to Fix (intentional design decisions)

- AI Chat module empty — out of scope for v1
- Missing DB tables (user_follows, banner_url, user_interests, exercise_options, daily_challenges, posts.view_count) — need product decisions
- Reactions have HAHA/LOVE in DB but UI only shows up/down — needs UI/backend alignment
- Duplicate leaderboard endpoints — different use cases, keep both
- GraphQL schema doesn't declare searchPosts/userPosts/pinPost — future features