-- Lesson-page illustrations. Null on a lesson means "use the subject".
-- Null on a category means the app uses its own subject art.
-- background_key is a short preset name (validated in the API, not an enum)
-- so a new preset does not need a schema change. The image URL is TEXT,
-- matching banner_url, because uploaded media links are not bounded to 255.
ALTER TABLE "categories" ADD COLUMN background_key VARCHAR(32);
ALTER TABLE "categories" ADD COLUMN background_image_url TEXT;

ALTER TABLE "lessons" ADD COLUMN background_key VARCHAR(32);
ALTER TABLE "lessons" ADD COLUMN background_image_url TEXT;
