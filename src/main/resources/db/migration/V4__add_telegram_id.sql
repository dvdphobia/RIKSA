-- Add telegram_id column to users table for Telegram-based OTP verification
ALTER TABLE "users" ADD COLUMN IF NOT EXISTS telegram_id TEXT UNIQUE;
