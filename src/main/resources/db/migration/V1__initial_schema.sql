-- ============================================================================
-- PostgreSQL Schema for Learning Platform
-- ============================================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- ENUMS
-- ============================================================================

-- User roles
CREATE TYPE "Role" AS ENUM ('USER', 'MODERATOR', 'ADMIN');

-- Reaction types for social interactions
CREATE TYPE "ReactionType" AS ENUM ('UPVOTE', 'DOWNVOTE', 'HAHA', 'LOVE');

-- Difficulty levels for lessons and exercises
CREATE TYPE "DifficultyLevel" AS ENUM ('EASY', 'MEDIUM', 'HARD', 'DIFFICULT');

-- Scope types for leaderboards and rank tracking
CREATE TYPE "ScopeType" AS ENUM ('PROVINCE', 'COUNTRY');

-- Tier types for rankings and achievements
CREATE TYPE "TierType" AS ENUM ('BRONZE', 'SILVER', 'GOLD', 'PLATINUM', 'DIAMOND', 'LEGEND');

-- Achievement criteria types
CREATE TYPE "AchievementCriteriaType" AS ENUM (
    'COMPLETE_COURSE',
    'COMPLETE_COURSE_IN_EVERY_CATEGORY',
    'COMPLETE_N_COURSES',
    'COMPLETE_ALL_IN_CATEGORY',
    'COMPLETE_ALL_IN_EVERY_CATEGORY',
    'COMPLETE_COURSE_WITH_DIFFICULTY',
    'EARN_XP'
);

-- Blog post status
CREATE TYPE "BlogStatus" AS ENUM ('PENDING', 'APPROVED', 'REJECTED');

-- Share platforms
CREATE TYPE "SharePlatform" AS ENUM ('TELEGRAM', 'FACEBOOK', 'COPY_LINK');

-- ============================================================================
-- CORE TABLES (No foreign key dependencies)
-- ============================================================================

-- Provinces table (referenced by users)
CREATE TABLE "provinces" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name_khmer VARCHAR(100) UNIQUE NOT NULL,
    name_latin VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Categories table (communities, referenced by many tables)
CREATE TABLE "categories" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    slug VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    icon_url TEXT,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Users table (central identity table)
CREATE TABLE "users" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) UNIQUE NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    avatar_url TEXT DEFAULT 'default-avatar.png',
    bio VARCHAR(160),
    gender VARCHAR(20),
    role "Role" NOT NULL DEFAULT 'USER',
    phone_number VARCHAR(20) UNIQUE,
    password TEXT,
    google_id TEXT UNIQUE,
    email TEXT UNIQUE,
    country_id VARCHAR(2) DEFAULT 'KH',
    province_id UUID REFERENCES "provinces"(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- ============================================================================
-- LEARNING DOMAIN
-- ============================================================================

-- Lessons table
CREATE TABLE "lessons" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    difficulty "DifficultyLevel" NOT NULL DEFAULT 'EASY',
    base_xp INTEGER NOT NULL DEFAULT 10,
    category_id UUID REFERENCES "categories"(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Exercises table
CREATE TABLE "exercises" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    lesson_id UUID NOT NULL REFERENCES "lessons"(id) ON DELETE CASCADE,
    question TEXT NOT NULL,
    answer_key TEXT NOT NULL,
    difficulty "DifficultyLevel" NOT NULL DEFAULT 'EASY',
    points INTEGER NOT NULL DEFAULT 5,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Lesson completions table
CREATE TABLE "lesson_completions" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    lesson_id UUID NOT NULL REFERENCES "lessons"(id) ON DELETE CASCADE,
    xp_earned INTEGER NOT NULL,
    completed_at TIMESTAMPTZ DEFAULT now(),
    created_at TIMESTAMPTZ DEFAULT now(),
    -- Prevent duplicate completions for same user+lesson
    UNIQUE (user_id, lesson_id)
);

-- ============================================================================
-- GAMIFICATION DOMAIN
-- ============================================================================

-- XP transactions ledger
CREATE TABLE "xp_transactions" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    amount INTEGER NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_id UUID,
    running_balance INTEGER NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- User gamification state
CREATE TABLE "user_gamification" (
    user_id UUID PRIMARY KEY REFERENCES "users"(id) ON DELETE CASCADE,
    total_xp INTEGER NOT NULL DEFAULT 0,
    current_streak INTEGER NOT NULL DEFAULT 0,
    highest_streak INTEGER NOT NULL DEFAULT 0,
    last_completed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Streak milestones definition
CREATE TABLE "streak_milestones" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    min_streak INTEGER UNIQUE NOT NULL,
    streak_name VARCHAR(100) NOT NULL,
    icon_url TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Rank tiers definition
CREATE TABLE "rank_tiers" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tier_name "TierType" UNIQUE NOT NULL,
    min_xp INTEGER NOT NULL,
    icon_url TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()

);

-- Rank history tracking
CREATE TABLE "rank_history" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    scope "ScopeType" NOT NULL,
    old_rank "TierType" NOT NULL,
    new_rank "TierType" NOT NULL,
    changed_at TIMESTAMPTZ DEFAULT now()
);

