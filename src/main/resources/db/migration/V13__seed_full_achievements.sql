-- ============================================================================
-- V13: Seed full achievement catalog — 30 achievements total
-- ============================================================================

-- Insert all new achievements (existing 6 are already in the DB from V7).
-- Use ON CONFLICT (name) DO NOTHING to avoid duplicates.

-- ── Learning: streak achievements (6) ──
INSERT INTO "achievements" (name, description, criteria_type, criteria_value, icon_url, rarity, xp_reward) VALUES
    ('First Steps',       'Maintain a 3-day streak',     'MAINTAIN_STREAK', 3,   '/icons/ach-streak-3.png',   'COMMON',    30),
    ('Week Warrior',      'Maintain a 7-day streak',     'MAINTAIN_STREAK', 7,   '/icons/ach-streak-7.png',   'COMMON',    50),
    ('Fortnight Fighter', 'Maintain a 14-day streak',    'MAINTAIN_STREAK', 14,  '/icons/ach-streak-14.png',  'RARE',      100),
    ('Monthly Master',    'Maintain a 30-day streak',    'MAINTAIN_STREAK', 30,  '/icons/ach-streak-30.png',  'EPIC',      300),
    ('Quarterly Quest',   'Maintain a 90-day streak',    'MAINTAIN_STREAK', 90,  '/icons/ach-streak-90.png',  'EPIC',      500),
    ('Yearly Legend',     'Maintain a 365-day streak',   'MAINTAIN_STREAK', 365, '/icons/ach-streak-365.png', 'LEGENDARY', 2000)
ON CONFLICT (name) DO NOTHING;

-- ── Learning: rank achievements (5) ──
-- criteria_value = TierType ordinal (BRONZE=0, SILVER=1, GOLD=2, PLATINUM=3, DIAMOND=4, LEGEND=5)
INSERT INTO "achievements" (name, description, criteria_type, criteria_value, icon_url, rarity, xp_reward) VALUES
    ('Silver Tier',   'Reach Silver rank',   'REACH_RANK', 1, '/icons/ach-rank-silver.png',   'COMMON',    50),
    ('Gold Tier',     'Reach Gold rank',     'REACH_RANK', 2, '/icons/ach-rank-gold.png',     'RARE',      150),
    ('Platinum Tier', 'Reach Platinum rank', 'REACH_RANK', 3, '/icons/ach-rank-platinum.png', 'EPIC',      400),
    ('Diamond Tier',  'Reach Diamond rank',  'REACH_RANK', 4, '/icons/ach-rank-diamond.png',  'EPIC',      800),
    ('Legend Tier',   'Reach Legend rank',   'REACH_RANK', 5, '/icons/ach-rank-legend.png',   'LEGENDARY', 2000)
ON CONFLICT (name) DO NOTHING;

-- ── Learning: additional course-completion achievements (4) ──
INSERT INTO "achievements" (name, description, criteria_type, criteria_value, icon_url, rarity, xp_reward) VALUES
    ('Century Scholar',   'Complete 100 lessons',                     'COMPLETE_N_COURSES', 100, '/icons/ach-century.png',     'EPIC',      1000),
    ('Difficult Mastery', 'Complete a DIFFICULT lesson',               'COMPLETE_COURSE_WITH_DIFFICULTY', 0, '/icons/ach-difficult.png', 'EPIC', 300),
    ('Category Explorer', 'Complete at least one lesson in every category', 'COMPLETE_COURSE_IN_EVERY_CATEGORY', 1, '/icons/ach-explorer.png', 'RARE', 200),
    ('Completionist',     'Complete ALL lessons across ALL categories',     'COMPLETE_ALL_IN_EVERY_CATEGORY', 1, '/icons/ach-completionist.png', 'LEGENDARY', 2000)
ON CONFLICT (name) DO NOTHING;

-- Set requires_advanced = true for the Difficult Mastery achievement
UPDATE "achievements" SET requires_advanced = true WHERE name = 'Difficult Mastery';

-- ── Social: post achievements (3) ──
INSERT INTO "achievements" (name, description, criteria_type, criteria_value, icon_url, rarity, xp_reward) VALUES
    ('First Post',         'Create your first community post',     'CREATE_POST', 1,  '/icons/ach-first-post.png',    'COMMON', 20),
    ('Active Contributor', 'Create 10 community posts',            'CREATE_POST', 10, '/icons/ach-contributor.png',   'RARE',   100),
    ('Community Pillar',   'Create 50 community posts',            'CREATE_POST', 50, '/icons/ach-pillar.png',        'EPIC',   500)
ON CONFLICT (name) DO NOTHING;

-- ── Social: reaction achievements (2) ──
INSERT INTO "achievements" (name, description, criteria_type, criteria_value, icon_url, rarity, xp_reward) VALUES
    ('Getting Noticed',  'Receive 10 reactions on your posts',  'RECEIVE_REACTION', 10,  '/icons/ach-noticed.png',  'COMMON', 50),
    ('Popular Voice',    'Receive 100 reactions on your posts', 'RECEIVE_REACTION', 100, '/icons/ach-popular.png',  'EPIC',   300)
ON CONFLICT (name) DO NOTHING;

-- ── Blog: blog post achievements (3) ──
INSERT INTO "achievements" (name, description, criteria_type, criteria_value, icon_url, rarity, xp_reward) VALUES
    ('First Blog',       'Publish your first approved blog post', 'WRITE_BLOG_POST', 1,  '/icons/ach-first-blog.png',   'COMMON', 50),
    ('Blogger',          'Publish 5 approved blog posts',         'WRITE_BLOG_POST', 5,  '/icons/ach-blogger.png',      'RARE',   200),
    ('Prolific Writer',  'Publish 20 approved blog posts',        'WRITE_BLOG_POST', 20, '/icons/ach-prolific.png',     'EPIC',   500)
ON CONFLICT (name) DO NOTHING;

-- ── Blog: blog like achievements (2) ──
INSERT INTO "achievements" (name, description, criteria_type, criteria_value, icon_url, rarity, xp_reward) VALUES
    ('Rising Author',  'Receive 10 likes on your blog posts',  'RECEIVE_BLOG_LIKE', 10,  '/icons/ach-rising-author.png',  'RARE',      100),
    ('Beloved Author', 'Receive 100 likes on your blog posts', 'RECEIVE_BLOG_LIKE', 100, '/icons/ach-beloved.png',        'LEGENDARY', 1000)
ON CONFLICT (name) DO NOTHING;