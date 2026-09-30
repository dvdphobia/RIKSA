-- Two Telegram sign-in options:
--  * one-tap: t.me/<bot>?start=login_<nonce>, confirmed with an inline button;
--  * code: the bot's /start or /code reply contains a short code pasted in the app.
-- Both look challenges up by start_nonce_hash (an HMAC of the nonce or code).
ALTER TABLE "telegram_auth_challenges"
    ADD COLUMN IF NOT EXISTS start_nonce_hash VARCHAR(64) UNIQUE;

-- The previous flow's CODE_SENT state no longer exists; those requests are
-- short-lived, so retire any leftovers instead of migrating them.
UPDATE "telegram_auth_challenges"
SET status = 'EXPIRED'
WHERE status = 'CODE_SENT';

ALTER TABLE "telegram_auth_challenges"
    DROP CONSTRAINT IF EXISTS chk_telegram_auth_challenge_status;

ALTER TABLE "telegram_auth_challenges"
    ADD CONSTRAINT chk_telegram_auth_challenge_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'DENIED', 'PROFILE_REQUIRED', 'COMPLETED', 'EXPIRED'));
