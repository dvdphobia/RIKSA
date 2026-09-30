-- Comments can now carry a single image alongside (or instead of) their text.
-- The URL is supplied by the client, matching how "posts".media_url already works.
ALTER TABLE "comments" ADD COLUMN IF NOT EXISTS media_url TEXT;

-- Image-only comments need content to be optional. Every existing row has
-- content, so relaxing the constraint is safe.
ALTER TABLE "comments" ALTER COLUMN content DROP NOT NULL;

-- ...but a comment that is neither text nor image is meaningless.
ALTER TABLE "comments" ADD CONSTRAINT comments_content_or_media_present
    CHECK (
        (content IS NOT NULL AND btrim(content) <> '')
        OR (media_url IS NOT NULL AND btrim(media_url) <> '')
    );