-- Achievements definition
CREATE TABLE "achievements" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT NOT NULL,
    criteria_type "AchievementCriteriaType" NOT NULL,
    criteria_value INTEGER,
    requires_advanced BOOLEAN NOT NULL DEFAULT FALSE,
    icon_url TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- User achievements (unlocked)
CREATE TABLE "user_achievements" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    achievement_id UUID NOT NULL REFERENCES "achievements"(id) ON DELETE CASCADE,
    unlocked_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE (user_id, achievement_id)
);

-- ============================================================================
-- LEADERBOARD DOMAIN
-- ============================================================================

-- Leaderboard snapshots (daily rankings)
CREATE TABLE "leaderboard_snapshots" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    scope "ScopeType" NOT NULL,
    scope_id TEXT NOT NULL,
    rank INTEGER NOT NULL,
    score INTEGER NOT NULL,
    ranking_tier "TierType" NOT NULL,
    snapshot_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMPTZ DEFAULT now(),
    -- One snapshot per user per scope per day
    UNIQUE (user_id, scope, scope_id, snapshot_date)
);

-- ============================================================================
-- SOCIAL DOMAIN
-- ============================================================================

-- User category memberships
CREATE TABLE "user_category_memberships" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES "categories"(id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE (user_id, category_id)
);

