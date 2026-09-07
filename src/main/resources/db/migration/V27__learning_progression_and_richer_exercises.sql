-- Ordered learning progression and richer exercise metadata.

ALTER TABLE "lessons" ADD COLUMN IF NOT EXISTS position INTEGER;

UPDATE "lessons" SET position = position + 100000 WHERE position IS NOT NULL;

WITH ranked_lessons AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY category_id
               ORDER BY created_at NULLS LAST, id
           ) AS cat_pos
    FROM "lessons"
    WHERE category_id IS NOT NULL
)
UPDATE "lessons" lesson
SET position = ranked_lessons.cat_pos
FROM ranked_lessons
WHERE lesson.id = ranked_lessons.id;

ALTER TABLE "lessons" ALTER COLUMN position SET NOT NULL;
ALTER TABLE "lessons" ADD CONSTRAINT lessons_position_positive CHECK (position > 0);

CREATE UNIQUE INDEX IF NOT EXISTS ux_lessons_category_position
    ON "lessons" (category_id, position)
    WHERE category_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_lessons_uncategorized_position
    ON "lessons" (position)
    WHERE category_id IS NULL;

ALTER TABLE "exercises"
    ADD COLUMN IF NOT EXISTS type VARCHAR(32) NOT NULL DEFAULT 'TEXT',
    ADD COLUMN IF NOT EXISTS options JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS hint TEXT,
    ADD COLUMN IF NOT EXISTS explanation TEXT;

ALTER TABLE "exercise_attempts"
    ADD COLUMN IF NOT EXISTS submitted_answer TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS attempt_number INTEGER;

UPDATE "exercise_attempts"
SET attempt_number = 1
WHERE attempt_number IS NULL;

ALTER TABLE "exercise_attempts" ALTER COLUMN attempt_number SET NOT NULL;
ALTER TABLE "exercise_attempts"
    DROP CONSTRAINT IF EXISTS exercise_attempts_user_id_exercise_id_key;
ALTER TABLE "exercise_attempts"
    ADD CONSTRAINT ux_exercise_attempts_user_exercise_number
    UNIQUE (user_id, exercise_id, attempt_number);

CREATE INDEX IF NOT EXISTS idx_exercise_attempts_user_exercise
    ON "exercise_attempts" (user_id, exercise_id, attempted_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS ux_xp_transactions_source
    ON "xp_transactions" (user_id, source_type, source_id)
    WHERE source_id IS NOT NULL;
