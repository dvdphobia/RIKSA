CREATE TABLE lesson_resource (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    lesson_id UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(120) NOT NULL CHECK (char_length(title) BETWEEN 3 AND 120),
    url TEXT NOT NULL,
    kind VARCHAR(10) NOT NULL CHECK (kind IN ('LINK', 'PDF', 'VIDEO')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_lesson_resource_lesson_created
    ON lesson_resource (lesson_id, created_at DESC, id DESC);
CREATE INDEX idx_lesson_resource_author_created
    ON lesson_resource (author_id, created_at DESC);

CREATE TABLE lesson_resource_save (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    resource_id UUID NOT NULL REFERENCES lesson_resource(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_lesson_resource_save UNIQUE (resource_id, user_id)
);
CREATE INDEX idx_lesson_resource_save_user ON lesson_resource_save (user_id, created_at DESC);

-- Keeps the 5 XP grant durable across unsave/resave cycles.
CREATE TABLE lesson_resource_save_reward (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    resource_id UUID NOT NULL REFERENCES lesson_resource(id) ON DELETE CASCADE,
    saver_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_lesson_resource_save_reward UNIQUE (resource_id, saver_id)
);

ALTER TABLE lesson_resource ADD COLUMN hidden_at TIMESTAMPTZ;
ALTER TABLE lesson_resource ADD COLUMN hidden_by UUID REFERENCES users(id) ON DELETE SET NULL;
