-- ============================================================================
-- V2: Add refresh_tokens, otp_codes tables and phone_verified column
-- ============================================================================

-- Add phone_verified column to users table
ALTER TABLE "users" ADD COLUMN IF NOT EXISTS phone_verified BOOLEAN NOT NULL DEFAULT false;

-- Add gender column to users table
ALTER TABLE "users" ADD COLUMN IF NOT EXISTS gender VARCHAR(20);

-- Refresh tokens table (for token revocation on logout)
CREATE TABLE "refresh_tokens" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    token TEXT NOT NULL,
    user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_token ON "refresh_tokens"(token);
CREATE INDEX idx_refresh_tokens_user_id ON "refresh_tokens"(user_id);

-- OTP codes table (for phone verification)
CREATE TABLE "otp_codes" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    phone_number VARCHAR(20) NOT NULL,
    code VARCHAR(6) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    verified BOOLEAN NOT NULL DEFAULT false,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_otp_codes_phone_number ON "otp_codes"(phone_number);
