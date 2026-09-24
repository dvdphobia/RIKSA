CREATE TABLE daily_quest (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    quest_date DATE NOT NULL,
    type VARCHAR(32) NOT NULL CHECK (type IN ('COMPLETE_LESSON', 'CORRECT_ANSWERS', 'SOCIAL_ACTION')),
    target INTEGER NOT NULL CHECK (target > 0),
    progress INTEGER NOT NULL DEFAULT 0 CHECK (progress >= 0 AND progress <= target),
    xp_reward INTEGER NOT NULL CHECK (xp_reward > 0),
    claimed BOOLEAN NOT NULL DEFAULT FALSE,
    claimed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_daily_quest_user_date_type UNIQUE (user_id, quest_date, type)
);
CREATE INDEX idx_daily_quest_user_date ON daily_quest (user_id, quest_date);
