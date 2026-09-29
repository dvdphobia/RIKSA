-- Founders run their own communities: moderators, bans, rules, a pinned post
-- and a switch for who may post.

-- OWNER, MODERATOR or MEMBER. The founder's membership row is the owner's.
ALTER TABLE "user_category_memberships"
    ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'MEMBER';

UPDATE "user_category_memberships" m
SET role = 'OWNER'
FROM "categories" c
WHERE c.id = m.category_id AND c.created_by = m.user_id;

-- MEMBERS: any member may post. MODERATORS: only the owner and moderators.
ALTER TABLE "categories"
    ADD COLUMN post_permission VARCHAR(16) NOT NULL DEFAULT 'MEMBERS';

ALTER TABLE "categories"
    ADD COLUMN pinned_post_id UUID REFERENCES "posts"(id) ON DELETE SET NULL;

CREATE TABLE "community_rules" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category_id UUID NOT NULL REFERENCES "categories"(id) ON DELETE CASCADE,
    position INT NOT NULL,
    title VARCHAR(100) NOT NULL,
    body VARCHAR(500)
);

CREATE INDEX idx_community_rules_category ON "community_rules" (category_id, position);

-- A banned member is removed and cannot join again until unbanned.
CREATE TABLE "community_bans" (
    category_id UUID NOT NULL REFERENCES "categories"(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    banned_by UUID REFERENCES "users"(id) ON DELETE SET NULL,
    reason VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (category_id, user_id)
);
