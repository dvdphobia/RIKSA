-- Community posts can carry up to four uploaded images. The position is the
-- display order and is also the per-post key.
CREATE TABLE "post_media" (
    post_id UUID NOT NULL REFERENCES "posts"(id) ON DELETE CASCADE,
    position SMALLINT NOT NULL CHECK (position BETWEEN 0 AND 3),
    url TEXT NOT NULL,
    width INTEGER CHECK (width BETWEEN 1 AND 10000),
    height INTEGER CHECK (height BETWEEN 1 AND 10000),
    PRIMARY KEY (post_id, position)
);

-- Normalize empty legacy values, then keep every existing cover as the first
-- image. Width and height were not recorded by the old schema.
UPDATE "posts"
SET media_url = NULLIF(btrim(media_url), '')
WHERE media_url IS NOT NULL;

INSERT INTO "post_media" (post_id, position, url)
SELECT id, 0, media_url
FROM "posts"
WHERE media_url IS NOT NULL;

-- The application already limits titles to 200 characters. Bring the original
-- VARCHAR(255) database column into line before allowing image-only posts.
UPDATE "posts"
SET title = left(title, 200)
WHERE char_length(title) > 200;

ALTER TABLE "posts" ALTER COLUMN title TYPE VARCHAR(200);
ALTER TABLE "posts" ALTER COLUMN title DROP NOT NULL;
