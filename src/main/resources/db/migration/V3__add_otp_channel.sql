-- Create OtpChannel enum type
CREATE TYPE "OtpChannel" AS ENUM ('SMS', 'TELEGRAM');

-- Rename phone_number to identifier (generic for both phone numbers and telegram chat IDs)
ALTER TABLE "otp_codes" RENAME COLUMN phone_number TO identifier;

-- Add channel column with TELEGRAM support
ALTER TABLE "otp_codes" ADD COLUMN channel "OtpChannel" NOT NULL DEFAULT 'SMS';

-- Widen identifier column for longer telegram chat IDs
ALTER TABLE "otp_codes" ALTER COLUMN identifier TYPE VARCHAR(100);

-- Drop old index if exists
DROP INDEX IF EXISTS idx_otp_codes_phone_number;

-- Create composite index for channel + identifier lookups
CREATE INDEX idx_otp_codes_channel_identifier ON "otp_codes"(channel, identifier);
