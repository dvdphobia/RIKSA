# Learning API — Implementation Plan (Updated)

## Learning Model: Read → Practice → Reflect

A lesson is created by admin. A student reads it, practices exercises, then writes a reflection. The lesson is **complete** only when all exercises are correct and a reflection is submitted.

```
📚 Lesson (created by admin)
├── title: "Limits — Chapter 1"
├── description: short paragraph explaining the concept
├── reflectionPrompt: "Explain what limits mean in your own words"
├── difficulty: EASY / MEDIUM / HARD / DIFFICULT
├── baseXp: 10
├── categoryId: Math
└── exercises[] (created by admin alongside lesson)
    ├── question: "What is lim(x→2) of x²?"
    ├── answerKey: "4"           (exact match for STEM)
    └── points: 10               (bonus XP for correct answer)

📝 Student flow:
1. Read the lesson paragraph
2. Practice exercises — correct = bonus XP, wrong = retry
3. Write reflection on the prompt
4. All exercises correct + reflection submitted → LESSON COMPLETE
5. → LessonCompletedEvent → XP + streak + rank + achievements
```

---

## XP Calculation

| Component | Source | Formula |
|-----------|--------|---------|
| Base XP | `lesson.baseXp` | Earned when lesson marked complete |
| Exercise XP | `exercise.points` per correct answer | Sum of points for all correct exercises |
| **Total XP** | | `baseXp + sum(correct_exercise.points)` |

Example: Lesson baseXp=10, 3 exercises worth 5/10/15 points
- All 3 correct: 10 + 5 + 10 + 15 = **40 XP**
- 2 of 3 correct: Lesson not complete yet. User must retry the wrong one.

---

## Reflection Validation

**V1 (now):** `ReflectionValidator` interface + `SimpleReflectionValidator` (min 20 chars, non-blank)

**V2 (later):** `AiReflectionValidator` calls the AI module to evaluate understanding. Swappable via Spring profile or feature flag.

```java
public interface ReflectionValidator {
    boolean isValid(String reflectionPrompt, String userReflection);
}

@Component
public class SimpleReflectionValidator implements ReflectionValidator {
    public boolean isValid(String prompt, String reflection) {
        return reflection != null && reflection.trim().length() >= 20;
    }
}
```

When the AI chat module is built, a new implementation calls the AI to judge understanding. No other code changes.

---

## Database Changes (Migration V10)

The V1 schema needs these additions:

```sql
-- V10__add_lesson_reflection_and_attempts.sql

-- Add reflection prompt to lessons (admin writes this)
ALTER TABLE "lessons" ADD COLUMN reflection_prompt TEXT;

-- Add reflection text to lesson completions (student writes this)
ALTER TABLE "lesson_completions" ADD COLUMN reflection_text TEXT NOT NULL DEFAULT '';

-- Add score column to lesson completions (percentage of correct exercises)
ALTER TABLE "lesson_completions" ADD COLUMN score INTEGER NOT NULL DEFAULT 0;

-- Exercise attempts tracking (so students can retry wrong answers)
CREATE TABLE "exercise_attempts" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    exercise_id UUID NOT NULL REFERENCES "exercises"(id) ON DELETE CASCADE,
    is_correct BOOLEAN NOT NULL,
    attempted_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE (user_id, exercise_id)  -- only latest attempt matters
);

CREATE INDEX idx_exercise_attempts_user_id ON "exercise_attempts"(user_id);
CREATE INDEX idx_exercise_attempts_exercise_id ON "exercise_attempts"(exercise_id);
```

**Why `exercise_attempts` table?**
- Students can retry wrong exercises multiple times
- We need to know which exercises the user has already gotten correct
- A lesson is "complete" only when all exercises for that lesson have a correct attempt
- `UNIQUE(user_id, exercise_id)` with `ON CONFLICT UPDATE` — only latest attempt stored

**Why `score` in `lesson_completions`?**
- Tracks what percentage of exercises were correct on final submission
- Useful for the student profile and future analytics
- Not used for pass/fail (all must be correct to complete)

**Why `reflection_prompt` is nullable?**
- Existing lessons don't have it. Admin can add it to new lessons.
- If null, reflection step is skipped for that lesson.

---

## Files to Create

### Migration (1)
```
db/migration/
└── V10__add_lesson_reflection_and_attempts.sql
```

### Entities (3 — Lesson already exists)
```
com.riksa.learning/entity/
├── Lesson.java              ✅ EXISTS (needs reflectionPrompt field added)
├── Exercise.java             NEW
├── LessonCompletion.java     NEW
└── ExerciseAttempt.java      NEW
```

### Repositories (4)
```
com.riksa.learning/repository/
├── LessonRepository.java
│   ├── findByCategoryId(UUID, Pageable)
│   ├── findByDifficulty(DifficultyLevel, Pageable)
│   └── findByCategoryIdAndDifficulty(UUID, DifficultyLevel, Pageable)
│
├── ExerciseRepository.java
│   └── findByLessonId(UUID) → List<Exercise>
│
├── LessonCompletionRepository.java
│   ├── findByUserId(UUID) → List<LessonCompletion>
│   ├── existsByUserIdAndLessonId(UUID, UUID) → boolean
│   └── findByUserIdAndLessonId(UUID, UUID) → Optional<LessonCompletion>
│
└── ExerciseAttemptRepository.java
    ├── findByUserIdAndExerciseId(UUID, UUID) → Optional<ExerciseAttempt>
    ├── findByUserIdAndLessonId(UUID, UUID) → List<ExerciseAttempt>
    └── countByUserIdAndLessonIdAndIsCorrectTrue(UUID, UUID) → int
```