-- Posts table
CREATE TABLE "posts" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    media_url TEXT,
    reaction_count INTEGER NOT NULL DEFAULT 0,
    comment_count INTEGER NOT NULL DEFAULT 0,
    share_count INTEGER NOT NULL DEFAULT 0,
    author_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES "categories"(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Comments table (supports nested replies via parent_id)
CREATE TABLE "comments" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content TEXT NOT NULL,
    author_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    post_id UUID NOT NULL REFERENCES "posts"(id) ON DELETE CASCADE,
    parent_id UUID REFERENCES "comments"(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Reactions table (polymorphic: can react to posts or comments)
CREATE TABLE "reactions" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    type "ReactionType" NOT NULL,
    user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    post_id UUID REFERENCES "posts"(id) ON DELETE CASCADE,
    comment_id UUID REFERENCES "comments"(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT now(),
    -- One reaction per user per target
    UNIQUE (user_id, post_id, comment_id)
);

-- Shares table
CREATE TABLE "shares" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    platform "SharePlatform",
    user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    post_id UUID NOT NULL REFERENCES "posts"(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- ============================================================================
-- CONTENT DOMAIN (Blog)
-- ============================================================================

-- Blog posts table
CREATE TABLE "blog_posts" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    about TEXT,
    banner_url TEXT,
    author_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES "categories"(id) ON DELETE RESTRICT,
    status "BlogStatus" NOT NULL DEFAULT 'PENDING',
    view_count INTEGER NOT NULL DEFAULT 0,
    like_count INTEGER NOT NULL DEFAULT 0,
    is_ai_generated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Blog views table
CREATE TABLE "blog_views" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    blog_post_id UUID NOT NULL REFERENCES "blog_posts"(id) ON DELETE CASCADE,
    user_id UUID REFERENCES "users"(id) ON DELETE SET NULL,
    viewed_at TIMESTAMPTZ DEFAULT now()
);

-- Blog likes table
CREATE TABLE "blog_likes" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    blog_post_id UUID NOT NULL REFERENCES "blog_posts"(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    liked_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE (blog_post_id, user_id)
);

-- ============================================================================
-- AI DOMAIN (Chat)
-- ============================================================================

-- Chat sessions table
CREATE TABLE "chat_sessions" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    context_summary TEXT,
    started_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Chat messages table
CREATE TABLE "chat_messages" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID NOT NULL REFERENCES "chat_sessions"(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('user', 'assistant')),
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- ============================================================================
-- INDEXES FOR PERFORMANCE
-- ============================================================================

-- Users
CREATE INDEX idx_users_username ON "users"(username);
CREATE INDEX idx_users_email ON "users"(email);
CREATE INDEX idx_users_province_id ON "users"(province_id);
CREATE INDEX idx_users_role ON "users"(role);

-- Lessons
CREATE INDEX idx_lessons_category_id ON "lessons"(category_id);
CREATE INDEX idx_lessons_difficulty ON "lessons"(difficulty);

-- Exercises
CREATE INDEX idx_exercises_lesson_id ON "exercises"(lesson_id);

-- Lesson completions
CREATE INDEX idx_lesson_completions_user_id ON "lesson_completions"(user_id);
CREATE INDEX idx_lesson_completions_lesson_id ON "lesson_completions"(lesson_id);
CREATE INDEX idx_lesson_completions_completed_at ON "lesson_completions"(completed_at);

-- XP transactions
CREATE INDEX idx_xp_transactions_user_id ON "xp_transactions"(user_id);
CREATE INDEX idx_xp_transactions_created_at ON "xp_transactions"(created_at);
CREATE INDEX idx_xp_transactions_source_type ON "xp_transactions"(source_type);

-- User gamification
CREATE INDEX idx_user_gamification_total_xp ON "user_gamification"(total_xp DESC);
CREATE INDEX idx_user_gamification_current_streak ON "user_gamification"(current_streak DESC);

-- Rank history
CREATE INDEX idx_rank_history_user_id ON "rank_history"(user_id);
CREATE INDEX idx_rank_history_scope ON "rank_history"(scope);
CREATE INDEX idx_rank_history_changed_at ON "rank_history"(changed_at);

-- User achievements
CREATE INDEX idx_user_achievements_user_id ON "user_achievements"(user_id);
CREATE INDEX idx_user_achievements_achievement_id ON "user_achievements"(achievement_id);

-- Leaderboard snapshots
CREATE INDEX idx_leaderboard_snapshots_scope_scope_id ON "leaderboard_snapshots"(scope, scope_id);
CREATE INDEX idx_leaderboard_snapshots_snapshot_date ON "leaderboard_snapshots"(snapshot_date);
CREATE INDEX idx_leaderboard_snapshots_score ON "leaderboard_snapshots"(scope, scope_id, snapshot_date, score DESC);

-- User category memberships
CREATE INDEX idx_user_category_memberships_user_id ON "user_category_memberships"(user_id);
CREATE INDEX idx_user_category_memberships_category_id ON "user_category_memberships"(category_id);

-- Posts
CREATE INDEX idx_posts_author_id ON "posts"(author_id);
CREATE INDEX idx_posts_category_id ON "posts"(category_id);
CREATE INDEX idx_posts_created_at ON "posts"(created_at DESC);

-- Comments
CREATE INDEX idx_comments_author_id ON "comments"(author_id);
CREATE INDEX idx_comments_post_id ON "comments"(post_id);
CREATE INDEX idx_comments_parent_id ON "comments"(parent_id);
CREATE INDEX idx_comments_created_at ON "comments"(created_at DESC);

-- Reactions
CREATE INDEX idx_reactions_user_id ON "reactions"(user_id);
CREATE INDEX idx_reactions_post_id ON "reactions"(post_id);
CREATE INDEX idx_reactions_comment_id ON "reactions"(comment_id);

-- Shares
CREATE INDEX idx_shares_user_id ON "shares"(user_id);
CREATE INDEX idx_shares_post_id ON "shares"(post_id);
CREATE INDEX idx_shares_platform ON "shares"(platform);

-- Blog posts
CREATE INDEX idx_blog_posts_author_id ON "blog_posts"(author_id);
CREATE INDEX idx_blog_posts_category_id ON "blog_posts"(category_id);
CREATE INDEX idx_blog_posts_status ON "blog_posts"(status);
CREATE INDEX idx_blog_posts_created_at ON "blog_posts"(created_at DESC);

-- Blog views
CREATE INDEX idx_blog_views_blog_post_id ON "blog_views"(blog_post_id);
CREATE INDEX idx_blog_views_user_id ON "blog_views"(user_id);
CREATE INDEX idx_blog_views_viewed_at ON "blog_views"(viewed_at);

-- Blog likes
CREATE INDEX idx_blog_likes_blog_post_id ON "blog_likes"(blog_post_id);
CREATE INDEX idx_blog_likes_user_id ON "blog_likes"(user_id);

-- Chat sessions
CREATE INDEX idx_chat_sessions_user_id ON "chat_sessions"(user_id);
CREATE INDEX idx_chat_sessions_updated_at ON "chat_sessions"(updated_at DESC);

-- Chat messages
CREATE INDEX idx_chat_messages_session_id ON "chat_messages"(session_id);
CREATE INDEX idx_chat_messages_created_at ON "chat_messages"(created_at);

-- ============================================================================
-- TRIGGERS FOR updated_at
-- ============================================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply updated_at trigger to tables that need it
CREATE TRIGGER update_provinces_updated_at
    BEFORE UPDATE ON "provinces"
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_categories_updated_at
    BEFORE UPDATE ON "categories"
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON "users"
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_lessons_updated_at
    BEFORE UPDATE ON "lessons"
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_exercises_updated_at
    BEFORE UPDATE ON "exercises"
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_gamification_updated_at
    BEFORE UPDATE ON "user_gamification"
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_posts_updated_at
    BEFORE UPDATE ON "posts"
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_comments_updated_at
    BEFORE UPDATE ON "comments"
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_blog_posts_updated_at
    BEFORE UPDATE ON "blog_posts"
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_chat_sessions_updated_at
    BEFORE UPDATE ON "chat_sessions"
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- VIEWS
-- ============================================================================

-- User activity summary view
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

-- ============================================================================
-- SEED DATA (Optional - uncomment to populate initial data)
-- ============================================================================

-- Insert default rank tiers
-- INSERT INTO "rank_tiers" (tier_name, min_xp, icon_url) VALUES
--     ('BRONZE', 0, '/icons/bronze.png'),
--     ('SILVER', 100, '/icons/silver.png'),
--     ('GOLD', 500, '/icons/gold.png'),
--     ('PLATINUM', 1000, '/icons/platinum.png'),
--     ('DIAMOND', 2500, '/icons/diamond.png'),
--     ('LEGEND', 5000, '/icons/legend.png');

-- Insert default streak milestones
-- INSERT INTO "streak_milestones" (min_streak, streak_name, icon_url) VALUES
--     (1, 'First Day', '/icons/streak-1.png'),
--     (3, 'Getting Started', '/icons/streak-3.png'),
--     (7, 'Week Warrior', '/icons/streak-7.png'),
--     (14, 'Two Week Streak', '/icons/streak-14.png'),
--     (30, 'Monthly Master', '/icons/streak-30.png'),
--     (60, 'Two Month Streak', '/icons/streak-60.png'),
--     (90, 'Quarterly Quest', '/icons/streak-90.png'),
--     (365, 'Yearly Legend', '/icons/streak-365.png');
