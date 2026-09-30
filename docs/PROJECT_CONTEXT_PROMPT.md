# RIKSA Project Context Prompt
# =====================================================
# Copy everything below this line and paste it to any AI
# assistant (ChatGPT, Claude, Gemini, etc.) to give it
# full understanding of your project purpose, architecture,
# and current UI wireframe design.
# =====================================================

---

## PROJECT OVERVIEW

I am building RIKSA — a Cambodian educational learning platform API (backend) that will serve a future Flutter mobile app. This is for a competition similar to a hackathon but with a 4-month timeline. The judges are a mixed audience (technical, business, and design/UX professionals). I need the best UI/UX to win.

**Target users:** Cambodian students (primarily high school and university) learning STEM, Technology, and History.

**Core value proposition:** Gamification-driven learning with provincial leaderboards, AI tutoring, and a social community — all with Cambodian cultural identity woven throughout.

---

## TECH STACK (BACKEND — already built / in progress)

- Java 21, Spring Boot 4.0.6, Maven
- PostgreSQL 17 + Flyway migrations (V1–V10, schema fully defined)
- Spring Security + JWT + Google OAuth + Telegram OTP
- Spring Data JPA / Hibernate (ddl-auto=validate, migrations only)
- GraphQL for social/community features, REST for everything else
- Spring ApplicationEvents for cross-domain communication (event bus)
- Lombok, Spring Cache, Actuator
- Docker + docker-compose (PostgreSQL + app)

**Architecture:** Package-by-feature under `com.riksa.*`, single Maven module.

---

## BACKEND MODULES & STATUS

1. **Auth** (FULLY IMPLEMENTED) — register, login, JWT refresh, logout, Google OAuth, Telegram OTP, email verification, password reset, account lockout, rate limiting. Cambodian provinces seeded (6 provinces).

2. **Learning** (SCHEMA ONLY, code pending) — lessons, exercises, lesson completions. Students browse lessons by category/difficulty, complete them by submitting answers (70% to pass), earn XP. Admins create/manage lessons.

3. **Gamification** (PARTIAL — read-side done, write-side pending) — XP ledger, streaks, 6 rank tiers, achievements, streak milestones. The XP grant hot path (lesson completed → XP awarded → streak bumped → rank change detected → achievements evaluated) is driven by events and not yet implemented.

4. **Leaderboard** (PARTIAL) — daily snapshot job, query APIs. Score = total_xp + (current_streak * 5). Scoped by province and country (Cambodia). Trend tracking (up/down/same).

5. **Social** (IMPLEMENTED) — communities (categories), posts, comments (nested), reactions, shares. GraphQL API. Content relevance filter (STEM/Tech/History keyword check, Khmer + English).

6. **Blog** (IMPLEMENTED) — user-generated blog posts with moderation (PENDING → APPROVED/REJECTED), bad-word filter (Khmer + English), view tracking, like toggling.

7. **AI Chat** (SCHEMA ONLY, code pending) — personalized AI tutor. Builds context from user's learning activity (lessons completed, rank, achievements, leaderboard position). LLM client is a stub interface.

8. **Admin** (SCHEMA ONLY, code pending) — CRUD for lessons/exercises, categories, blog moderation, user role management. Moderator/Admin role-gated.

---

## DATABASE SCHEMA (key tables)

