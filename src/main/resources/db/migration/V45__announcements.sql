CREATE TABLE "announcements" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT announcements_valid_window CHECK (ends_at IS NULL OR ends_at > starts_at)
);

CREATE INDEX idx_announcements_created_at ON "announcements" (created_at DESC);
CREATE INDEX idx_announcements_window ON "announcements" (starts_at, ends_at);
