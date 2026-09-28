ALTER TABLE "achievements"
    ADD COLUMN IF NOT EXISTS "active" BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE "xp_boosts" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    multiplier NUMERIC(5, 2) NOT NULL CHECK (multiplier >= 1 AND multiplier <= 10),
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    created_by UUID REFERENCES "users"(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT xp_boosts_valid_window CHECK (ends_at > starts_at)
);

CREATE INDEX idx_xp_boosts_window ON "xp_boosts" (starts_at, ends_at);

CREATE TABLE "app_settings" (
    key TEXT PRIMARY KEY,
    value JSONB NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES "users"(id) ON DELETE SET NULL
);
