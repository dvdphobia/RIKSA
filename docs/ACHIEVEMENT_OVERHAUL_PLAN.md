# Achievement System Overhaul Plan

**Project:** RIKSA Learning Platform (riksa-api)
**Date:** July 2026
**Status:** Draft — awaiting approval

---

## 1. Current State (Audit)

### What exists

| Component | Status |
|---|---|
| `Achievement` entity + `achievements` table | Exists, well-structured |
| `UserAchievement` entity + `user_achievements` table | Exists, has `unlocked_at` |
| `AchievementRepository.findByCriteriaType()` | Exists |
| `UserAchievementRepository.findByUserId()` / `existsByUserIdAndAchievementId()` | Exists |
| `AchievementService` with check methods | Partially implemented (see below) |
| `AchievementListener` (listens to XP/streak/rank events) | Partially wired (see below) |
| `GamificationController` `GET /api/v1/gamification/achievements` | Returns earned/unearned list |
| `AchievementCriteriaType` enum (7 values) | 4 of 7 never implemented |
| V7 seed migration | 6 achievements seeded |

### What's broken or missing

**BROKEN WIRING — `AchievementListener` does not listen to `LessonCompletedEvent`.**
The listener only handles `XpGainedEvent`, `StreakUpdatedEvent`, and `RankChangedEvent`.
The methods `checkCourseCompletionAchievements()` and `checkDifficultyAchievements()` exist in `AchievementService` but are never called by any listener. Four of the six seeded achievements are unreachable.

| Seeded achievement | Criteria type | Fires? | Reason |
|---|---|---|---|
| First Lesson (N=1) | COMPLETE_N_COURSES | NO | No listener calls `checkCourseCompletionAchievements` |
| Quick Learner (N=10) | COMPLETE_N_COURSES | NO | Same |
| Dedicated Student (N=50) | COMPLETE_N_COURSES | NO | Same |
| XP Hunter (1000 XP) | EARN_XP | YES | Via XpGainedEvent → checkXpAchievements |
| XP Master (5000 XP) | EARN_XP | YES | Same |
| Hard Mode | COMPLETE_COURSE_WITH_DIFFICULTY | NO | No listener calls `checkDifficultyAchievements` |

**STUB METHODS — `checkStreakAchievements()` and `checkRankAchievements()` are empty.**
They log a message and return. No streak or rank criteria type exists in the enum, and no achievements are seeded for them.

**DEAD CRITERIA TYPES — 4 of 7 enum values have no implementation and no seed data.**
`COMPLETE_COURSE`, `COMPLETE_COURSE_IN_EVERY_CATEGORY`, `COMPLETE_ALL_IN_CATEGORY`, `COMPLETE_ALL_IN_EVERY_CATEGORY`.

**NO LESSON_COMPLETION REPOSITORY.**
The `lesson_completions` table exists in the DB but there is no `LessonCompletion` entity or `LessonCompletionRepository` in the Java code. This means we can't count how many lessons a user has completed without a native query or a new entity.

**NO PROGRESS TRACKING.** The API returns `earned: true/false` but not progress toward the threshold (e.g., "7/10 lessons").

**NO ACHIEVEMENT RARITY/TIER.** All achievements are flat — no common/rare/epic/legendary distinction.

**NO XP REWARD FOR UNLOCKING.** Unlocking an achievement gives nothing.

**NO SOCIAL/BLOG ACHIEVEMENTS.** The app has posts, comments, reactions, blog posts, and blog likes — none are tracked for achievements.

**ONLY 6 ACHIEVEMENTS.** Even if all worked, 6 is thin for a gamified learning app.

---

## 2. Implementation Plan

### Phase 1: Fix the wiring (make existing achievements work)

**Goal:** Make all 6 currently-seeded achievements unlock correctly.

#### 1.1 Create `LessonCompletion` entity + repository

New file: `src/main/java/com/riksa/learning/entity/LessonCompletion.java`
```
@Entity @Table(name = "lesson_completions")
Fields: id (UUID), userId, lessonId, xpEarned, completedAt, createdAt
Unique constraint: (user_id, lesson_id)
```

