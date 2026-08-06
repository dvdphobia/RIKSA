CREATE TYPE "ContentStatus" AS ENUM ('VISIBLE', 'HIDDEN');

ALTER TABLE "posts" ADD COLUMN IF NOT EXISTS status "ContentStatus" NOT NULL DEFAULT 'VISIBLE',
                    ADD COLUMN IF NOT EXISTS hidden_reason VARCHAR(255),
                    ADD COLUMN IF NOT EXISTS hidden_by UUID REFERENCES "users"(id),
                    ADD COLUMN IF NOT EXISTS hidden_at TIMESTAMPTZ;
CREATE INDEX IF NOT EXISTS idx_posts_status ON "posts" (status) WHERE status = 'HIDDEN';

ALTER TABLE "comments" ADD COLUMN IF NOT EXISTS status "ContentStatus" NOT NULL DEFAULT 'VISIBLE',
                         ADD COLUMN IF NOT EXISTS hidden_reason VARCHAR(255),
                         ADD COLUMN IF NOT EXISTS hidden_by UUID REFERENCES "users"(id),
                         ADD COLUMN IF NOT EXISTS hidden_at TIMESTAMPTZ;
