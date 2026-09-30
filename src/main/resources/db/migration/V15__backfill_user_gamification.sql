-- ============================================================================
-- V15: Backfill user_gamification rows for existing users
--
-- When the gamification module was first built, no row was created in
-- user_gamification at registration time. This means existing users who
-- registered before the fix would crash the XP hot path with
-- "UserGamification row not found".
--
-- This migration inserts a default row (0 XP, 0 streak) for every user
-- that doesn't already have one.
-- ============================================================================

INSERT INTO "user_gamification" (user_id, total_xp, current_streak, highest_streak, last_completed_at, updated_at, version)
SELECT u.id, 0, 0, 0, NULL, now(), 0
FROM "users" u
WHERE NOT EXISTS (
    SELECT 1 FROM "user_gamification" ug WHERE ug.user_id = u.id
);
