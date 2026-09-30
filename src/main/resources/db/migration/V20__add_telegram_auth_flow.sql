-- Secure Telegram bot deep-link authentication.
-- telegram_id is the stable Telegram user id; telegram_chat_id is where the bot
-- can deliver future login and recovery messages. They are equal for normal
-- private chats today, but are stored separately because they have different
-- meanings in Telegram's API.
ALTER TABLE "users"
    ADD COLUMN IF NOT EXISTS telegram_chat_id TEXT UNIQUE;

UPDATE "users"
SET telegram_chat_id = telegram_id
WHERE telegram_id IS NOT NULL
  AND telegram_chat_id IS NULL;

CREATE TABLE "telegram_auth_challenges" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    telegram_user_id TEXT,
    telegram_chat_id TEXT,
    telegram_username VARCHAR(64),
    telegram_display_name VARCHAR(100),
    expires_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_telegram_auth_challenge_status
        CHECK (status IN ('PENDING', 'CODE_SENT', 'COMPLETED', 'EXPIRED'))
);

CREATE INDEX idx_telegram_auth_challenges_expires_at
    ON "telegram_auth_challenges"(expires_at);

