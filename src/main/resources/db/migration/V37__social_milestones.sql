ALTER TABLE users
    ADD COLUMN share_milestones BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE posts
    ADD COLUMN kind VARCHAR(16) NOT NULL DEFAULT 'USER'
        CHECK (kind IN ('USER', 'MILESTONE')),
    ADD COLUMN milestone_type VARCHAR(24),
    ADD COLUMN milestone_title VARCHAR(255),
    ADD COLUMN milestone_title_km VARCHAR(255),
    ADD COLUMN milestone_value INTEGER;

ALTER TABLE posts
    ADD CONSTRAINT ck_post_milestone_fields
    CHECK (kind = 'USER' OR
        (milestone_type IS NOT NULL AND milestone_title IS NOT NULL
         AND milestone_title_km IS NOT NULL AND milestone_value IS NOT NULL));

CREATE UNIQUE INDEX uq_milestone_post_user_type_value
    ON posts (author_id, milestone_type, milestone_value)
    WHERE kind = 'MILESTONE';

INSERT INTO categories (id, name, slug, description, icon_url, official, created_by)
VALUES (
    'b37a0000-0000-0000-0000-000000000001',
    'Wins / ជ័យជម្នះ',
    'wins',
    'Learning milestones shared by the community.',
    NULL,
    TRUE,
    NULL
)
ON CONFLICT (slug) DO NOTHING;
