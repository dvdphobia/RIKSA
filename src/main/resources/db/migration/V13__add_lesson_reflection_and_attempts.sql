ALTER TABLE "lessons" ADD COLUMN IF NOT EXISTS reflection_prompt TEXT;
ALTER TABLE "lesson_completions" ADD COLUMN IF NOT EXISTS reflection_text TEXT NOT NULL DEFAULT '';
ALTER TABLE "lesson_completions" ADD COLUMN IF NOT EXISTS score INTEGER NOT NULL DEFAULT 0;
CREATE TABLE IF NOT EXISTS "exercise_attempts" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    exercise_id UUID NOT NULL REFERENCES "exercises"(id) ON DELETE CASCADE,
    is_correct BOOLEAN NOT NULL,
    attempted_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE (user_id, exercise_id)
);
CREATE INDEX IF NOT EXISTS idx_exercise_attempts_user_id ON "exercise_attempts"(user_id);
CREATE INDEX IF NOT EXISTS idx_exercise_attempts_exercise_id ON "exercise_attempts"(exercise_id);