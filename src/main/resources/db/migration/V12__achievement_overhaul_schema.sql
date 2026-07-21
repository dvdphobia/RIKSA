-- ============================================================================
-- V12: Achievement system overhaul — schema changes
-- ============================================================================

-- 1. Create AchievementRarity enum
CREATE TYPE "AchievementRarity" AS ENUM ('COMMON', 'RARE', 'EPIC', 'LEGENDARY');

-- 2. Add rarity, category_id, and xp_reward columns to achievements
ALTER TABLE "achievements"
    ADD COLUMN IF NOT EXISTS "rarity" "AchievementRarity" NOT NULL DEFAULT 'COMMON',
    ADD COLUMN IF NOT EXISTS "category_id" UUID REFERENCES "categories"(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS "xp_reward" INTEGER NOT NULL DEFAULT 0;

-- 3. Add new AchievementCriteriaType enum values
-- Note: ALTER TYPE ... ADD VALUE must be run outside a transaction in PG < 12,
-- but PG 12+ with IF NOT EXISTS is safe inside a transaction.
ALTER TYPE "AchievementCriteriaType" ADD VALUE IF NOT EXISTS 'MAINTAIN_STREAK';
ALTER TYPE "AchievementCriteriaType" ADD VALUE IF NOT EXISTS 'REACH_RANK';
ALTER TYPE "AchievementCriteriaType" ADD VALUE IF NOT EXISTS 'CREATE_POST';
ALTER TYPE "AchievementCriteriaType" ADD VALUE IF NOT EXISTS 'RECEIVE_REACTION';
ALTER TYPE "AchievementCriteriaType" ADD VALUE IF NOT EXISTS 'WRITE_BLOG_POST';
ALTER TYPE "AchievementCriteriaType" ADD VALUE IF NOT EXISTS 'RECEIVE_BLOG_LIKE';

-- 4. Update existing achievements with rarity and XP rewards
UPDATE "achievements" SET rarity = 'COMMON', xp_reward = 50  WHERE name = 'First Lesson';
UPDATE "achievements" SET rarity = 'COMMON', xp_reward = 100 WHERE name = 'Quick Learner';
UPDATE "achievements" SET rarity = 'RARE',   xp_reward = 500 WHERE name = 'Dedicated Student';
UPDATE "achievements" SET rarity = 'COMMON', xp_reward = 100 WHERE name = 'XP Hunter';
UPDATE "achievements" SET rarity = 'EPIC',   xp_reward = 500 WHERE name = 'XP Master';
UPDATE "achievements" SET rarity = 'RARE',   xp_reward = 150 WHERE name = 'Hard Mode';