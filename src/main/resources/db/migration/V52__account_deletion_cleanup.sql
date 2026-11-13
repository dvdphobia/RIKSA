-- Preserve moderation history while removing links to a deleted account.
ALTER TABLE "admin_audit_logs" ALTER COLUMN actor_id DROP NOT NULL;
ALTER TABLE "admin_audit_logs" DROP CONSTRAINT IF EXISTS admin_audit_logs_actor_id_fkey;
ALTER TABLE "admin_audit_logs"
    ADD CONSTRAINT admin_audit_logs_actor_id_fkey
    FOREIGN KEY (actor_id) REFERENCES "users"(id) ON DELETE SET NULL;

ALTER TABLE "posts" DROP CONSTRAINT IF EXISTS posts_hidden_by_fkey;
ALTER TABLE "posts"
    ADD CONSTRAINT posts_hidden_by_fkey
    FOREIGN KEY (hidden_by) REFERENCES "users"(id) ON DELETE SET NULL;

ALTER TABLE "comments" DROP CONSTRAINT IF EXISTS comments_hidden_by_fkey;
ALTER TABLE "comments"
    ADD CONSTRAINT comments_hidden_by_fkey
    FOREIGN KEY (hidden_by) REFERENCES "users"(id) ON DELETE SET NULL;

ALTER TABLE "content_reports" DROP CONSTRAINT IF EXISTS content_reports_reporter_id_fkey;
ALTER TABLE "content_reports"
    ADD CONSTRAINT content_reports_reporter_id_fkey
    FOREIGN KEY (reporter_id) REFERENCES "users"(id) ON DELETE CASCADE;

ALTER TABLE "content_reports" DROP CONSTRAINT IF EXISTS content_reports_resolved_by_fkey;
ALTER TABLE "content_reports"
    ADD CONSTRAINT content_reports_resolved_by_fkey
    FOREIGN KEY (resolved_by) REFERENCES "users"(id) ON DELETE SET NULL;

-- Track upload ownership so account deletion can remove unreferenced local files.
CREATE TABLE media_assets (
    id VARCHAR(40) PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    public_url TEXT NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_media_assets_owner ON media_assets (owner_id);
