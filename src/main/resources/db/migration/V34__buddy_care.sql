CREATE TABLE buddy (
    user_id UUID PRIMARY KEY REFERENCES "users"(id) ON DELETE CASCADE,
    species VARCHAR(20) NOT NULL CHECK (species IN ('crocodile', 'capybara')),
    name VARCHAR(16) NOT NULL,
    energy SMALLINT NOT NULL DEFAULT 60 CHECK (energy BETWEEN 0 AND 100),
    bond INTEGER NOT NULL DEFAULT 0 CHECK (bond >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE buddy_care_event (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    source_key VARCHAR(20) NOT NULL,
    source_id UUID NOT NULL,
    care_day DATE NOT NULL,
    energy_gain SMALLINT NOT NULL,
    bond_gain SMALLINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_buddy_care_source UNIQUE (user_id, source_key, source_id)
);

CREATE INDEX idx_buddy_care_event_user_day
    ON buddy_care_event (user_id, care_day);
