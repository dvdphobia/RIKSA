# Learning Spring Boot — Guide for Building the RIKSA Learning API

You are building the Learning module while learning Spring Boot. This guide covers the essentials you need, using your own project as the reference implementation.

---

## 1. The 5 Things That Matter Most

### 1.1 Annotations Are the Language

Everything in Spring Boot is annotations. Here are the ones you'll use 90% of the time:

| Annotation | What It Does | Where You'll Use It |
|---|---|---|
| `@RestController` | Makes a class handle HTTP requests | Every controller |
| `@Service` | Marks a business logic class | Every service |
| `@Repository` | Marks a DB access class | Every repository (JPA does this for you with extends) |
| `@RequiredArgsConstructor` | Generates a constructor for `final` fields | Every class with dependencies |
| `@GetMapping` | Maps GET requests to a method | Read endpoints |
| `@PostMapping` | Maps POST requests to a method | Create/submit endpoints |
| `@PathVariable` | Extracts `{id}` from the URL | `GET /lessons/{id}` |
| `@RequestParam` | Extracts `?page=0&size=20` from URL | List/filter endpoints |
| `@RequestHeader` | Gets the `Authorization` header | JWT extraction |
| `@RequestBody` | Maps JSON body to a Java object | POST request bodies |

### 1.2 The 4-Layer Pattern (Copy This Everywhere)

Every feature follows the same structure:

```
Controller  →  Service  →  Repository  →  Entity
   HTTP          Logic       DB query       Table
```

**Never skip a layer:**
- Controller never touches Repository directly
- Service never returns an Entity to the client — always convert to a DTO
- Each layer has one job

### 1.3 Lombok Saves You from Boilerplate

```java
@Getter @Setter          // generates getX() and setX() for every field
@NoArgsConstructor       // generates constructor with no args (JPA needs this)
@RequiredArgsConstructor // generates constructor for all final fields (dependency injection)
```

**Every entity gets:** `@Getter @Setter @NoArgsConstructor`
**Every service/controller gets:** `@RequiredArgsConstructor`

### 1.4 The JWT Pattern (Copy-Paste)

Every authenticated endpoint extracts the user ID from the JWT header. Your gamification controller already does this — copy it:

```java
private UUID extractUserId(String authHeader) {
    String token = authHeader.replace("Bearer ", "");
    return jwtService.extractUserId(token);
}
```

You'll put this exact method in `LessonController`.

### 1.5 How to Debug

| Command | What It Does |
|---|---|
| `./mvnw clean compile` | Compile only — fast check for syntax errors |
| `./mvnw test -Dtest=YourTest` | Run one specific test |
| `./mvnw spring-boot:run` | Start the app locally |
| `@Slf4j` + `log.info("userId={}", userId)` | Add logging when confused |

**Read error messages top to bottom.** The FIRST error line is always the real problem. The rest are cascading failures.

---

## 2. The Golden Rule

**Never modify another domain's tables.**

| If you need to... | Do this |
|---|---|
| Award XP when a lesson is completed | Publish `LessonCompletedEvent` → Gamification listens and writes XP |
| Read user data (name, province) | Inject `UserRepository` and call `findById()` |
| Read gamification data (streak, rank) | Inject `UserGamificationRepository` and call `findByUserId()` |
| Write to `lesson_completions` | ✅ This is your table — write freely |

The event system we built is the bridge. Learning publishes, Gamification consumes. No direct writes across domains.

---

## 3. Build Order (One Day at a Time)

### Day 1: Entities + Repositories

**Goal:** Map database tables to Java classes and create Spring Data JPA repositories.

**What to learn:**
- `@Entity` maps a class to a database table
- `@Table(name = "table_name")` specifies the exact table name
- `@Column(name = "column_name")` maps a field to a column
- `@Enumerated(EnumType.STRING)` for enum columns
- `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` for PostgreSQL custom enum types
- `JpaRepository<Entity, IdType>` gives you `save()`, `findById()`, `findAll()`, `delete()` for free

**Reference:** Look at `gamification/entity/UserGamification.java` and `gamification/repository/UserGamificationRepository.java`

**What to build:**
- `Exercise.java` entity (maps to `exercises` table)
- `LessonCompletion.java` entity (maps to `lesson_completions` table)
- `ExerciseAttempt.java` entity (maps to `exercise_attempts` table — new in V10)
- Update `Lesson.java` (add `reflectionPrompt` field)
- `ExerciseRepository.java`
- `LessonRepository.java`
- `LessonCompletionRepository.java`
- `ExerciseAttemptRepository.java`

**Checkpoint:** `./mvnw clean compile` passes

### Day 2: DTOs + Migration

**Goal:** Create request/response objects and the database migration.

**What to learn:**
- DTOs are plain Java objects (POJOs) with `@Getter @Setter @NoArgsConstructor`
- Never expose entities directly to the client — always map to DTOs
- Flyway migrations are just SQL files named `V{number}__description.sql`

**Reference:** Look at `gamification/dto/GamificationProfileResponse.java`

**What to build:**
- `V10__add_lesson_reflection_and_attempts.sql` migration
- `LessonListResponse.java`
- `LessonDetailResponse.java`
- `ExerciseResponse.java` (NO answerKey field)
- `CompletionRequest.java`
- `CompletionResponse.java`
- `ExerciseAttemptRequest.java`
- `AnswerResult.java`
- `ReflectionValidator.java` (interface)
- `SimpleReflectionValidator.java` (V1: min 20 chars)

**Checkpoint:** `./mvnw clean compile` passes