New file: `src/main/java/com/riksa/learning/repository/LessonCompletionRepository.java`
```
interface LessonCompletionRepository extends JpaRepository<LessonCompletion, UUID> {
    long countByUserId(UUID userId);
    boolean existsByUserIdAndLessonId(UUID userId, UUID lessonId);
    List<LessonCompletion> findByUserId(UUID userId);
}
```

#### 1.2 Fix `AchievementListener` to also listen to `LessonCompletedEvent`

Add a new `@EventListener` method in `AchievementListener`:
```java
@EventListener
public void handleLessonCompleted(LessonCompletedEvent event) {
    long coursesCompleted = lessonCompletionRepository.countByUserId(event.getUserId());
    achievementService.checkCourseCompletionAchievements(event.getUserId(), (int) coursesCompleted);

    // Fetch lesson difficulty and check difficulty achievements
    Lesson lesson = lessonRepository.findById(event.getLessonId()).orElse(null);
    if (lesson != null) {
        achievementService.checkDifficultyAchievements(event.getUserId(), lesson.getDifficulty().name());
    }
}
```

Inject `LessonCompletionRepository` and `LessonRepository` into `AchievementListener`.

#### 1.3 Fix `checkDifficultyAchievements` to filter by difficulty

Current code unlocks ALL difficulty achievements on any difficulty lesson. Fix it to check `requiresAdvanced` flag:
- If lesson difficulty is HARD or DIFFICULT → unlock achievements where `requiresAdvanced = true`
- If lesson difficulty is EASY or MEDIUM → unlock achievements where `requiresAdvanced = false`

Add a `difficulty` column to the `achievements` table (nullable, stores which difficulty this achievement is for) OR reuse the `requiresAdvanced` boolean.

**Files to change:**
- `AchievementListener.java` — add `handleLessonCompleted`, inject repos
- `AchievementService.java` — fix `checkDifficultyAchievements` logic
- New: `LessonCompletion.java`, `LessonCompletionRepository.java`

**Verification:** Complete a lesson → "First Lesson" achievement should unlock. Complete a HARD lesson → "Hard Mode" should unlock.

---

### Phase 2: Implement the 4 dead criteria types

**Goal:** Make `COMPLETE_COURSE`, `COMPLETE_COURSE_IN_EVERY_CATEGORY`, `COMPLETE_ALL_IN_CATEGORY`, `COMPLETE_ALL_IN_EVERY_CATEGORY` work.

These require category-aware queries:

#### 2.1 Add query methods to `LessonCompletionRepository`

```java
// Count distinct categories the user has completed lessons in
@Query("SELECT COUNT(DISTINCT l.category.id) FROM LessonCompletion lc JOIN Lesson l ON lc.lessonId = l.id WHERE lc.userId = :userId")
long countDistinctCategoriesCompleted(@Param("userId") UUID userId);

// Check if user has completed all lessons in a specific category
@Query("SELECT COUNT(lc) FROM LessonCompletion lc JOIN Lesson l ON lc.lessonId = l.id WHERE lc.userId = :userId AND l.category.id = :categoryId")
long countCompletedInCategory(@Param("userId") UUID userId, @Param("categoryId") UUID categoryId);
```

#### 2.2 Add `categoryId` to `Achievement` entity (optional, for category-specific achievements)

Some achievements like "Complete all lessons in Category X" need to know which category. Add a nullable `category_id` FK to `achievements` table:
```sql
ALTER TABLE "achievements" ADD COLUMN "category_id" UUID REFERENCES "categories"(id) ON DELETE SET NULL;
```

#### 2.3 Implement check methods in `AchievementService`

```java
// COMPLETE_COURSE — unlock on any single lesson completion (criteria_value ignored)
public void checkSingleCourseAchievements(UUID userId) { ... }

// COMPLETE_COURSE_IN_EVERY_CATEGORY — user has completed at least 1 lesson in every category
public void checkEveryCategoryAchievements(UUID userId) { ... }

// COMPLETE_ALL_IN_CATEGORY — user has completed ALL lessons in a specific category
public void checkAllInCategoryAchievements(UUID userId) { ... }

// COMPLETE_ALL_IN_EVERY_CATEGORY — user has completed ALL lessons across ALL categories
public void checkAllInEveryCategoryAchievements(UUID userId) { ... }
```