- **users** — id (UUID), username, display_name, avatar_url, bio, gender, role (USER/MODERATOR/ADMIN), email, phone_number, google_id, telegram_id, province_id, email_verified, failed_login_attempts, locked_until
- **provinces** — id, name_khmer, name_latin (6 Cambodian provinces seeded)
- **lessons** — id, title, description, difficulty (EASY/MEDIUM/HARD/DIFFICULT), base_xp, category_id
- **exercises** — id, lesson_id, question, answer_key, difficulty, points
- **lesson_completions** — id, user_id, lesson_id, xp_earned, completed_at (unique user+lesson)
- **xp_transactions** — id, user_id, amount, source_type, source_id, running_balance, created_at
- **user_gamification** — user_id (PK), total_xp, current_streak, highest_streak, last_completed_at, version (optimistic lock)
- **rank_tiers** — tier_name (BRONZE/SILVER/GOLD/PLATINUM/DIAMOND/LEGEND), min_xp, icon_url (6 tiers seeded: 0/100/500/1000/2500/5000 XP)
- **rank_history** — id, user_id, scope (PROVINCE/COUNTRY), old_rank, new_rank, changed_at
- **achievements** — id, name, description, criteria_type, criteria_value, requires_advanced, icon_url (6 seeded)
- **user_achievements** — id, user_id, achievement_id, unlocked_at (unique user+achievement)
- **streak_milestones** — min_streak, streak_name, icon_url (8 seeded: 1/3/7/14/30/60/90/365 days)
- **leaderboard_snapshots** — id, user_id, scope, scope_id, rank, score, ranking_tier, snapshot_date (unique user+scope+date)
- **categories** — id, name, slug, description, icon_url (community categories)
- **user_category_memberships** — id, user_id, category_id, joined_at (unique user+category)
- **posts** — id, title, content, media_url, reaction_count, comment_count, share_count, author_id, category_id
- **comments** — id, content, author_id, post_id, parent_id (nested replies)
- **reactions** — id, type (UPVOTE/DOWNVOTE/HAHA/LOVE), user_id, post_id, comment_id (unique user+target)
- **shares** — id, platform (TELEGRAM/FACEBOOK/COPY_LINK), user_id, post_id
- **blog_posts** — id, title, content, about, banner_url, cover_url, author_id, category_id, status (PENDING/APPROVED/REJECTED), view_count, like_count, is_ai_generated
- **blog_views** — id, blog_post_id, user_id, viewed_at
- **blog_likes** — id, blog_post_id, user_id, created_at (unique post+user)
- **chat_sessions** — id, user_id, context_summary, started_at, updated_at
- **chat_messages** — id, session_id, role (user/assistant), content, created_at
- **user_activity_summary** (VIEW) — user_id, username, display_name, total_xp, current_streak, highest_streak, current_rank, province_name, total_lessons_completed, total_achievements, last_completed_at

