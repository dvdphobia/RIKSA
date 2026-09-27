ALTER TABLE user_gamification
    ADD COLUMN xp_balance BIGINT NOT NULL DEFAULT 0 CHECK (xp_balance >= 0);

UPDATE user_gamification
SET xp_balance = total_xp;

ALTER TABLE buddy
    ADD COLUMN chosen_at TIMESTAMPTZ NULL;

UPDATE buddy
SET chosen_at = created_at;

CREATE TABLE market_item (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    slot VARCHAR(16) NOT NULL CHECK (slot IN ('HAT', 'EYEWEAR')),
    name VARCHAR(80) NOT NULL,
    name_km VARCHAR(120) NOT NULL,
    price BIGINT NOT NULL CHECK (price >= 0),
    rarity VARCHAR(16) NOT NULL CHECK (rarity IN ('COMMON', 'RARE', 'EPIC')),
    asset_key VARCHAR(64) NOT NULL UNIQUE,
    species_mask TEXT,
    min_tier VARCHAR(32),
    available_from TIMESTAMPTZ,
    available_until TIMESTAMPTZ,
    sort_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_market_item_active_order
    ON market_item (active, sort_order, price);

CREATE TABLE buddy_inventory (
    user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    item_id UUID NOT NULL REFERENCES market_item(id) ON DELETE CASCADE,
    acquired_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    source VARCHAR(16) NOT NULL CHECK (source IN ('PURCHASE', 'CHEST', 'ADMIN')),
    price_paid BIGINT NOT NULL CHECK (price_paid >= 0),
    idempotency_key UUID,
    PRIMARY KEY (user_id, item_id),
    CONSTRAINT uq_buddy_inventory_idempotency UNIQUE (user_id, idempotency_key)
);

CREATE INDEX idx_buddy_inventory_user_source_time
    ON buddy_inventory (user_id, source, acquired_at);

CREATE TABLE buddy_equipped (
    user_id UUID NOT NULL REFERENCES buddy(user_id) ON DELETE CASCADE,
    slot VARCHAR(16) NOT NULL CHECK (slot IN ('HAT', 'EYEWEAR')),
    item_id UUID NOT NULL,
    PRIMARY KEY (user_id, slot),
    CONSTRAINT fk_buddy_equipped_inventory FOREIGN KEY (user_id, item_id)
        REFERENCES buddy_inventory(user_id, item_id) ON DELETE CASCADE
);

INSERT INTO market_item (slot, name, name_km, price, rarity, asset_key, sort_order) VALUES
    ('HAT', 'Straw hat', 'មួកចំបើង', 300, 'COMMON', 'hat_straw', 10),
    ('HAT', 'Red cap', 'មួកក្រហម', 250, 'COMMON', 'hat_cap_red', 20),
    ('HAT', 'Graduation cap', 'មួកបញ្ចប់ការសិក្សា', 800, 'RARE', 'hat_graduation', 30),
    ('HAT', 'Krama headwrap', 'ក្រមាពាក់ក្បាល', 500, 'RARE', 'hat_krama', 40),
    ('HAT', 'Little crown', 'មកុដតូច', 1500, 'EPIC', 'hat_crown', 50),
    ('EYEWEAR', 'Round glasses', 'វ៉ែនតាមូល', 200, 'COMMON', 'eye_round', 60),
    ('EYEWEAR', 'Sunglasses', 'វ៉ែនតាការពារពន្លឺ', 350, 'COMMON', 'eye_sun', 70),
    ('EYEWEAR', 'Star glasses', 'វ៉ែនតាផ្កាយ', 700, 'RARE', 'eye_star', 80);
