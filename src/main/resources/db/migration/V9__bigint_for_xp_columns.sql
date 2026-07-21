-- ============================================================================
-- V9: Change XP-related columns from INTEGER to BIGINT
--
-- XP accumulates indefinitely. INTEGER max (2.1B) is sufficient for a
-- single user but risky for running balances in high-volume systems.
-- BIGINT provides headroom and is consistent with the @Version BIGINT
-- already on user_gamification.
-- ============================================================================

-- Drop the view that depends on total_xp before altering
DROP VIEW IF EXISTS "user_activity_summary";

ALTER TABLE "xp_transactions" ALTER COLUMN "amount" TYPE BIGINT;
ALTER TABLE "xp_transactions" ALTER COLUMN "running_balance" TYPE BIGINT;

ALTER TABLE "user_gamification" ALTER COLUMN "total_xp" TYPE BIGINT;
ALTER TABLE "user_gamification" ALTER COLUMN "current_streak" TYPE BIGINT;
ALTER TABLE "user_gamification" ALTER COLUMN "highest_streak" TYPE BIGINT;

ALTER TABLE "rank_tiers" ALTER COLUMN "min_xp" TYPE BIGINT;

ALTER TABLE "achievements" ALTER COLUMN "criteria_value" TYPE BIGINT;

ALTER TABLE "streak_milestones" ALTER COLUMN "min_streak" TYPE BIGINT;

-- Recreate the view
CREATE OR REPLACE VIEW "user_activity_summary" AS
SELECT
    u.id AS user_id,
    u.username,
    u.display_name,
    COALESCE(ug.total_xp, 0) AS total_xp,
    COALESCE(ug.current_streak, 0) AS current_streak,
    COALESCE(ug.highest_streak, 0) AS highest_streak,
    (SELECT rt.tier_name FROM "rank_tiers" rt
     WHERE ug.total_xp >= rt.min_xp
     ORDER BY rt.min_xp DESC LIMIT 1) AS current_rank,
    p.name_latin AS province_name,
    COALESCE((SELECT COUNT(*) FROM "lesson_completions" lc WHERE lc.user_id = u.id), 0) AS total_lessons_completed,
    COALESCE((SELECT COUNT(*) FROM "user_achievements" ua WHERE ua.user_id = u.id), 0) AS total_achievements,
    ug.last_completed_at
FROM "users" u
LEFT JOIN "user_gamification" ug ON u.id = ug.user_id
LEFT JOIN "provinces" p ON u.province_id = p.id;