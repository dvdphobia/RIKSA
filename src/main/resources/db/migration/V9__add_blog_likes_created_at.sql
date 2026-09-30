ALTER TABLE "blog_likes"
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT now();

UPDATE "blog_likes"
    SET created_at = liked_at
    WHERE created_at IS NULL;

ALTER TABLE "blog_likes"
    ALTER COLUMN created_at SET NOT NULL;
