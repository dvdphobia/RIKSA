CREATE TABLE IF NOT EXISTS "admin_audit_logs" (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    actor_id    UUID NOT NULL REFERENCES "users"(id),
    action      VARCHAR(60) NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id   UUID,
    details     JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_audit_actor ON "admin_audit_logs" (actor_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_target ON "admin_audit_logs" (target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_audit_created ON "admin_audit_logs" (created_at DESC);
