-- Add failed login attempts and lockout columns
ALTER TABLE "users" ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE "users" ADD COLUMN IF NOT EXISTS locked_until TIMESTAMPTZ;

-- Add email verified column
ALTER TABLE "users" ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT false;