#### 2.4 Wire into `AchievementListener.handleLessonCompleted`

After counting courses completed, also call the category-based checks.

**Files to change:**
- `AchievementService.java` — 4 new check methods
- `AchievementListener.java` — call the new methods
- `Achievement.java` — add `categoryId` field
- `LessonCompletionRepository.java` — add category queries
- New migration: `V12__add_category_id_to_achievements.sql`

---

### Phase 3: Add STREAK and RANK criteria types + seed data

**Goal:** Streak milestones and rank tiers become unlockable achievements.

#### 3.1 Add new enum values

In `AchievementCriteriaType.java`:
```java
MAINTAIN_STREAK,   // criteria_value = N days
REACH_RANK         // criteria_value = rank tier min_xp (or store tier name in a new column)
```

In DB migration:
```sql
ALTER TYPE "AchievementCriteriaType" ADD VALUE IF NOT EXISTS 'MAINTAIN_STREAK';
ALTER TYPE "AchievementCriteriaType" ADD VALUE IF NOT EXISTS 'REACH_RANK';
```

#### 3.2 Implement `checkStreakAchievements` properly

Replace the empty stub:
```java
public void checkStreakAchievements(UUID userId, int currentStreak) {
    List<Achievement> streakAchievements = achievementRepository
            .findByCriteriaType(AchievementCriteriaType.MAINTAIN_STREAK);
    for (Achievement a : streakAchievements) {
        if (alreadyEarned(userId, a.getId())) continue;
        if (currentStreak >= a.getCriteriaValue()) {
            unlockAchievement(userId, a);
        }
    }
}
```

#### 3.3 Implement `checkRankAchievements` properly

```java
public void checkRankAchievements(UUID userId, String newRank) {
    List<Achievement> rankAchievements = achievementRepository
            .findByCriteriaType(AchievementCriteriaType.REACH_RANK);
    for (Achievement a : rankAchievements) {
        if (alreadyEarned(userId, a.getId())) continue;
        // Compare rank name stored in description or a new rank_name column
        if (newRank.equals(a.getCriteriaValue().toString())) {  // or use a string field
            unlockAchievement(userId, a);
        }
    }
}
```

Note: `REACH_RANK` needs a string comparison (rank name), but `criteriaValue` is a Long. Options:
- (A) Add a `criteria_text` column to `achievements` for string-based criteria
- (B) Store rank ordinal as the criteriaValue (BRONZE=0, SILVER=1, etc.)

Option B is simpler. Map tier names to ordinals in the service.

#### 3.4 Seed streak + rank achievements (new migration)

```sql
-- Streak achievements
INSERT INTO "achievements" (name, description, criteria_type, criteria_value, icon_url) VALUES
    ('First Steps', 'Maintain a 3-day streak', 'MAINTAIN_STREAK', 3, '/icons/ach-streak-3.png'),
    ('Week Warrior', 'Maintain a 7-day streak', 'MAINTAIN_STREAK', 7, '/icons/ach-streak-7.png'),
    ('Fortnight Fighter', 'Maintain a 14-day streak', 'MAINTAIN_STREAK', 14, '/icons/ach-streak-14.png'),
    ('Monthly Master', 'Maintain a 30-day streak', 'MAINTAIN_STREAK', 30, '/icons/ach-streak-30.png'),
    ('Quarterly Quest', 'Maintain a 90-day streak', 'MAINTAIN_STREAK', 90, '/icons/ach-streak-90.png'),
    ('Yearly Legend', 'Maintain a 365-day streak', 'MAINTAIN_STREAK', 365, '/icons/ach-streak-365.png');

-- Rank achievements (using tier ordinal as criteria_value)
INSERT INTO "achievements" (name, description, criteria_type, criteria_value, icon_url) VALUES
    ('Silver Tier', 'Reach Silver rank', 'REACH_RANK', 1, '/icons/ach-rank-silver.png'),
    ('Gold Tier', 'Reach Gold rank', 'REACH_RANK', 2, '/icons/ach-rank-gold.png'),
    ('Platinum Tier', 'Reach Platinum rank', 'REACH_RANK', 3, '/icons/ach-rank-platinum.png'),
    ('Diamond Tier', 'Reach Diamond rank', 'REACH_RANK', 4, '/icons/ach-rank-diamond.png'),
    ('Legend Tier', 'Reach Legend rank', 'REACH_RANK', 5, '/icons/ach-rank-legend.png');
```

