-- In-app notifications: reactions, comments, replies, blog likes and reviews.
CREATE TABLE "notifications" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    recipient_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    actor_id UUID REFERENCES "users"(id) ON DELETE SET NULL,
    type VARCHAR(32) NOT NULL,
    target_type VARCHAR(16) NOT NULL,
    target_id UUID NOT NULL,
    post_id UUID,
    preview VARCHAR(200),
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- The inbox and the unread badge.
CREATE INDEX idx_notifications_recipient_created ON "notifications" (recipient_id, created_at DESC, id DESC);
CREATE INDEX idx_notifications_recipient_unread ON "notifications" (recipient_id) WHERE read_at IS NULL;
-- Repeat reactions and likes refresh one row instead of stacking up.
CREATE INDEX idx_notifications_dedupe ON "notifications" (recipient_id, actor_id, type, target_id);
