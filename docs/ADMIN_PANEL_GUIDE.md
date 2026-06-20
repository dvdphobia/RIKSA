# Admin Panel Build Guide — Lessons, Categories, Moderation

*For the team building `com.riksa.admin` — CRUD for lessons/categories, blog moderation, and role management.*

---

## Table of Contents
1. [What We're Building](#1-what-were-building)
2. [Current State of the Codebase](#2-current-state-of-the-codebase)
3. [Prerequisites — Entities & Repositories We Need First](#3-prerequisites--entities--repositories-we-need-first)
4. [Admin Package Architecture](#4-admin-package-architecture)
5. [Lesson CRUD — Full Walkthrough](#5-lesson-crud--full-walkthrough)
6. [Category CRUD — Full Walkthrough](#6-category-crud--full-walkthrough)
7. [Blog Moderation — Full Walkthrough](#7-blog-moderation--full-walkthrough)
8. [User Role Management](#8-user-role-management)
9. [Security & Authorization](#9-security--authorization)
10. [Files to Create — Complete Inventory](#10-files-to-create--complete-inventory)
11. [Step-by-Step Team Plan](#11-step-by-step-team-plan)
12. [Testing Strategy](#12-testing-strategy)
13. [Common Pitfalls & Rules](#13-common-pitfalls--rules)

---

## 1. What We're Building

The admin panel is a set of `MODERATOR`-only endpoints that manage:

| Feature | Endpoints | Purpose |
|---------|-----------|---------|
| **Lesson CRUD** | `POST/PUT/DELETE /api/v1/admin/lessons` + bulk upload | Create/manage lessons & exercises |
| **Category CRUD** | `POST/PUT/DELETE /api/v1/admin/categories` | Manage community categories |
| **Blog Moderation** | `PATCH /api/v1/admin/blog/{id}/status` | Approve/reject blog posts |
| **User Role** | `POST /api/v1/admin/users/{id}/role` | Change user role (e.g., promote to MODERATOR) |

**Security requirement:** All admin endpoints require `hasRole('MODERATOR')` or `hasRole('ADMIN')`.

---

## 2. Current State of the Codebase

### What Exists ✅

| Artifact | Location | Notes |
|----------|----------|-------|
| Admin package scaffold | `com.riksa.admin/` | Only `.gitkeep` files — empty |
| DB schema for all tables | `V1__initial_schema.sql` | `lessons`, `exercises`, `categories`, `blog_posts` all exist |
| Admin sub-packages | `controller/`, `service/`, `dto/`, `repository/`, `entity/` | All empty |
| `Lesson.java` entity | `com.riksa.gamification.entity.Lesson` | **Incomplete** — only maps `id`, `baseXp`, `difficulty` — missing `title`, `description`, `category_id`, timestamps |
| Global auth/security | `SecurityConfig`, `JwtService`, `JwtAuthenticationFilter`, `Role` enum | Working — `@PreAuthorize` support ready |
| Global exception handler | `GlobalExceptionHandler` | Handles `IllegalArgumentException`, validation errors, general exceptions |

### What's Missing ❌

| Thing | Why It Matters |
|-------|---------------|
| `Exercise.java` entity | No JPA mapping for `exercises` table |
| `Category.java` entity | No JPA mapping for `categories` table |
| `BlogPost.java` entity | No JPA mapping for `blog_posts` table |
| `LessonRepository`, `ExerciseRepository`, `CategoryRepository`, `BlogPostRepository` | No repositories for querying |
| `BlogStatus` enum in Java | No Java enum for `'PENDING'\|'APPROVED'\|'REJECTED'` |

### Lesson Entity — Needs Rewrite

The current `Lesson.java` in `gamification.entity` was placed there as a minimal reference for the gamification module. It **does not** match the full DB schema. The admin CRUD needs the complete entity. **Decision:** Create a proper `Lesson.java` in `learning.entity` (or in `admin.entity`), then the gamification module can reference it if needed.

**Follow the build guide's package-by-domain rule:** Entities live in their domain package (`learning.entity.Lesson`), not in admin. Admin services inject repositories from other domains.

---

## 3. Prerequisites — Entities & Repositories We Need First

Before the admin controllers can work, we need entities and repositories for the tables they manage. These go in their *domain* packages (not in admin):

### 3.1 Learning Domain (`com.riksa.learning`)

**Entity: `Lesson.java`** — maps the `lessons` table fully:

| DB Column | Java Field | Type | Notes |
|-----------|-----------|------|-------|
| `id` | `id` | `UUID` | `@GeneratedValue(strategy = GenerationType.UUID)` |
| `title` | `title` | `String` | `@NotBlank` |
| `description` | `description` | `String` | nullable |
| `difficulty` | `difficulty` | `DifficultyLevel` | `@Enumerated(EnumType.STRING)` + `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` |
| `base_xp` | `baseXp` | `int` | |
| `category_id` | `category` | `@ManyToOne Category` | nullable, `@JoinColumn(name = "category_id")` |
| `created_at` | `createdAt` | `OffsetDateTime` | `updatable = false` |
| `updated_at` | `updatedAt` | `OffsetDateTime` | `@PreUpdate` |

**Entity: `Exercise.java`** — maps the `exercises` table:

| DB Column | Java Field | Type | Notes |
|-----------|-----------|------|-------|
| `id` | `id` | `UUID` | auto-gen |
| `lesson_id` | `lesson` | `@ManyToOne Lesson` | `@JoinColumn(name = "lesson_id")`, NOT NULL |
| `question` | `question` | `String` | `@NotBlank` `@Column(columnDefinition = "TEXT")` |
| `answer_key` | `answerKey` | `String` | `@NotBlank` `@Column(columnDefinition = "TEXT")` |
| `difficulty` | `difficulty` | `DifficultyLevel` | enum |
| `points` | `points` | `int` | default 5 |
| `created_at` | `createdAt` | `OffsetDateTime` | |
| `updated_at` | `updatedAt` | `OffsetDateTime` | |

**Repositories:**
- `LessonRepository extends JpaRepository<Lesson, UUID>` — no custom queries needed for admin CRUD
- `ExerciseRepository extends JpaRepository<Exercise, UUID>` — add `findByLessonId(UUID lessonId)` for bulk operations

### 3.2 Social Domain (`com.riksa.social`)

**Entity: `Category.java`** — maps the `categories` table:

| DB Column | Java Field | Type |
|-----------|-----------|------|
| `id` | `id` | `UUID` |
| `name` | `name` | `String` (unique, not null) |
| `slug` | `slug` | `String` (unique, not null) |
| `description` | `description` | `String` (nullable) |
| `icon_url` | `iconUrl` | `String` (nullable) |
| `created_at` | `createdAt` | `OffsetDateTime` |
| `updated_at` | `updatedAt` | `OffsetDateTime` |

**Repository:** `CategoryRepository extends JpaRepository<Category, UUID>`
- `Optional<Category> findBySlug(String slug)` — for lookup by slug
- `boolean existsByName(String name)` — for name uniqueness check
- `boolean existsBySlug(String slug)` — for slug uniqueness check

### 3.3 Blog Domain (`com.riksa.blog`)

**Entity: `BlogPost.java`** — maps the `blog_posts` table:

| DB Column | Java Field | Type |
|-----------|-----------|------|
| `id` | `id` | `UUID` |
| `title` | `title` | `String` |
| `content` | `content` | `String` (TEXT) |
| `about` | `about` | `String` (nullable) |
| `banner_url` | `bannerUrl` | `String` (nullable) |
| `author_id` | `author` | `@ManyToOne User` |
| `category_id` | `category` | `@ManyToOne Category` |
| `status` | `status` | `BlogStatus` enum |
| `view_count` | `viewCount` | `int` |
| `like_count` | `likeCount` | `int` |
| `is_ai_generated` | `isAiGenerated` | `boolean` |
| `created_at` | `createdAt` | `OffsetDateTime` |
| `updated_at` | `updatedAt` | `OffsetDateTime` |

**Java enum: `BlogStatus.java`** in `com.riksa.common.enums` (or `com.riksa.blog.enums`):
```java
package com.riksa.common.enums;
public enum BlogStatus {
    PENDING, APPROVED, REJECTED
}
```

**Repository:** `BlogPostRepository extends JpaRepository<BlogPost, UUID>`
- `List<BlogPost> findByStatus(BlogStatus status)` — for moderation listing
- `Optional<BlogPost> findByIdAndStatus(UUID id, BlogStatus status)` — for moderation actions

---

## 4. Admin Package Architecture

```
com.riksa.admin/
├── controller/
│   ├── LessonAdminController.java     (POST/PUT/DELETE lessons + exercises)
│   └── CategoryAdminController.java   (POST/PUT/DELETE categories)
├── service/
│   ├── LessonAdminService.java        (transactional CRUD logic)
│   └── CategoryAdminService.java      (transactional CRUD logic)
└── dto/
    ├── LessonCreateRequest.java       (title, description, difficulty, baseXp, categoryId, exercises[])
    ├── LessonUpdateRequest.java       (same fields, all optional)
    ├── LessonResponse.java            (response with nested exercises)
    ├── ExerciseRequest.java           (question, answerKey, difficulty, points)
    ├── ExerciseResponse.java          (id, question, difficulty, points)
    ├── CategoryRequest.java           (name, slug, description, iconUrl)
    ├── CategoryResponse.java          (id, name, slug, description, iconUrl, createdAt)
    ├── ModerationRequest.java         (status: APPROVED|REJECTED, reason?)
    └── BulkLessonResult.java          (successCount, failureCount, errors[])
```

**Note:** Blog moderation is exposed via admin `controller/` but delegates to a service in the blog domain. Follow single-responsibility — admin controller just validates auth then delegates.

---

## 5. Lesson CRUD — Full Walkthrough

### 5.1 LessonAdminController (`/api/v1/admin/lessons`)

```java
@RestController
@RequestMapping("/api/v1/admin/lessons")
@RequiredArgsConstructor
public class LessonAdminController {
    private final LessonAdminService lessonAdminService;
    private final JwtService jwtService;

    private UUID extractUserId(String authHeader) {
        // same pattern as GamificationController
    }
}
```

**Endpoints:**

| Method | Path | Body | Returns | Notes |
|--------|------|------|---------|-------|
| `POST` | `/api/v1/admin/lessons` | `LessonCreateRequest` | `LessonResponse` (201) | Creates lesson + exercises |
| `PUT` | `/api/v1/admin/lessons/{id}` | `LessonUpdateRequest` | `LessonResponse` | Updates lesson, replaces exercises |
| `DELETE` | `/api/v1/admin/lessons/{id}` | — | 204 No Content | Deletes lesson (CASCADE deletes exercises) |
| `POST` | `/api/v1/admin/lessons/bulk` | `List<LessonCreateRequest>` | `BulkLessonResult` | Bulk upload (JSON) |

### 5.2 DTOs

**LessonCreateRequest.java:**
```java
@Getter @Setter
public class LessonCreateRequest {
    @NotBlank String title;
    String description;
    @NotNull DifficultyLevel difficulty;
    @Min(0) int baseXp = 10;
    UUID categoryId;                          // nullable
    @NotEmpty List<ExerciseRequest> exercises;
}
```

**ExerciseRequest.java:**
```java
@Getter @Setter
public class ExerciseRequest {
    @NotBlank String question;
    @NotBlank String answerKey;
    @NotNull DifficultyLevel difficulty;
    @Min(1) int points = 5;
}
```

**LessonResponse.java:**
```java
@Getter @Setter
public class LessonResponse {
    UUID id;
    String title;
    String description;
    String difficulty;
    int baseXp;
    UUID categoryId;
    List<ExerciseResponse> exercises;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
```

### 5.3 LessonAdminService Logic

**Create lesson:**
```
1. Validate categoryId exists if provided (fetch Category or throw)
2. Build Lesson entity
3. Save lesson (flushes to get ID)
4. Build Exercise entities from request, associate with lesson
5. Save all exercises
6. Return assembled LessonResponse
```

Key pattern — create exercises *after* saving the lesson so we have the lesson ID:
```java
@Transactional
public LessonResponse create(LessonCreateRequest req) {
    Category category = null;
    if (req.getCategoryId() != null) {
        category = categoryRepository.findById(req.getCategoryId())
            .orElseThrow(() -> new IllegalArgumentException("Category not found: " + req.getCategoryId()));
    }

    Lesson lesson = new Lesson();
    lesson.setTitle(req.getTitle());
    lesson.setDescription(req.getDescription());
    lesson.setDifficulty(req.getDifficulty());
    lesson.setBaseXp(req.getBaseXp());
    lesson.setCategory(category);
    lesson = lessonRepository.save(lesson);

    List<Exercise> exercises = req.getExercises().stream().map(er -> {
        Exercise e = new Exercise();
        e.setLesson(lesson);
        e.setQuestion(er.getQuestion());
        e.setAnswerKey(er.getAnswerKey());
        e.setDifficulty(er.getDifficulty());
        e.setPoints(er.getPoints());
        return e;
    }).toList();
    exerciseRepository.saveAll(exercises);

    return toResponse(lesson, exercises);
}
```

**Update lesson:**
```
1. Fetch existing lesson or throw 404
2. Update fields that are provided (null-aware: only set if not null in request)
3. Delete all existing exercises for this lesson
4. Create new exercises from request
5. Save and return
```

**Delete lesson:**
```
1. Fetch lesson or throw 404
2. Delete (CASCADE handles exercises)
3. Return 204
```

**Bulk upload:**
```
1. Iterate through each LessonCreateRequest
2. Try to create each one, collect successes and failures
3. Return BulkLessonResult with counts
```

---

## 6. Category CRUD — Full Walkthrough

### 6.1 CategoryAdminController (`/api/v1/admin/categories`)

```java
@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
public class CategoryAdminController {
    private final CategoryAdminService categoryAdminService;
    private final JwtService jwtService;
}
```

**Endpoints:**

| Method | Path | Body | Returns | Notes |
|--------|------|------|---------|-------|
| `POST` | `/api/v1/admin/categories` | `CategoryRequest` | `CategoryResponse` (201) | Create category |
| `PUT` | `/api/v1/admin/categories/{id}` | `CategoryRequest` | `CategoryResponse` | Update category |
| `DELETE` | `/api/v1/admin/categories/{id}` | — | 204 No Content | Delete category |

### 6.2 DTOs

**CategoryRequest.java:**
```java
@Getter @Setter
public class CategoryRequest {
    @NotBlank String name;
    @NotBlank String slug;            // URL-friendly, e.g. "computer-science"
    String description;
    String iconUrl;
}
```

**CategoryResponse.java:**
```java
@Getter @Setter
public class CategoryResponse {
    UUID id;
    String name;
    String slug;
    String description;
    String iconUrl;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
```

### 6.3 CategoryAdminService Logic

**Create:**
```
1. Check name uniqueness — throw 400 if taken
2. Check slug uniqueness — throw 400 if taken
3. Build entity, save, return response
```

**Update:**
```
1. Fetch existing or throw 404
2. Check name uniqueness if changed (exclude self)
3. Check slug uniqueness if changed (exclude self)
4. Update fields, save, return
```

**Delete:**
```
1. Fetch existing or throw 404
2. Check if any lessons reference this category (ON DELETE SET NULL — safe, but warn)
3. Delete
4. Return 204
```

---

## 7. Blog Moderation — Full Walkthrough

### 7.1 Endpoint Design

The `PATCH /api/v1/admin/blog/{id}/status` endpoint actually lives in the blog domain's controller but is exposed under admin prefix. Two approaches:

**Approach A (Recommended — follows build guide pattern):** Admin controller delegates to blog service.

```java
// In admin/controller/BlogAdminController.java
@RestController
@RequestMapping("/api/v1/admin/blog")
@RequiredArgsConstructor
public class BlogAdminController {
    private final BlogModerationService blogModerationService;
    private final JwtService jwtService;

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<BlogPostResponse> moderate(
            @PathVariable UUID id,
            @RequestBody @Valid ModerationRequest request,
            @RequestHeader("Authorization") String authHeader) {
        UUID moderatorId = extractUserId(authHeader);
        return ResponseEntity.ok(blogModerationService.moderate(id, request.getStatus(), moderatorId));
    }
}
```

### 7.2 ModerationRequest DTO

```java
@Getter @Setter
public class ModerationRequest {
    @NotNull String status;       // "APPROVED" or "REJECTED"
    String reason;                // optional — why rejected
}
```

### 7.3 BlogModerationService Logic

This service goes in `com.riksa.blog.service.BlogModerationService`:

```
1. Fetch blog post by id or throw 404
2. If not PENDING → throw 400 "Post already moderated"
3. Set status to APPROVED or REJECTED
4. Save
5. Publish BlogApprovedEvent or BlogRejectedEvent (for AI context update)
6. Return updated BlogPostResponse
```

---

## 8. User Role Management

### 8.1 Endpoint

`POST /api/v1/admin/users/{id}/role`

This is defined in the build guide but not in the original 3 (lessons, categories, moderation). Include it as a bonus.

```java
// In admin/controller/UserAdminController.java
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class UserAdminController {
    private final UserAdminService userAdminService;
    private final JwtService jwtService;
}
```

**DTO:**
```java
@Getter @Setter
public class RoleUpdateRequest {
    @NotNull String role;    // "MODERATOR" or "ADMIN"
}
```

**Logic:**
```
1. Fetch user or throw 404
2. Validate new role (can only promote — MODERATOR can't demote ADMIN, etc.)
3. Set role on user entity
4. Save
5. Return updated user profile
```

---

## 9. Security & Authorization

### 9.1 @PreAuthorize Setup

The build guide says `@PreAuthorize("hasRole('MODERATOR')")`. But looking at the `SecurityConfig`, there's no `@EnableMethodSecurity` annotation. We need to add it:

```java
// In SecurityConfig.java — add this import and annotation
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity     // ← ADD THIS
@RequiredArgsConstructor
public class SecurityConfig {
    // ... rest stays the same
}
```

Then on each controller class or method:

```java
@RestController
@RequestMapping("/api/v1/admin/lessons")
@PreAuthorize("hasRole('MODERATOR')")     // protects ALL endpoints in this controller
@RequiredArgsConstructor
public class LessonAdminController {
    // ...
}
```

### 9.2 How Role Check Works

1. `JwtAuthenticationFilter` extracts `role` claim from JWT → sets `SimpleGrantedAuthority("ROLE_MODERATOR")`
2. `@PreAuthorize("hasRole('MODERATOR')")` checks if the authentication has `ROLE_MODERATOR` authority
3. Regular `USER` role tokens get `ROLE_USER` → access denied

### 9.3 Role Hierarchy (Optional)

If MODERATOR should inherit all USER permissions and ADMIN should inherit all MODERATOR permissions, add a `RoleHierarchy` bean. Ask the team if they want this — otherwise each endpoint needs explicit role checks.

---

## 10. Files to Create — Complete Inventory

### Phase 1 — Domain Entities & Repositories (Foundation)

| # | File | Package | Priority |
|---|------|---------|----------|
| 1 | `Lesson.java` | `com.riksa.learning.entity` | **HIGH** — must exist before admin CRUD |
| 2 | `Exercise.java` | `com.riksa.learning.entity` | **HIGH** |
| 3 | `LessonRepository.java` | `com.riksa.learning.repository` | **HIGH** |
| 4 | `ExerciseRepository.java` | `com.riksa.learning.repository` | **HIGH** |
| 5 | `Category.java` | `com.riksa.social.entity` | **HIGH** |
| 6 | `CategoryRepository.java` | `com.riksa.social.repository` | **HIGH** |
| 7 | `BlogPost.java` | `com.riksa.blog.entity` | **HIGH** |
| 8 | `BlogPostRepository.java` | `com.riksa.blog.repository` | **HIGH** |
| 9 | `BlogStatus.java` | `com.riksa.common.enums` (new) | **HIGH** |

### Phase 2 — Admin DTOs

| # | File | Notes |
|---|------|-------|
| 10 | `LessonCreateRequest.java` | `com.riksa.admin.dto` |
| 11 | `LessonUpdateRequest.java` | `com.riksa.admin.dto` |
| 12 | `LessonResponse.java` | `com.riksa.admin.dto` |
| 13 | `ExerciseRequest.java` | `com.riksa.admin.dto` |
| 14 | `ExerciseResponse.java` | `com.riksa.admin.dto` |
| 15 | `CategoryRequest.java` | `com.riksa.admin.dto` |
| 16 | `CategoryResponse.java` | `com.riksa.admin.dto` |
| 17 | `ModerationRequest.java` | `com.riksa.admin.dto` |
| 18 | `BulkLessonResult.java` | `com.riksa.admin.dto` |

### Phase 3 — Admin Services

| # | File | Notes |
|---|------|-------|
| 19 | `LessonAdminService.java` | `com.riksa.admin.service` |
| 20 | `CategoryAdminService.java` | `com.riksa.admin.service` |

### Phase 4 — Blog Moderation Service (in blog domain)

| # | File | Notes |
|---|------|-------|
| 21 | `BlogModerationService.java` | `com.riksa.blog.service` |

### Phase 5 — Admin Controllers

| # | File | Notes |
|---|------|-------|
| 22 | `LessonAdminController.java` | `com.riksa.admin.controller` |
| 23 | `CategoryAdminController.java` | `com.riksa.admin.controller` |
| 24 | `BlogAdminController.java` | `com.riksa.admin.controller` |

### Phase 6 — Config Change

| # | File | Change |
|---|------|--------|
| 25 | `SecurityConfig.java` | Add `@EnableMethodSecurity` |

---

## 11. Step-by-Step Team Plan

This work can be split across 2 developers in parallel, with a shared foundation task.

### Step 0 — Foundation (½ day) — Any Dev

Create all domain entities and repositories:

1. Create `BlogStatus.java` enum in `common/enums/`
2. Rewrite `Lesson.java` in `learning/entity/` with full schema mapping (title, description, category, timestamps)
3. Create `Exercise.java` in `learning/entity/` with `@ManyToOne Lesson`
4. Create `LessonRepository.java` in `learning/repository/`
5. Create `ExerciseRepository.java` in `learning/repository/`
6. Create `Category.java` in `social/entity/`
7. Create `CategoryRepository.java` in `social/repository/`
8. Create `BlogPost.java` in `blog/entity/`
9. Create `BlogPostRepository.java` in `blog/repository/`
10. Add `@EnableMethodSecurity` to `SecurityConfig.java`

**Verify:** `./mvnw clean install -DskipTests` compiles.

### Step 1 — Lesson CRUD (1 day) — Dev A

Admin DTOs + Service + Controller for lessons:

1. `LessonCreateRequest.java`, `LessonUpdateRequest.java`, `ExerciseRequest.java`
2. `LessonResponse.java`, `ExerciseResponse.java`
3. `BulkLessonResult.java`
4. `LessonAdminService.java` — create (with nested exercises), update (replace exercises), delete, bulk
5. `LessonAdminController.java` — POST, PUT, DELETE, POST bulk

**Verify:** Start app, create a lesson via curl:
```bash
curl -X POST http://localhost:8080/api/v1/admin/lessons \
  -H "Authorization: Bearer $MOD_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Intro to Programming",
    "description": "Learn the basics",
    "difficulty": "EASY",
    "baseXp": 50,
    "exercises": [
      {"question": "What is a variable?", "answerKey": "A storage location", "difficulty": "EASY", "points": 5}
    ]
  }'
```

### Step 2 — Category CRUD (1 day) — Dev B

Admin DTOs + Service + Controller for categories:

1. `CategoryRequest.java`, `CategoryResponse.java`
2. `CategoryAdminService.java` — create (uniqueness checks), update, delete
3. `CategoryAdminController.java` — POST, PUT, DELETE

**Verify:**
```bash
curl -X POST http://localhost:8080/api/v1/admin/categories \
  -H "Authorization: Bearer $MOD_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Computer Science", "slug": "computer-science", "description": "CS topics"}'
```

### Step 3 — Blog Moderation + Role (1 day) — Dev A

1. `ModerationRequest.java` (dto)
2. `BlogModerationService.java` in `blog/service/` — moderate(PENDING → APPROVED/REJECTED), publish events
3. `BlogAdminController.java` — PATCH `/api/v1/admin/blog/{id}/status`
4. `UserAdminController.java` + `UserAdminService.java` — POST `/api/v1/admin/users/{id}/role`

**Verify:**
```bash
curl -X PATCH http://localhost:8080/api/v1/admin/blog/$BLOG_ID/status \
  -H "Authorization: Bearer $MOD_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "APPROVED"}'
```

### Step 4 — Security + Polish (½ day) — Both Devs

- Double-check `@PreAuthorize("hasRole('MODERATOR')")` on all admin controllers
- Test that USER role gets 403 on admin endpoints
- Add `@Valid` annotations on request bodies
- Ensure proper HTTP status codes (201 for create, 204 for delete)
- Add integration tests

---

## 12. Testing Strategy

### Unit Tests

| Test | Class | What to Test |
|------|-------|-------------|
| Create lesson with 3 exercises | `LessonAdminServiceTest` | All exercises persisted, lesson has correct fields |
| Update lesson replaces exercises | `LessonAdminServiceTest` | Old exercises gone, new ones present |
| Delete lesson cascades | `LessonAdminServiceTest` | Exercises deleted, lesson gone |
| Create category duplicate name | `CategoryAdminServiceTest` | Throws IllegalArgumentException |
| Create category duplicate slug | `CategoryAdminServiceTest` | Throws IllegalArgumentException |
| Moderate approved blog | `BlogModerationServiceTest` | Status changes to APPROVED |
| Moderate already moderated blog | `BlogModerationServiceTest` | Throws 400 |

### Integration Tests

| Test | What It Covers |
|------|---------------|
| Admin creates lesson → user can see it via GET /lessons | Full CRUD flow |
| Admin creates category → user can list it via GET /categories | Category visibility |
| Admin approves blog → blog appears in GET /blog | Moderation flow |
| User tries admin endpoint → 403 | Security enforcement |

### How to Run Tests

```bash
./mvnw test
./mvnw test -Dtest=LessonAdminServiceTest
```

---

## 13. Common Pitfalls & Rules

### DO

- Use `@Transactional` on service methods that do multiple writes
- Use `@Valid` on request bodies in controllers
- Return 201 with `ResponseEntity.created(uri)` for creates
- Return 204 for deletes
- Use `@PreAuthorize("hasRole('MODERATOR')")` at the class level for all admin controllers
- Always check entity existence before operating on it (throw `IllegalArgumentException` with clear message)
- Use `@ManyToOne(fetch = FetchType.LAZY)` for entity relationships

### DON'T

- ❌ Don't put entities in the admin package — they belong to their domain
- ❌ Don't bypass `@PreAuthorize` — security is enforced at the controller level
- ❌ Don't use raw SQL — use JPA repositories
- ❌ Don't forget `@EnableMethodSecurity` in `SecurityConfig` — `@PreAuthorize` won't work without it
- ❌ Don't add `@Version` or optimistic locking to Lesson/Exercise/Category — those are admin-only writes, no contention
- ❌ Don't use `@CreationTimestamp` / `@UpdateTimestamp` — the DB triggers handle `updated_at`; use `OffsetDateTime` with `@PreUpdate` or let DB handle it

### Schema Ownership Check

| Endpoint | Reads From | Writes To | Owned By |
|----------|-----------|-----------|----------|
| `POST/PUT/DELETE /admin/lessons` | `lessons`, `exercises` | `lessons`, `exercises` | **Admin** |
| `POST/PUT/DELETE /admin/categories` | `categories` | `categories` | **Admin** |
| `PATCH /admin/blog/{id}/status` | `blog_posts` | `blog_posts` | **Blog** (admin delegates) |
| `POST /admin/users/{id}/role` | `users` | `users` | **Auth** (admin delegates) |

**Key rule:** Admin can write to `lessons`, `exercises`, and `categories` directly. For `blog_posts` and `users`, it should delegate to the owning domain's service.

---

*This guide references the RIKSA_BUILD_GUIDE.md at `/docs/RIKSA_BUILD_GUIDE.md`. Stick to the patterns already established in the codebase: `@RequiredArgsConstructor`, `JwtService.extractUserId()`, `@PreAuthorize`, and response entity patterns.*
