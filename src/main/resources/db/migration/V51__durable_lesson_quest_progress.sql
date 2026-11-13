CREATE TABLE quest_lesson_progress_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    lesson_id UUID NOT NULL REFERENCES "lessons"(id) ON DELETE CASCADE,
    completed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_quest_lesson_progress_user_lesson UNIQUE (user_id, lesson_id)
);

CREATE INDEX idx_quest_lesson_progress_completed_at
    ON quest_lesson_progress_events (completed_at);
