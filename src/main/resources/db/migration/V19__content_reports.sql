CREATE TYPE "ReportTargetType" AS ENUM ('POST', 'COMMENT', 'BLOG');
CREATE TYPE "ReportStatus" AS ENUM ('OPEN', 'RESOLVED', 'DISMISSED');

CREATE TABLE IF NOT EXISTS "content_reports" (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reporter_id  UUID NOT NULL REFERENCES "users"(id),
    target_type  "ReportTargetType" NOT NULL,
    target_id    UUID NOT NULL,
    reason       VARCHAR(60) NOT NULL,
    details      TEXT,
    status       "ReportStatus" NOT NULL DEFAULT 'OPEN',
    resolved_by  UUID REFERENCES "users"(id),
    resolved_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (reporter_id, target_type, target_id)
);
CREATE INDEX IF NOT EXISTS idx_reports_open ON "content_reports" (status, created_at) WHERE status = 'OPEN';
