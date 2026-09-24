-- Learners and teachers can now create their own communities.
--
-- `official` marks the curated categories that lessons and the
-- "every category" achievement are built on; every row that exists today is
-- one of those. `created_by` records who founded a user community. It is
-- nulled if that account is deleted, while `official` stays false, so an
-- orphaned community never turns into an official one.

ALTER TABLE "categories"
    ADD COLUMN official BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE "categories" SET official = TRUE;

ALTER TABLE "categories"
    ADD COLUMN created_by UUID REFERENCES "users"(id) ON DELETE SET NULL;

CREATE INDEX idx_categories_created_by ON "categories"(created_by);

-- "Biology" and "biology" must not both exist. The service checks first; this
-- index settles two requests that race past that check.
CREATE UNIQUE INDEX ux_categories_name_lower ON "categories"(lower(name));
