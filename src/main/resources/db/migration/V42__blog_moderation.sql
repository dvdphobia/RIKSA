ALTER TABLE "blog_posts"
    ADD COLUMN first_approved_at TIMESTAMPTZ NULL,
    ADD COLUMN normalized_hash VARCHAR(64) NULL,
    ADD COLUMN moderation_score INTEGER NULL,
    ADD COLUMN moderation_reasons VARCHAR(500) NULL;

UPDATE "blog_posts"
SET first_approved_at = COALESCE(updated_at, created_at)
WHERE status = 'APPROVED';

CREATE INDEX idx_blog_posts_author_hash_created
    ON "blog_posts" (author_id, normalized_hash, created_at);
