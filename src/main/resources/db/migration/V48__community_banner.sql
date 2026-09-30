-- Owners style their community: a banner photo, or a banner colour.
ALTER TABLE "categories" ADD COLUMN banner_url TEXT;
-- A #RRGGBB colour the owner picked; null keeps the subject colour.
ALTER TABLE "categories" ADD COLUMN banner_color VARCHAR(7);
