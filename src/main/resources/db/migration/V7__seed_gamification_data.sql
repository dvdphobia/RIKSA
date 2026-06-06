-- ============================================================================
-- Seed gamification reference data: rank tiers, streak milestones, achievements
-- ============================================================================

INSERT INTO "rank_tiers" (tier_name, min_xp, icon_url) VALUES
    ('BRONZE', 0, '/icons/rank-bronze.png'),
    ('SILVER', 100, '/icons/rank-silver.png'),
    ('GOLD', 500, '/icons/rank-gold.png'),
    ('PLATINUM', 1000, '/icons/rank-platinum.png'),
    ('DIAMOND', 2500, '/icons/rank-diamond.png'),
    ('LEGEND', 5000, '/icons/rank-legend.png');

INSERT INTO "streak_milestones" (min_streak, streak_name, icon_url) VALUES
    (1, 'First Day', '/icons/streak-1.png'),
    (3, 'Getting Started', '/icons/streak-3.png'),
    (7, 'Week Warrior', '/icons/streak-7.png'),
    (14, 'Two Week Streak', '/icons/streak-14.png'),
    (30, 'Monthly Master', '/icons/streak-30.png'),
    (60, 'Two Month Streak', '/icons/streak-60.png'),
    (90, 'Quarterly Quest', '/icons/streak-90.png'),
    (365, 'Yearly Legend', '/icons/streak-365.png');

INSERT INTO "achievements" (name, description, criteria_type, criteria_value, icon_url) VALUES
    ('First Lesson', 'Complete your first lesson', 'COMPLETE_N_COURSES', 1, '/icons/ach-first-lesson.png'),
    ('Quick Learner', 'Complete 10 lessons', 'COMPLETE_N_COURSES', 10, '/icons/ach-quick-learner.png'),
    ('Dedicated Student', 'Complete 50 lessons', 'COMPLETE_N_COURSES', 50, '/icons/ach-dedicated.png'),
    ('XP Hunter', 'Earn 1000 XP', 'EARN_XP', 1000, '/icons/ach-xp-hunter.png'),
    ('XP Master', 'Earn 5000 XP', 'EARN_XP', 5000, '/icons/ach-xp-master.png'),
    ('Hard Mode', 'Complete a HARD difficulty lesson', 'COMPLETE_COURSE_WITH_DIFFICULTY', 0, '/icons/ach-hard-mode.png');