**Files to change:**
- `AchievementCriteriaType.java` — add 2 enum values
- `AchievementService.java` — implement `checkStreakAchievements` + `checkRankAchievements`
- New migration: `V13__add_streak_rank_criteria_types.sql` (ALTER TYPE + seed)

**Verification:** Update streak to 7 → "Week Warrior" unlocks. Rank up to Gold → "Gold Tier" unlocks.

---

### Phase 4: Add SOCIAL and BLOG achievements

**Goal:** Reward community participation.

#### 4.1 Add new criteria types

```java
CREATE_POST,           // criteria_value = N posts
RECEIVE_REACTION,      // criteria_value = N total reactions on user's posts
WRITE_BLOG_POST,       // criteria_value = N approved blog posts
RECEIVE_BLOG_LIKE,     // criteria_value = N total blog likes
```

DB migration:
```sql
ALTER TYPE "AchievementCriteriaType" ADD VALUE IF NOT EXISTS 'CREATE_POST';
ALTER TYPE "AchievementCriteriaType" ADD VALUE IF NOT EXISTS 'RECEIVE_REACTION';
ALTER TYPE "AchievementCriteriaType" ADD VALUE IF NOT EXISTS 'WRITE_BLOG_POST';
ALTER TYPE "AchievementCriteriaType" ADD VALUE IF NOT EXISTS 'RECEIVE_BLOG_LIKE';
```

#### 4.2 Create new events

The existing `PostCreatedEvent` is a record (not a `DomainEvent`). For consistency, either:
- (A) Convert it to extend `DomainEvent` and publish via `EventPublisher`
- (B) Keep it as-is and have `AchievementListener` listen to it directly via `ApplicationEventPublisher`

Option B is less disruptive. Add `@EventListener` in a new `SocialAchievementListener`:

New files:
- `src/main/java/com/riksa/social/event/CommentCreatedEvent.java` — `(commentId, authorId, postId)`
- `src/main/java/com/riksa/social/event/ReactionReceivedEvent.java` — `(targetOwnerId, reactionType, postId)`
- `src/main/java/com/riksa/blog/event/BlogPostApprovedEvent.java` — `(blogPostId, authorId)`
- `src/main/java/com/riksa/blog/event/BlogPostLikedEvent.java` — `(blogPostId, authorId, likerId)`

#### 4.3 Create `SocialAchievementListener`

```java
@Component
public class SocialAchievementListener {
    @EventListener
    public void handlePostCreated(PostCreatedEvent event) {
        long postCount = postRepository.countByAuthorId(event.authorId());
        achievementService.checkPostAchievements(event.authorId(), (int) postCount);
    }
    // ... similar for reactions, blog posts, blog likes
}
```

#### 4.4 Add count methods to repositories

```java
// PostRepository
long countByAuthorId(UUID authorId);

// ReactionRepository — count reactions on all posts by an author
@Query("SELECT COUNT(r) FROM Reaction r WHERE r.post.authorId = :authorId")
long countReactionsOnUserPosts(@Param("authorId") UUID authorId);

// BlogPostRepository
long countByAuthorIdAndStatus(UUID authorId, BlogStatus status);

// BlogLikeRepository — count likes on all blog posts by an author
@Query("SELECT COUNT(bl) FROM BlogLike bl WHERE bl.blogPost.authorId = :authorId")
long countLikesOnUserBlogPosts(@Param("authorId") UUID authorId);
```

#### 4.5 Seed social + blog achievements