**KNOWN DB GAPS for the UI design (not yet implemented):**
- No user_follows table (profile shows following/followers)
- No users.banner_url column (profile banner image)
- No user_interests table (onboarding topic selection)
- No exercise_options / multiple-choice columns (exercises only have free-text answer_key)
- No daily_challenges table
- No post view_count column
- Reactions are currently UPVOTE/DOWNVOTE/HAHA/LOVE — the UI design uses Reddit-style upvote/downvote only (this is a design decision, backend hasn't been changed yet)

---

## UI/UX DESIGN SYSTEM

**Design philosophy:** X.com (Twitter) design language + Cambodian cultural identity + Reddit-style voting.

### Color Palette
- Primary: Deep indigo (#1A1A2E) — knowledge/depth
- Accent gold (#FFB800) — Cambodian royal gold, used for XP, achievements, rank badges
- Accent red (#E63946) — streaks/urgency
- Success green (#2EC4B6) — lesson completion, upvotes
- Light mode: White (#FFFFFF) bg, off-white (#F7F9F9) secondary, border (#EFF3F4), text (#0F1419 primary, #536471 secondary)
- Dark mode: Pure black (#000000) bg, (#16181C) hover/secondary, border (#2F3336), text (#E7E9EA primary, #71767B secondary)
- Province colors: each of the 6 provinces gets a color identity

### Typography
- Headings: Khmer-compatible display font (Battambang/Kantumruy for Khmer, Inter for English)
- Body: Inter / Noto Sans Khmer
- Numbers/stats: monospace accent for XP scores, rankings (gaming feel)

### Visual Language
- X.com-style sticky blur headers (backdrop-filter) with back button + title + subtitle
- Tab bars with gold underline indicator (like X's blue underline, but gold)
- Pill-shaped buttons (30px border radius)
- Hairline borders (light: #EFF3F4, dark: #2F3336)
- Rounded cards (16-20px radius)
- Progress rings and bars for XP/rank
- Subtle gradients on gamification elements (gold-to-amber)
- Reddit-style upvote/downvote: ▲ score ▼ (green for up, red for down, score colored by sign)
- Bottom navigation: Home | Learn | Compete | Community | Chat (SVG icons)
- Hover states: subtle background change, color shifts
- Micro-animations (to be implemented in Flutter): confetti on achievement unlock, flame grows with streak, rank badge shines

---

## WIREFRAME SCREENS (13 screens, interactive HTML)

An interactive wireframe exists at `docs/wireframes/index.html` (open in browser). It has a sidebar navigator and a phone frame (390x800px) with light/dark mode toggle. Here's every screen:

### 01. Splash
- Black background, RIKSA logo (Khmer "រ" in gold gradient square), "RIKSA" wordmark, bilingual tagline "Learn. Rise. Lead." / "សិក្សា។ លើកស្ទួយ។ ដឹកនាំ។", "Get Started" button.

### 02. Onboarding (2 screens)
- Screen A: Province selector — 6 tappable cards with crest icons (Phnom Penh, Siem Reap, Battambang, Kampong Cham, Sihanoukville, Mondulkiri). Selected card gets gold border.
- Screen B: Topic selection — STEM, Technology, History as tappable cards with icons, lesson counts, and checkmark circles. Multi-select.

### 03. Register
- RIKSA mini logo, "Create your account" heading, form fields (Full Name, Email, Password, Province readonly), gold "Create Account" button, "or" divider, "Sign in with Google" outline button, language toggle (English / ភាសាខ្មែរ).

### 04. Home / Dashboard
- Sticky header with avatar (tappable → profile), "Home" title, "For you" subtitle.
- Tab bar: For You | Lessons | Community (gold underline on active).
- Gamification banner (indigo gradient): avatar + name + rank, XP number + progress bar + "to Scholar" remaining, streak badge (🔥 13 day streak).
- "Today's Challenge" section: card with icon, title, progress (1 of 2 done), +50 XP reward badge.
- "Quick Actions" horizontal scroll: Continue (Fractions 101 · 40%), Leaderboard (#15), Achievements (3/6 earned).
- "Recent Activity" section: X-style post card from community (see post card format below).
- Bottom nav (Home active).

### 05. Learn — Lesson Catalog
- Sticky header "Lessons" with tab bar: All | STEM | Tech | History.
- Difficulty pills: All Levels | Easy | Medium | Hard.
- "Continue where you left off" card (indigo gradient): lesson title, progress bar, "2 of 5 exercises", "+80 XP".
- Lesson cards (X-style row layout): icon, title, meta (category · difficulty stars · exercise count), XP badge (+60) or done checkmark (✓ green).

### 06. Exercise — Core Loop
- Header: back arrow, progress bar (40% filled green), "2/5" question counter.
- Category tag (STEM · Math, blue pill).
- Question text (large, bold).
- 4 option cards (A/B/C/D letter circles): one highlighted green (correct, with ✓), others default.
- "Next Question →" gold button at bottom.

### 07. Completion — The Payoff
- 🎉 celebration emoji, "Lesson Complete!" heading.
- Score "80%" (large green), "4 out of 5 correct · Pass = 70%".
- XP earned badge: "🪙 +80 XP" (gold pill).
- Rank-up card (gold gradient border): "🎖️ RANK UP!" / "Rising Scholar → Scholar".
- Streak update card (red tint): "🔥 Streak: 12 → 13 days".
- Two buttons: "Back to Lessons" (outline) | "Next Lesson →" (gold).

### 08. User Profile (X-style — NEW)
- Sticky header: back arrow, "Sok Pisey" title, "42 posts" subtitle.
- Banner image area (120px, indigo gradient with 🏔️ province icon).
- Large circular avatar (88px, gold gradient, overlapping banner).
- "Edit Profile" button (pill outline, right side).
- Profile info: name, @handle, bio ("📚 Learning STEM & Tech | 🏙️ Phnom Penh | 🔥 13-day streak | Goal: Top 10"), location + join date, following/followers counts.
- Gamification stats bar (4 columns, divider lines): Total XP (5,550 gold) | Streak (13 red) | Lessons (42) | Province Rank (#15).
- Rank progression card (indigo gradient): ⭐ badge, "Rising Scholar", "2,450 XP to Scholar", progress bar (70% gold).
- Achievement showcase (horizontal scroll): 6 badges (3 earned full color, 3 locked grayscale), each with icon + name.
- Tab bar: Posts | Replies | Lessons | XP History.
- User's posts as X-style post cards with Reddit-style vote bars.

### 09. Gamification Profile
- Sticky header: back arrow, "Gamification" / "Your progress".
- Indigo gradient header: large avatar, name, province, rank badge ("⭐ Rising Scholar · Tier 3").
- Stats grid (2x2, divider lines): Total XP (5,550 gold) | Current Streak (13 red) | Highest Streak (21) | Lessons Done (42).
- Tab bar: XP History | Achievements | Rank History.
- XP history entries (X-style rows): 🪙 icon, source name + time, +amount (gold) and running balance.

### 10. Leaderboard
- Sticky header: back arrow, "Leaderboard" / "Updated 12:05 AM today".
- Scope toggle (pill segment): 🇰🇭 Cambodia | 🏙️ Phnom Penh (active).
- Top 3 podium: visual blocks (gold 1st center 72px, silver 2nd left 54px, bronze 3rd right 40px), each with avatar, name, XP.
- Ranked list (X-style rows): rank number, avatar, name + province, XP (gold). User's row highlighted with gold tint + gold avatar.
- Sticky bottom banner (black): "#15 in Phnom Penh", "5,550 XP · Rising Scholar", 📈 trend.

### 11. AI Chat Tutor
- Chat header: back arrow, 🦉 mascot (Krama Tutor owl with krama scarf), "Krama Tutor" / "● Online · Knows your progress".
- Context card (dashed border): "📋 I know about you:" with tags (Rank: Rising Scholar, 🔥 13-day streak, 42 lessons done, Struggled with fractions).
- AI message bubble (left, secondary bg): personalized message referencing recent lesson completion.
- Suggested prompts (gold outline pills): "Yes, explain it", "Give me a practice problem", "What should I learn next?".
- User message bubble (right, black bg).
- Typing indicator (3 animated dots).
- Chat input bar: text field + gold send button (➤).

### 12. Social Feed (X-style timeline + Reddit votes)
- Sticky header: avatar (tappable → profile), "Community" title.
- Tab bar: For You | Following | STEM | Tech | History.
- Compose box: avatar + text input ("Share something about your learning...") + media icons (📷 🔗 🏷️) + "Post" button (gold pill).
- Post cards (X-style, see format below) with Reddit-style vote bars.
- Floating action button (gold circle, ✏️) bottom right.

### 13. Blog
- Sticky header: back arrow, "Blog" + tab bar (All | Approved | My Posts).
- Featured blog card (large, rounded): cover image area (indigo gradient with icon), title, meta (author · views · likes).
- 2-column grid of blog mini cards: cover, title, meta.

---

## POST CARD FORMAT (X-style + Reddit votes)

Used in Home timeline, Profile posts, and Social feed:

```
[Avatar] [Display Name] [@handle · time] [Category Badge]
         [Post title (if any)]
         [Post content text]
         [Optional: media image area]
         ▲  [score]  ▼  |  💬 [comments]  📤
```

- Avatar: 44px circle, initials, indigo gradient (or gold for current user)
- Name: bold, primary color. Handle + time: secondary color
- Category badge: colored pill (STEM=blue, Tech=green, History=amber)
- Vote bar: ▲ upvote arrow (green when active) + score (green if positive, red if negative) + ▼ downvote arrow (red when active) + divider + 💬 comment count + 📤 share
- Entire card has hairline bottom border, hover background change

---

## DEMO FLOW (for competition judges — 3 minutes)

1. Start as new student in Phnom Penh (onboarding — 15s)
2. Complete a math lesson, earn XP, see streak update (core loop — 45s)
3. Rank up triggers celebration animation (wow moment — 15s)
4. Check leaderboard — see yourself climb (social proof — 15s)
5. Ask AI tutor for help on tricky concept (technical wow — 20s)
6. Post in community about the lesson, get upvotes (social engagement — 15s)
7. End on gamification dashboard showing full journey (45s)

---

## WHAT I NEED FROM YOU (the AI assistant)

When I ask you to help with this project, I need you to understand:

1. **This is a design-first phase.** The backend exists but I'm currently focused on UI/UX wireframes and design decisions. Don't write backend code unless I explicitly ask.
2. **The wireframe at `docs/wireframes/index.html` is the current source of truth for the UI.** It's an interactive HTML mockup with 13 screens, light/dark mode, and clickable navigation.
3. **The design language is X.com + Cambodian identity + Reddit voting.** Sticky blur headers, tab bars with gold underline, pill buttons, hairline borders, Reddit-style ▲ score ▼ on posts.
4. **Cultural identity matters.** Khmer text, province crests, gold (royal) accent color, krama scarf on AI mascot — these are intentional differentiators for the competition.
5. **The database schema is already defined** (V1–V10 Flyway migrations). The UI should align with what the DB can support, and any gaps should be flagged.
6. **Flutter is the target frontend.** The wireframes are HTML mockups that will be implemented in Flutter later. Design decisions should be Flutter-friendly (e.g., backdrop blur, sliver headers, custom tab bars are all doable in Flutter).

When I ask you to:
- **Improve a screen** → reference the screen number and suggest specific layout/UX changes
- **Add a new screen** → follow the existing design system (colors, typography, components)
- **Check DB alignment** → compare UI elements against the schema tables listed above
- **Design a new flow** → consider the demo flow and competition context
- **Write Flutter code** → use the design system values (hex colors, border radii, component patterns) from the wireframe