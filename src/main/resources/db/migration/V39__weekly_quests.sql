CREATE TABLE weekly_quest (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    week_start DATE NOT NULL,
    type VARCHAR(32) NOT NULL CHECK (type IN (
        'WEEKLY_LESSONS', 'WEEKLY_CORRECT_ANSWERS', 'WEEKLY_ACTIVE_DAYS', 'WEEKLY_SOCIAL'
    )),
    target INTEGER NOT NULL CHECK (target > 0),
    progress INTEGER NOT NULL DEFAULT 0 CHECK (progress >= 0 AND progress <= target),
    xp_reward INTEGER NOT NULL CHECK (xp_reward > 0),
    claimed BOOLEAN NOT NULL DEFAULT FALSE,
    claimed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_weekly_quest_user_week_type UNIQUE (user_id, week_start, type)
);
CREATE INDEX idx_weekly_quest_user_week ON weekly_quest (user_id, week_start);

CREATE TABLE quest_chest (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    week_start DATE NOT NULL,
    opened_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_quest_chest_user_week UNIQUE (user_id, week_start)
);