```sql
INSERT INTO "achievements" (name, description, criteria_type, criteria_value, icon_url) VALUES
    ('First Post', 'Create your first community post', 'CREATE_POST', 1, '/icons/ach-first-post.png'),
    ('Active Contributor', 'Create 10 community posts', 'CREATE_POST', 10, '/icons/ach-contributor.png'),
    ('Community Pillar', 'Create 50 community posts', 'CREATE_POST', 50, '/icons/ach-pillar.png'),
    ('Getting Noticed', 'Receive 10 reactions on your posts', 'RECEIVE_REACTION', 10, '/icons/ach-noticed.png'),
    ('Popular Voice', 'Receive 100 reactions on your posts', 'RECEIVE_REACTION', 100, '/icons/ach-popular.png'),
    ('First Blog', 'Publish your first approved blog post', 'WRITE_BLOG_POST', 1, '/icons/ach-first-blog.png'),
    ('Blogger', 'Publish 5 approved blog posts', 'WRITE_BLOG_POST', 5, '/icons/ach-blogger.png'),
    ('Prolific Writer', 'Publish 20 approved blog posts', 'WRITE_BLOG_POST', 20, '/icons/ach-prolific.png'),
    ('Rising Author', 'Receive 10 likes on your blog posts', 'RECEIVE_BLOG_LIKE', 10, '/icons/ach-rising-author.png'),
    ('Beloved Author', 'Receive 100 likes on your blog posts', 'RECEIVE_BLOG_LIKE', 100, '/icons/ach-beloved.png');
```

**Files to change:**
- `AchievementCriteriaType.java` — 4 new enum values
- `AchievementService.java` — 4 new check methods
- New: `SocialAchievementListener.java`
- New events: `CommentCreatedEvent`, `ReactionReceivedEvent`, `BlogPostApprovedEvent`, `BlogPostLikedEvent`
- `PostService.java`, `ReactionService.java`, `BlogService.java` / `BlogModerationService.java` — publish new events
- Repository query additions
- New migration: `V14__add_social_blog_criteria_types.sql`

**Verification:** Create a post → "First Post" unlocks. Get a blog post approved → "First Blog" unlocks.

---

### Phase 5: Add progress tracking to the API

**Goal:** Let the Flutter app show "7/10 lessons" progress bars toward each achievement.

#### 5.1 Add `progress` and `progressMax` to `AchievementResponse`

```java
public class AchievementResponse {
    private UUID id;
    private String name;
    private String description;
    private String iconUrl;
    private boolean earned;
    private OffsetDateTime unlockedAt;
    // NEW:
    private int progress;      // current value (e.g., 7 lessons completed)
    private int progressMax;   // threshold (e.g., 10)
    private double progressPercent; // 0.0 - 1.0 (convenience for Flutter)
}
```

#### 5.2 Compute progress in `GamificationService.getAchievements`

For each unearned achievement, compute progress based on criteria type:

| Criteria type | Progress source |
|---|---|
| EARN_XP | `userGamification.totalXp` |
| COMPLETE_N_COURSES | `lessonCompletionRepository.countByUserId()` |
| MAINTAIN_STREAK | `userGamification.currentStreak` |
| REACH_RANK | ordinal of user's current rank tier |
| CREATE_POST | `postRepository.countByAuthorId()` |
| RECEIVE_REACTION | `reactionRepository.countReactionsOnUserPosts()` |
| WRITE_BLOG_POST | `blogPostRepository.countByAuthorIdAndStatus(APPROVED)` |
| RECEIVE_BLOG_LIKE | `blogLikeRepository.countLikesOnUserBlogPosts()` |
| COMPLETE_ALL_IN_CATEGORY | `completedInCategory / totalLessonsInCategory` |
| Others | progress = 0 (binary: earned or not) |

For earned achievements, set `progress = progressMax` and `progressPercent = 1.0`.

#### 5.3 Add `rarity` field (optional but recommended)

Add a `rarity` column to `achievements`:
```sql
CREATE TYPE "AchievementRarity" AS ENUM ('COMMON', 'RARE', 'EPIC', 'LEGENDARY');
ALTER TABLE "achievements" ADD COLUMN "rarity" "AchievementRarity" NOT NULL DEFAULT 'COMMON';
```

Map in the response so Flutter can show different badge colors/styles.

**Files to change:**
- `AchievementResponse.java` — add `progress`, `progressMax`, `progressPercent`, `rarity`
- `GamificationService.java` — compute progress in `getAchievements()`
- `Achievement.java` — add `rarity` field
- New migration: `V15__add_achievement_rarity.sql`

---

### Phase 6: XP reward for unlocking achievements