### Day 3: LessonService (Read-Only)

**Goal:** Build the lesson catalog — list lessons with filters, show lesson detail.

**What to learn:**
- `@Service` marks a business logic class
- `@Transactional(readOnly = true)` for read-only methods (performance optimization)
- `Pageable` and `Page<T>` for pagination (Spring Data gives this to you)
- Repository methods like `findByCategoryId()` are auto-generated by Spring from the method name

**Reference:** Look at `gamification/service/GamificationService.java` — `getProfile()`, `getLeaderboard()`, `getXpHistory()`

**What to build:**
- `LessonService.listLessons(userId, categoryId, difficulty, page, size)` — returns paginated list with `completed` flag per lesson
- `LessonService.getLessonDetail(userId, lessonId)` — returns lesson + exercises (without answerKey) + completion status

**Checkpoint:** Write a simple test that calls `listLessens()` with mock repositories

### Day 4: ExerciseAttemptService + LessonCompletionService (Write Logic)

**Goal:** The hot path — submitting exercise answers and completing lessons.

**What to learn:**
- `@Transactional` (without readOnly) for write methods
- `Optional<T>` and how to handle "not found" cases
- `EventPublisher.publish()` to trigger the gamification pipeline
- `@PrePersist` / `@PreUpdate` for auto-setting timestamps

**Reference:** Look at `gamification/service/XpLedgerService.java` — it's the same pattern: lock row → modify → save → publish event

**What to build:**
- `ExerciseAttemptService.attemptExercise(userId, exerciseId, answer)` — compare answer to answerKey, upsert attempt, return correct/incorrect
- `LessonCompletionService.completeLesson(userId, lessonId, CompletionRequest)` — validate all exercises correct, validate reflection, calculate XP, insert completion, publish `LessonCompletedEvent`

**Checkpoint:** Write tests for: correct answer, wrong answer, complete lesson with all exercises correct, complete lesson with missing exercises

### Day 5: LessonController + Manual Testing

**Goal:** Wire everything to HTTP endpoints and test with real requests.

**What to learn:**
- `@RestController` + `@RequestMapping("/api/v1/lessons")`
- `@GetMapping`, `@PostMapping`
- `@PathVariable UUID id` extracts from URL
- `@RequestParam(defaultValue = "0") int page` extracts from query string
- `@RequestHeader("Authorization") String authHeader` gets the JWT
- `ResponseEntity.ok(body)` returns 200 with JSON

**Reference:** Look at `gamification/controller/GamificationController.java`

**What to build:**
- `LessonController` with 3 endpoints:
  - `GET /api/v1/lessons?categoryId=&difficulty=&page=0&size=20` [AUTH]
  - `GET /api/v1/lessons/{id}` [AUTH]
  - `POST /api/v1/lessons/{id}/exercises/{exerciseId}/attempt` [AUTH]
  - `POST /api/v1/lessons/{id}/complete` [AUTH]

**Checkpoint:** Start the app (`./mvnw spring-boot:run`) and test with curl or Postman

---

## 4. Quick Reference — File Locations

When you're building, open these files as reference:

| What You're Building | Reference File |
|---|---|
| Entity | `gamification/entity/UserGamification.java` |
| Repository | `gamification/repository/UserGamificationRepository.java` |
| DTO | `gamification/dto/GamificationProfileResponse.java` |
| Service (read) | `gamification/service/GamificationService.java` |
| Service (write) | `gamification/service/XpLedgerService.java` |
| Controller | `gamification/controller/GamificationController.java` |
| Event | `gamification/event/XpGainedEvent.java` |
| Listener | `gamification/listener/XpLedgerListener.java` |
| Exception handling | `common/exception/GlobalExceptionHandler.java` |
| Migration | `db/migration/V9__bigint_for_xp_columns.sql` |

---

## 5. Common Mistakes to Avoid

| Mistake | Fix |
|---|---|
| Returning Entity directly from controller | Always map to a DTO first |
| Forgetting `@Transactional` on write methods | Add it, or your changes won't be committed |
| Using `int` for money/XP fields | Use `long` or `BigDecimal` to avoid overflow |
| Writing to another domain's table | Publish an event instead |
| Forgetting `@RequiredArgsConstructor` | Dependencies won't be injected — NullPointerException |
| Putting `answerKey` in exercise response DTO | That would leak answers to students |
| Catching raw `IllegalArgumentException` | Use `ResourceNotFoundException` mapped to 404 |
| Forgetting `@PrePersist` on entities | `createdAt` won't be auto-set |

---

## 6. Testing Pattern

Every service test follows the same structure:

```java
@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

    @Mock private LessonRepository lessonRepository;
    @Mock private LessonCompletionRepository completionRepository;
    @InjectMocks private LessonService lessonService;

    @Test
    void listLessons_returnsPaginatedResults() {
        // Arrange — set up mock data
        when(lessonRepository.findByCategoryId(eq(categoryId), any()))
            .thenReturn(page);

        // Act — call the method
        var result = lessonService.listLessons(userId, categoryId, null, 0, 20);

        // Assert — verify the result
        assertThat(result.getEdges()).hasSize(2);
        verify(lessonRepository).findByCategoryId(eq(categoryId), any());
    }
}
```

Key Mockito methods:
- `when(mock.method(args)).thenReturn(value)` — stub a method
- `verify(mock).method(args)` — verify a method was called
- `any()`, `eq(value)`, `isNull()` — argument matchers
- `assertThat(result).isEqualTo(expected)` — assertions

---

*This guide lives in the project root. Refer back to it as you build each day.*