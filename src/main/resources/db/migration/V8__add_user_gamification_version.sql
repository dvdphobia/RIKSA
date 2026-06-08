-- V8: optimistic locking on user_gamification
--
-- The user_gamification row is the hottest row in the system: it is mutated
-- on every lesson completion, every task completion, and every daily challenge.
-- Concurrent XP grants without a version column will silently lose updates
-- (last-writer-wins) and produce off-by-one XP and streaks.
--
-- Adding a BIGINT version column. Hibernate's @Version uses 0 as the initial
-- value for newly inserted rows, so existing rows get 0 here.

ALTER TABLE "user_gamification"
    ADD COLUMN "version" BIGINT NOT NULL DEFAULT 0;
