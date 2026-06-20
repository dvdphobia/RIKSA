-- ============================================================================
-- V9: Change XP-related columns from INTEGER to BIGINT
--
-- XP accumulates indefinitely. INTEGER max (2.1B) is sufficient for a
-- single user but risky for running balances in high-volume systems.
-- BIGINT provides headroom and is consistent with the @Version BIGINT
-- already on user_gamification.
-- ============================================================================

ALTER TABLE "xp_transactions" ALTER COLUMN "amount" TYPE BIGINT;
ALTER TABLE "xp_transactions" ALTER COLUMN "running_balance" TYPE BIGINT;

ALTER TABLE "user_gamification" ALTER COLUMN "total_xp" TYPE BIGINT;
ALTER TABLE "user_gamification" ALTER COLUMN "current_streak" TYPE BIGINT;
ALTER TABLE "user_gamification" ALTER COLUMN "highest_streak" TYPE BIGINT;

ALTER TABLE "rank_tiers" ALTER COLUMN "min_xp" TYPE BIGINT;

ALTER TABLE "achievements" ALTER COLUMN "criteria_value" TYPE BIGINT;

ALTER TABLE "streak_milestones" ALTER COLUMN "min_streak" TYPE BIGINT;