### DTOs (7)
```
com.riksa.learning/dto/
├── LessonListResponse.java       → id, title, description, difficulty, baseXp, categoryId, completed, reflectionPrompt
├── LessonDetailResponse.java     → full lesson + exercises (NO answerKey) + completion status
├── ExerciseResponse.java        → id, question, difficulty, points (NO answerKey)
├── CompletionRequest.java        → { exerciseAnswers: [{exerciseId, answer}], reflectionText }
├── CompletionResponse.java      → { completionId, lessonId, xpEarned, score, passed }
├── AnswerResult.java            → { exerciseId, correct }
└── ExerciseAttemptRequest.java  → { exerciseId, answer }  (for retrying a single exercise)
```

### Services (3)
```
com.riksa.learning/service/
├── LessonService.java              → catalog queries, filtering, completion status
├── LessonCompletionService.java   → complete lesson: validate exercises, reflection, calc XP, publish event
└── ExerciseAttemptService.java    → submit individual exercise answer, return correct/incorrect
```

### Validation (1)
```
com.riksa.learning/validation/
└── ReflectionValidator.java        → interface
└── SimpleReflectionValidator.java  → V1: min 20 chars
```

### Controller (1)
```
com.riksa.learning/controller/
└── LessonController.java
    GET  /api/v1/lessons?categoryId=&difficulty=&page=0&size=20   [AUTH]
    GET  /api/v1/lessons/{id}                                      [AUTH]
    POST /api/v1/lessons/{id}/exercises/{exerciseId}/attempt       [AUTH]
         → Submit one exercise answer, get correct/incorrect + XP preview
    POST /api/v1/lessons/{id}/complete                              [AUTH]
         → Submit reflection + finalize (all exercises must be correct first)
```

### Admin Controller (1) — data model only, teammate builds logic
```
com.riksa.learning/dto/
├── LessonCreateRequest.java   → title, description, reflectionPrompt, difficulty, baseXp, categoryId, exercises[]
├── LessonUpdateRequest.java   → same fields, all optional
└── ExerciseCreateRequest.java → question, answerKey, difficulty, points
```

Admin endpoints are defined here but implementation is for your dev team:
```
POST   /api/v1/admin/lessons              → Create lesson + exercises
PUT    /api/v1/admin/lessons/{id}         → Update lesson
DELETE /api/v1/admin/lessons/{id}         → Delete lesson
POST   /api/v1/admin/lessons/bulk         → Bulk upload
```

---

## Service Logic

### LessonCompletionService.completeLesson()
```
1. Check user hasn't already completed this lesson → 409 if exists
2. Load lesson (with exercises)
3. Check all exercises have correct attempts for this user → 400 if not all correct
4. Validate reflection text via ReflectionValidator → 400 if invalid
5. Calculate xpEarned = lesson.baseXp + sum(exercise.points where correct)
6. Insert lesson_completions row (userId, lessonId, xpEarned, score, reflectionText)
7. Publish LessonCompletedEvent(this, userId, lessonId, xpEarned)
8. Return CompletionResponse
```

### ExerciseAttemptService.attemptExercise()
```
1. Load exercise by ID
2. Compare user answer (trimmed, case-insensitive) against answerKey
3. Upsert exercise_attempts row (user_id, exercise_id, is_correct)
4. Return { exerciseId, correct, pointsIfCorrect }
```

Note: This endpoint does NOT award XP or publish events. XP is only awarded when the lesson is completed as a whole. This endpoint just tracks progress.

---

## Event Flow (End-to-End)

```
POST /api/v1/lessons/{id}/complete
  │
  ▼
LessonCompletionService.completeLesson()
  ├── Validate all exercises correct
  ├── Validate reflection text
  ├── Calculate xpEarned
  ├── Insert lesson_completions row
  └── Publish LessonCompletedEvent(this, userId, lessonId, xpEarned)
        │
        ├── XpLedgerListener → XpLedgerService.awardXp()
        │     └── Publish XpGainedEvent
        │           ├── RankListener → RankService.checkAndPromoteRank()
        │           │     └── RankChangedEvent (if tier changed)
        │           │           └── AchievementListener → check rank achievements
        │           └── AchievementListener → check XP achievements
        │
        └── StreakListener → StreakService.updateStreak()
              └── StreakUpdatedEvent
                    └── AchievementListener → check streak achievements
```

---

## Execution Order

| Step | What | Why |
|------|------|-----|
| 1 | V10 migration | Add reflection_prompt, score, exercise_attempts table |
| 2 | Update Lesson entity + create Exercise, LessonCompletion, ExerciseAttempt entities | Map to DB |
| 3 | Repositories (4) | JPA queries |
| 4 | DTOs (7) | Request/response contracts |
| 5 | ReflectionValidator interface + SimpleReflectionValidator | V1 implementation |
| 6 | ExerciseAttemptService | Submit individual exercise answers |
| 7 | LessonService (read) | Lesson listing + detail |
| 8 | LessonCompletionService (write) | Complete lesson, publish event |
| 9 | LessonController | Wire to endpoints |
| 10 | Unit tests | Service + completion logic |

---

## What's NOT in This Build (Future Work)

- **AI reflection validation** — Will be `AiReflectionValidator` when chat module is built
- **Admin lesson CRUD logic** — Data model (DTOs, entities) is here, controller/service for admin is for your teammate
- **Lesson ordering/prerequisites** — Not in scope yet
- **Category filtering with join** — Can be added later
- **Bulk lesson upload** — Admin feature, not blocking