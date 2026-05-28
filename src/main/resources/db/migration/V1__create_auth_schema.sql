-- ============================================================
-- Riksa API - Auth Schema
-- Phone OTP + Google OAuth for Cambodian users
-- ============================================================

-- Provinces (reference data for Cambodia)
CREATE TABLE provinces (
    id          BIGSERIAL   PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    name_khmer  VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Schools
CREATE TABLE schools (
    id          BIGSERIAL   PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    name_khmer  VARCHAR(200) NOT NULL,
    province_id BIGINT      NOT NULL REFERENCES provinces(id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Users (supports phone OTP and Google OAuth)
CREATE TABLE users (
    id                BIGSERIAL    PRIMARY KEY,
    name              VARCHAR(100),
    name_khmer        VARCHAR(100),
    email             VARCHAR(255),
    phone_number      VARCHAR(20),
    password_hash     VARCHAR(255),
    google_id         VARCHAR(255),
    avatar_url        VARCHAR(500),
    school_id         BIGINT       REFERENCES schools(id),
    province_id       BIGINT       REFERENCES provinces(id),
    role              VARCHAR(20)  NOT NULL DEFAULT 'STUDENT',
    trust_level       INTEGER      NOT NULL DEFAULT 0,
    current_rank      VARCHAR(50),
    grade             VARCHAR(10),
    is_banned         BOOLEAN      NOT NULL DEFAULT FALSE,
    is_phone_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    is_email_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_users_email        UNIQUE (email),
    CONSTRAINT uq_users_phone_number  UNIQUE (phone_number),
    CONSTRAINT uq_users_google_id     UNIQUE (google_id),
    CONSTRAINT ck_users_role           CHECK (role IN ('STUDENT','TEACHER','ADMIN'))
);

-- User sessions (JWT refresh tokens, one per device)
CREATE TABLE user_sessions (
    id             BIGSERIAL    PRIMARY KEY,
    user_id        BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    refresh_token  VARCHAR(500) NOT NULL,
    device_info    VARCHAR(255),
    expires_at     TIMESTAMPTZ  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- OTP verifications (phone number OTP for Khmer users)
CREATE TABLE otp_verifications (
    id           BIGSERIAL   PRIMARY KEY,
    phone_number VARCHAR(20) NOT NULL,
    code         VARCHAR(6)  NOT NULL,
    verified     BOOLEAN     NOT NULL DEFAULT FALSE,
    attempts     INTEGER     NOT NULL DEFAULT 0,
    expires_at   TIMESTAMPTZ NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Indexes
-- ============================================================
CREATE INDEX idx_users_phone_number     ON users(phone_number);
CREATE INDEX idx_users_email            ON users(email);
CREATE INDEX idx_users_google_id        ON users(google_id) WHERE google_id IS NOT NULL;
CREATE INDEX idx_user_sessions_token    ON user_sessions(refresh_token);
CREATE INDEX idx_user_sessions_user_id  ON user_sessions(user_id);
CREATE INDEX idx_otp_phone_code         ON otp_verifications(phone_number, code);
CREATE INDEX idx_otp_expires            ON otp_verifications(expires_at);

-- ============================================================
-- Seed Cambodian provinces (24 provinces + Phnom Penh)
-- ============================================================
INSERT INTO provinces (name, name_khmer) VALUES
    ('Phnom Penh',           'ភ្នំពេញ'),
    ('Banteay Meanchey',     'បន្ទាយមានជ័យ'),
    ('Battambang',            'បាត់ដំបង'),
    ('Kampong Cham',          'កំពង់ចាម'),
    ('Kampong Chhnang',       'កំពង់ឆ្នាំង'),
    ('Kampong Speu',          'កំពង់ស្ពឺ'),
    ('Kampong Thom',          'កំពង់ធំ'),
    ('Kampot',                'កំពត'),
    ('Kandal',                'កណ្ដាល'),
    ('Kep',                   'កែប'),
    ('Koh Kong',              'កោះកុង'),
    ('Kratié',                'ក្រចេះ'),
    ('Mondulkiri',            'មណ្ឌលគីរី'),
    ('Oddar Meanchey',        'ឧត្តរមានជ័យ'),
    ('Pailin',                'ប៉ៃលិន'),
    ('Preah Sihanouk',        'ព្រះសីហនុ'),
    ('Preah Vihear',          'ព្រះវិហារ'),
    ('Pursat',                'ពោធិ៍សាត់'),
    ('Prey Veng',             'ព្រៃវែង'),
    ('Ratanakiri',            'រតនគីរី'),
    ('Siem Reap',            'សៀមរាប'),
    ('Stung Treng',           'ស្ទឹងត្រែង'),
    ('Svay Rieng',            'ស្វាយរៀង'),
    ('Takeo',                 'តាកែវ'),
    ('Tboung Khmum',          'ត្បូងឃ្មុំ');