**Goal:** Unlocking an achievement gives bonus XP, creating a positive feedback loop.

#### 6.1 Add `xp_reward` column to `achievements`

```sql
ALTER TABLE "achievements" ADD COLUMN "xp_reward" INTEGER NOT NULL DEFAULT 0;
```

#### 6.2 Award XP in `unlockAchievement`

In `AchievementService.unlockAchievement()`:
```java
private void unlockAchievement(UUID userId, Achievement achievement) {
    UserAchievement ua = new UserAchievement();
    ua.setUserId(userId);
    ua.setAchievementId(achievement.getId());
    userAchievementRepository.save(ua);

    if (achievement.getXpReward() > 0) {
        xpLedgerService.awardXp(userId, achievement.getXpReward(), "ACHIEVEMENT_UNLOCK", achievement.getId());
    }

    log.info("Achievement unlocked: '{}' for user {} (XP reward: {})",
            achievement.getName(), userId, achievement.getXpReward());
    eventPublisher.publish(new AchievementUnlockedEvent(this, userId,
            achievement.getId(), achievement.getName()));
}
```

Inject `XpLedgerService` into `AchievementService`.

**CAUTION:** This creates an event chain: unlock → awardXp → XpGainedEvent → checkXpAchievements → potentially unlock another achievement → awardXp → ...
The `alreadyEarned()` check prevents infinite loops (once an achievement is unlocked, it won't unlock again). But be aware that a batch of XP achievements could cascade. This is acceptable — it's a one-time cascade that terminates when all XP achievements are earned.

#### 6.3 Update seed data with XP rewards

```sql
UPDATE "achievements" SET xp_reward = 50 WHERE name = 'First Lesson';
UPDATE "achievements" SET xp_reward = 100 WHERE name = 'Quick Learner';
UPDATE "achievements" SET xp_reward = 500 WHERE name = 'Dedicated Student';
-- ... etc
```

**Files to change:**
- `Achievement.java` — add `xpReward` field
- `AchievementService.java` — inject `XpLedgerService`, call `awardXp` in `unlockAchievement`
- New migration: `V16__add_achievement_xp_reward.sql`

---

### Phase 7: Expand to 30+ achievements (full seed)

**Goal:** A rich, diverse achievement catalog.

Full seed list (30 achievements across all categories):

#### Learning (10)
| Name | Criteria | Value | Rarity | XP Reward |
|---|---|---|---|---|
| First Lesson | COMPLETE_N_COURSES | 1 | COMMON | 50 |
| Quick Learner | COMPLETE_N_COURSES | 10 | COMMON | 100 |
| Dedicated Student | COMPLETE_N_COURSES | 50 | RARE | 500 |
| Century Scholar | COMPLETE_N_COURSES | 100 | EPIC | 1000 |
| Hard Mode | COMPLETE_COURSE_WITH_DIFFICULTY | 0 (HARD) | RARE | 150 |
| Difficult Mastery | COMPLETE_COURSE_WITH_DIFFICULTY | 0 (DIFFICULT) | EPIC | 300 |
| Category Explorer | COMPLETE_COURSE_IN_EVERY_CATEGORY | 1 | RARE | 200 |
| Completionist | COMPLETE_ALL_IN_EVERY_CATEGORY | 1 | LEGENDARY | 2000 |
| XP Hunter | EARN_XP | 1000 | COMMON | 100 |
| XP Master | EARN_XP | 5000 | EPIC | 500 |

#### Streak (6)
| Name | Criteria | Value | Rarity | XP Reward |
|---|---|---|---|---|
| First Steps | MAINTAIN_STREAK | 3 | COMMON | 30 |
| Week Warrior | MAINTAIN_STREAK | 7 | COMMON | 50 |
| Fortnight Fighter | MAINTAIN_STREAK | 14 | RARE | 100 |
| Monthly Master | MAINTAIN_STREAK | 30 | EPIC | 300 |
| Quarterly Quest | MAINTAIN_STREAK | 90 | EPIC | 500 |
| Yearly Legend | MAINTAIN_STREAK | 365 | LEGENDARY | 2000 |

#### Rank (5)
| Name | Criteria | Value | Rarity | XP Reward |
|---|---|---|---|---|
| Silver Tier | REACH_RANK | 1 | COMMON | 50 |
| Gold Tier | REACH_RANK | 2 | RARE | 150 |
| Platinum Tier | REACH_RANK | 3 | EPIC | 400 |
| Diamond Tier | REACH_RANK | 4 | EPIC | 800 |
| Legend Tier | REACH_RANK | 5 | LEGENDARY | 2000 |

#### Social (5)
| Name | Criteria | Value | Rarity | XP Reward |
|---|---|---|---|---|
| First Post | CREATE_POST | 1 | COMMON | 20 |
| Active Contributor | CREATE_POST | 10 | RARE | 100 |
| Getting Noticed | RECEIVE_REACTION | 10 | COMMON | 50 |
| Popular Voice | RECEIVE_REACTION | 100 | EPIC | 300 |
| Community Pillar | CREATE_POST | 50 | EPIC | 500 |

#### Blog (4)
| Name | Criteria | Value | Rarity | XP Reward |
|---|---|---|---|---|
| First Blog | WRITE_BLOG_POST | 1 | COMMON | 50 |
| Blogger | WRITE_BLOG_POST | 5 | RARE | 200 |
| Rising Author | RECEIVE_BLOG_LIKE | 10 | RARE | 100 |
| Beloved Author | RECEIVE_BLOG_LIKE | 100 | LEGENDARY | 1000 |

**Total: 30 achievements**

New migration: `V17__seed_full_achievements.sql` — replaces the V7 seed (or adds the missing ones with `INSERT ... ON CONFLICT (name) DO NOTHING`).

---

## 3. Migration Summary

| Migration | Description |
|---|---|
| V12__add_category_id_to_achievements.sql | Add nullable `category_id` FK to `achievements` |
| V13__add_streak_rank_criteria_types.sql | ALTER TYPE add `MAINTAIN_STREAK`, `REACH_RANK` + seed streak/rank achievements |
| V14__add_social_blog_criteria_types.sql | ALTER TYPE add 4 social/blog types + seed social/blog achievements |
| V15__add_achievement_rarity.sql | Create `AchievementRarity` enum + add `rarity` column |
| V16__add_achievement_xp_reward.sql | Add `xp_reward` INTEGER column |
| V17__seed_full_achievements.sql | Full 30-achievement seed with rarity + XP rewards |

---

## 4. File Change Summary

### New files (16)
| File | Purpose |
|---|---|
| `learning/entity/LessonCompletion.java` | Entity for `lesson_completions` table |
| `learning/repository/LessonCompletionRepository.java` | Count + category queries |
| `gamification/listener/SocialAchievementListener.java` | Listens to social/blog events |
| `social/event/CommentCreatedEvent.java` | Event for comment creation |
| `social/event/ReactionReceivedEvent.java` | Event for reaction on a post |
| `blog/event/BlogPostApprovedEvent.java` | Event for blog approval |
| `blog/event/BlogPostLikedEvent.java` | Event for blog like |
| 6 Flyway migration SQL files | Schema changes + seed data |

### Modified files (12)
| File | Changes |
|---|---|
| `common/enums/AchievementCriteriaType.java` | Add 6 new enum values |
| `gamification/entity/Achievement.java` | Add `categoryId`, `rarity`, `xpReward` fields |
| `gamification/service/AchievementService.java` | Implement all stub methods + add social/blog checks + XP reward in `unlockAchievement` |
| `gamification/listener/AchievementListener.java` | Add `handleLessonCompleted`, inject repos |
| `gamification/dto/AchievementResponse.java` | Add `progress`, `progressMax`, `progressPercent`, `rarity` |
| `gamification/service/GamificationService.java` | Compute progress in `getAchievements()` |
| `social/service/PostService.java` | Publish `PostCreatedEvent` (already does) — ensure it fires |
| `social/service/ReactionService.java` | Publish `ReactionReceivedEvent` |
| `social/service/CommentService.java` | Publish `CommentCreatedEvent` |
| `blog/service/BlogModerationService.java` | Publish `BlogPostApprovedEvent` on approval |
| `blog/service/BlogEngagementService.java` | Publish `BlogPostLikedEvent` on like |
| `social/event/PostCreatedEvent.java` | Convert from record to class extending `DomainEvent` (or keep record + listen directly) |

### New test files (3)
| File | Purpose |
|---|---|
| `gamification/service/AchievementServiceTest.java` | Unit test all criteria type checks |
| `gamification/listener/AchievementListenerTest.java` | Test event wiring |
| `gamification/listener/SocialAchievementListenerTest.java` | Test social event wiring |

---

## 5. Implementation Order

1. **Phase 1** (fix wiring) — highest impact, lowest effort. Makes 4 of 6 existing achievements work.
2. **Phase 3** (streak + rank) — medium effort, high user engagement value. Streak milestones already exist visually.
3. **Phase 5** (progress tracking) — medium effort, high UX value. Flutter app can show progress bars.
4. **Phase 4** (social + blog) — higher effort (new events + listener), but rewards community participation.
5. **Phase 2** (category criteria) — medium effort, lower priority (depends on having enough lessons per category).
6. **Phase 6** (XP reward) — low effort, nice feedback loop.
7. **Phase 7** (full seed) — low effort (just SQL), do alongside each phase.

---

## 6. API Contract (after all phases)

### GET /api/v1/gamification/achievements

**Response:**
```json
[
  {
    "id": "uuid",
    "name": "Quick Learner",
    "description": "Complete 10 lessons",
    "iconUrl": "/icons/ach-quick-learner.png",
    "earned": false,
    "unlockedAt": null,
    "progress": 7,
    "progressMax": 10,
    "progressPercent": 0.7,
    "rarity": "COMMON",
    "xpReward": 100
  },
  {
    "id": "uuid",
    "name": "First Lesson",
    "description": "Complete your first lesson",
    "iconUrl": "/icons/ach-first-lesson.png",
    "earned": true,
    "unlockedAt": "2026-07-15T10:30:00Z",
    "progress": 1,
    "progressMax": 1,
    "progressPercent": 1.0,
    "rarity": "COMMON",
    "xpReward": 50
  }
]
```

### GET /api/v1/gamification/achievements/{id}

**Optional new endpoint** — get a single achievement detail with progress. Useful for a detail screen in Flutter.

### GET /api/v1/gamification/achievements/summary

**Optional new endpoint** — aggregate stats:
```json
{
  "totalAchievements": 30,
  "earnedCount": 8,
  "earnedPercent": 0.267,
  "totalXpFromAchievements": 1200,
  "earnedXpFromAchievements": 350,
  "rarestAchievement": "Yearly Legend"
}
```

---

## 7. Risks and Mitigations

| Risk | Mitigation |
|---|---|
| Event cascade (unlock → XP → unlock) | `alreadyEarned()` check terminates the chain. Add a max-depth guard if paranoid. |
| N+1 queries in progress computation | Batch-fetch counts per criteria type, not per achievement. Cache if needed. |
| `ALTER TYPE ... ADD VALUE` in Flyway | PostgreSQL supports this but it must be in a separate transaction. Each ALTER TYPE in its own migration file or use `IF NOT EXISTS`. |
| Category-specific achievements need stable category IDs | Seed categories in a migration first, then reference them by UUID in achievement seeds. |
| `PostCreatedEvent` is a record, not a `DomainEvent` | Either convert it or listen to it directly via `@EventListener` — Spring doesn't require extending `ApplicationEvent`. |
| Progress for binary achievements (COMPLETE_COURSE_IN_EVERY_CATEGORY) | Return `progress = 0, progressMax = 1` when not earned, `1/1` when earned. Flutter shows a locked badge. |

---

## 8. Flutter Impact

After the backend changes, the Flutter app's achievement screen needs to:
1. Display achievements grouped by category (Learning, Streak, Rank, Social, Blog) or by rarity
2. Show progress bars for unearned achievements (using `progress` / `progressMax`)
3. Show rarity badges with different colors (COMMON=gray, RARE=blue, EPIC=purple, LEGENDARY=gold)
4. Show XP reward on each achievement card
5. Show a celebration animation when an achievement unlocks (listen for `AchievementUnlockedEvent` via WebSocket or poll on screen entry)
6. Optional: achievement detail screen with description, progress, and unlocked date

These are Flutter-side changes and should be tracked separately after the backend is deployed.