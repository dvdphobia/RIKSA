-- Email verification tokens
CREATE TABLE "email_verification_tokens" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    token TEXT NOT NULL,
    user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_email_verification_tokens_token ON "email_verification_tokens"(token);
