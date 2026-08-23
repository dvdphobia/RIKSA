-- ============================================================================
-- V22: Comprehensive Demo Data for Flutter App
-- Seeds: Categories, Users, Gamification, Social Feed, Comments, Reactions,
--        Blog Posts, Blog Views/Likes, Lessons, Exercises, Completions,
--        XP Transactions, User Achievements, Rank History, Leaderboards.
-- Password for all demo accounts: password123
-- BCrypt: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
-- ============================================================================

-- ── 1. CATEGORIES ───────────────────────────────────────────────────────────
INSERT INTO "categories" (id, name, slug, description, icon_url) VALUES
    ('c0000000-0000-0000-0000-000000000001', 'Mathematics', 'mathematics', 'Calculus, Algebra, Geometry, and Advanced Problem Solving for High School & University.', 'https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=150'),
    ('c0000000-0000-0000-0000-000000000002', 'Computer Science', 'computer-science', 'Algorithms, Flutter, Web Development, and Software Engineering in Khmer.', 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=150'),
    ('c0000000-0000-0000-0000-000000000003', 'Khmer Literature', 'khmer-literature', 'Khmer grammar, classical poetry, essay writing, and cultural linguistic heritage.', 'https://images.unsplash.com/photo-1455390582262-044cdead277a?w=150'),
    ('c0000000-0000-0000-0000-000000000004', 'Physics', 'physics', 'Classical mechanics, thermodynamics, electromagnetism, and modern physics.', 'https://images.unsplash.com/photo-1636466497217-26a8cbeaf0aa?w=150'),
    ('c0000000-0000-0000-0000-000000000005', 'English Learning', 'english-learning', 'Grammar, Academic Vocabulary, IELTS preparation, and conversational English.', 'https://images.unsplash.com/photo-1546410531-bb4caa6b424d?w=150'),
    ('c0000000-0000-0000-0000-000000000006', 'Chemistry', 'chemistry', 'Organic, Inorganic chemistry, balancing equations, and laboratory science.', 'https://images.unsplash.com/photo-1603126857599-f6e157fa2fe6?w=150'),
    ('c0000000-0000-0000-0000-000000000007', 'Biology', 'biology', 'Cell biology, human genetics, physiology, botany, and ecology.', 'https://images.unsplash.com/photo-1530026405186-ed1f139313f8?w=150'),
    ('c0000000-0000-0000-0000-000000000008', 'History & Heritage', 'history-heritage', 'Angkorian civilization, Southeast Asian history, and cultural heritage.', 'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=150'),
    ('c0000000-0000-0000-0000-000000000009', 'General Discussion', 'general-discussion', 'Community study tips, daily motivations, student life, and career guidance.', 'https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=150')
ON CONFLICT (slug) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    icon_url = EXCLUDED.icon_url,
    updated_at = now();

-- Update existing Health and Travel categories if present
UPDATE "categories" SET description = 'Health science, wellness, nutrition, and student mental health.', icon_url = 'https://images.unsplash.com/photo-1505751172876-fa1923c5c528?w=150' WHERE slug = 'health';
UPDATE "categories" SET description = 'Study abroad, field trips, Cambodian cultural tourism, and discoveries.', icon_url = 'https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=150' WHERE slug = 'travel';


-- ── 2. DEMO USERS ───────────────────────────────────────────────────────────
-- BCrypt password 'password123': $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO "users" (id, username, display_name, email, password, role, avatar_url, bio, gender, phone_number, phone_verified, email_verified, country_id, province_id) VALUES
    ('a0000000-0000-0000-0000-000000000011', 'demo.user', 'Demo Student (You)', 'demo@riksa.local', '$2b$12$A4TmBWy.wKWzGtxgV/r.L.QxnNS6FdF1c3YQmjdL0C7Y0WW0ur9JK', 'USER', '/avatars/avatar-12.png', 'Primary demo account for exploring Flutter app features 📱', 'OTHER', '+85512000011', true, true, 'KH', '00000000-0000-0000-0000-000000000015'),
    ('a0000000-0000-0000-0000-000000000001', 'sokha.cambodia', 'Sokha Chan (Top Learner)', 'sokha@riksa.local', '$2b$12$A4TmBWy.wKWzGtxgV/r.L.QxnNS6FdF1c3YQmjdL0C7Y0WW0ur9JK', 'USER', '/avatars/avatar-1.png', 'National exam candidate & Math Olympiad enthusiast 🥇', 'MALE', '+85512000001', true, true, 'KH', '00000000-0000-0000-0000-000000000015'),
    ('a0000000-0000-0000-0000-000000000002', 'bopha.stem', 'Bopha Pich (Siem Reap)', 'bopha@riksa.local', '$2b$12$A4TmBWy.wKWzGtxgV/r.L.QxnNS6FdF1c3YQmjdL0C7Y0WW0ur9JK', 'USER', '/avatars/avatar-2.png', 'Passionate about Physics & Chemistry experiments 🔬', 'FEMALE', '+85512000002', true, true, 'KH', '00000000-0000-0000-0000-000000000021'),
    ('a0000000-0000-0000-0000-000000000003', 'dara.coder', 'Dara Seng (Developer)', 'dara@riksa.local', '$2b$12$A4TmBWy.wKWzGtxgV/r.L.QxnNS6FdF1c3YQmjdL0C7Y0WW0ur9JK', 'USER', '/avatars/avatar-3.png', 'Building open source mobile & web tools with Flutter 💻', 'MALE', '+85512000003', true, true, 'KH', '00000000-0000-0000-0000-000000000002'),
    ('a0000000-0000-0000-0000-000000000004', 'kannitha.khmer', 'Kannitha Chea (Literature)', 'kannitha@riksa.local', '$2b$12$A4TmBWy.wKWzGtxgV/r.L.QxnNS6FdF1c3YQmjdL0C7Y0WW0ur9JK', 'USER', '/avatars/avatar-4.png', 'Khmer literature researcher & ancient poetry lover 📚', 'FEMALE', '+85512000004', true, true, 'KH', '00000000-0000-0000-0000-000000000008'),
    ('a0000000-0000-0000-0000-000000000005', 'visal.kampot', 'Visal Meng (Kampot)', 'visal@riksa.local', '$2b$12$A4TmBWy.wKWzGtxgV/r.L.QxnNS6FdF1c3YQmjdL0C7Y0WW0ur9JK', 'USER', '/avatars/avatar-5.png', 'High school senior focusing on Biology & Chemistry 🌿', 'MALE', '+85512000005', true, true, 'KH', '00000000-0000-0000-0000-000000000007'),
    ('a0000000-0000-0000-0000-000000000006', 'channary.kpc', 'Channary Nuon (KPC)', 'channary@riksa.local', '$2b$12$A4TmBWy.wKWzGtxgV/r.L.QxnNS6FdF1c3YQmjdL0C7Y0WW0ur9JK', 'USER', '/avatars/avatar-6.png', 'Grade 12 student aiming for University scholarships 🎯', 'FEMALE', '+85512000006', true, true, 'KH', '00000000-0000-0000-0000-000000000003'),
    ('a0000000-0000-0000-0000-000000000007', 'rithy.shv', 'Rithy Heng (Sihanoukville)', 'rithy@riksa.local', '$2b$12$A4TmBWy.wKWzGtxgV/r.L.QxnNS6FdF1c3YQmjdL0C7Y0WW0ur9JK', 'USER', '/avatars/avatar-7.png', 'English enthusiast preparing for IELTS 7.5+ 🌏', 'MALE', '+85512000007', true, true, 'KH', '00000000-0000-0000-0000-000000000016'),
    ('a0000000-0000-0000-0000-000000000008', 'sreymom.takeo', 'Sreymom Ouk (Takeo)', 'sreymom@riksa.local', '$2b$12$A4TmBWy.wKWzGtxgV/r.L.QxnNS6FdF1c3YQmjdL0C7Y0WW0ur9JK', 'USER', '/avatars/avatar-8.png', 'Daily learner studying 2 hours every evening ⏳', 'FEMALE', '+85512000008', true, true, 'KH', '00000000-0000-0000-0000-000000000024'),
    ('a0000000-0000-0000-0000-000000000009', 'kosal.svayrieng', 'Kosal Lim (Svay Rieng)', 'kosal@riksa.local', '$2b$12$A4TmBWy.wKWzGtxgV/r.L.QxnNS6FdF1c3YQmjdL0C7Y0WW0ur9JK', 'USER', '/avatars/avatar-9.png', 'Aspiring software developer & algorithms fan 🚀', 'MALE', '+85512000009', true, true, 'KH', '00000000-0000-0000-0000-000000000023'),
    ('a0000000-0000-0000-0000-000000000010', 'rathana.pursat', 'Rathana Mao (Pursat)', 'rathana@riksa.local', '$2b$12$A4TmBWy.wKWzGtxgV/r.L.QxnNS6FdF1c3YQmjdL0C7Y0WW0ur9JK', 'USER', '/avatars/avatar-10.png', 'History enthusiast & cultural heritage explorer 🏛️', 'MALE', '+85512000010', true, true, 'KH', '00000000-0000-0000-0000-000000000019'),
    ('a0000000-0000-0000-0000-000000000012', 'teacher.sophea', 'Teacher Sophea (Math Expert)', 'sophea@riksa.local', '$2b$12$A4TmBWy.wKWzGtxgV/r.L.QxnNS6FdF1c3YQmjdL0C7Y0WW0ur9JK', 'MODERATOR', '/avatars/avatar-15.png', 'High school math educator & BakII exam tutor 📐', 'FEMALE', '+85512000012', true, true, 'KH', '00000000-0000-0000-0000-000000000015'),
    ('a0000000-0000-0000-0000-000000000013', 'admin.riksa', 'Riksa Platform Admin', 'admin.riksa@riksa.local', '$2b$12$A4TmBWy.wKWzGtxgV/r.L.QxnNS6FdF1c3YQmjdL0C7Y0WW0ur9JK', 'ADMIN', '/avatars/avatar-30.png', 'System administrator for Riksa Education Platform 🛡️', 'OTHER', '+85512000013', true, true, 'KH', '00000000-0000-0000-0000-000000000015')
ON CONFLICT (id) DO UPDATE
SET username = EXCLUDED.username,
    display_name = EXCLUDED.display_name,
    password = EXCLUDED.password,
    role = EXCLUDED.role,
    avatar_url = EXCLUDED.avatar_url,
    bio = EXCLUDED.bio,
    gender = EXCLUDED.gender,
    phone_number = EXCLUDED.phone_number,
    phone_verified = true,
    email_verified = true,
    country_id = 'KH',
    province_id = EXCLUDED.province_id,
    updated_at = now();

-- Ensure all existing users have valid demo password, valid province and avatar
UPDATE "users"
SET password = '$2b$12$A4TmBWy.wKWzGtxgV/r.L.QxnNS6FdF1c3YQmjdL0C7Y0WW0ur9JK',
    avatar_url = CASE
        WHEN avatar_url NOT LIKE '/avatars/%' THEN '/avatars/avatar-1.png'
        ELSE avatar_url
    END,
    province_id = COALESCE(province_id, '00000000-0000-0000-0000-000000000015'),
    email_verified = true,
    phone_verified = true
WHERE password IS NULL OR password = '' OR avatar_url NOT LIKE '/avatars/%';


-- ── 3. USER GAMIFICATION (STREAK, XP, TIERS) ────────────────────────────────
INSERT INTO "user_gamification" (user_id, total_xp, current_streak, highest_streak, last_completed_at, version) VALUES
    ('a0000000-0000-0000-0000-000000000001', 5420, 42, 45, now() - interval '2 hours', 1),
    ('a0000000-0000-0000-0000-000000000002', 4180, 28, 30, now() - interval '5 hours', 1),
    ('a0000000-0000-0000-0000-000000000012', 3800, 35, 40, now() - interval '1 hour', 1),
    ('a0000000-0000-0000-0000-000000000003', 3250, 21, 25, now() - interval '4 hours', 1),
    ('a0000000-0000-0000-0000-000000000013', 2500, 20, 20, now() - interval '6 hours', 1),
    ('a0000000-0000-0000-0000-000000000004', 2150, 15, 18, now() - interval '8 hours', 1),
    ('a0000000-0000-0000-0000-000000000005', 1840, 12, 14, now() - interval '12 hours', 1),
    ('a0000000-0000-0000-0000-000000000011', 1520, 14, 14, now() - interval '30 minutes', 1),
    ('a0000000-0000-0000-0000-000000000006', 1220,  9, 10, now() - interval '14 hours', 1),
    ('a0000000-0000-0000-0000-000000000007',  880,  7,  8, now() - interval '18 hours', 1),
    ('a0000000-0000-0000-0000-000000000008',  650,  5,  7, now() - interval '20 hours', 1),
    ('a0000000-0000-0000-0000-000000000009',  340,  4,  5, now() - interval '22 hours', 1),
    ('a0000000-0000-0000-0000-000000000010',  160,  3,  3, now() - interval '23 hours', 1)
ON CONFLICT (user_id) DO UPDATE
SET total_xp = EXCLUDED.total_xp,
    current_streak = EXCLUDED.current_streak,
    highest_streak = EXCLUDED.highest_streak,
    last_completed_at = EXCLUDED.last_completed_at,
    updated_at = now();


-- ── 4. CATEGORY MEMBERSHIPS ─────────────────────────────────────────────────
INSERT INTO "user_category_memberships" (user_id, category_id) VALUES
    ('a0000000-0000-0000-0000-000000000011', 'c0000000-0000-0000-0000-000000000001'),
    ('a0000000-0000-0000-0000-000000000011', 'c0000000-0000-0000-0000-000000000002'),
    ('a0000000-0000-0000-0000-000000000011', 'c0000000-0000-0000-0000-000000000003'),
    ('a0000000-0000-0000-0000-000000000011', 'c0000000-0000-0000-0000-000000000005'),
    ('a0000000-0000-0000-0000-000000000011', 'c0000000-0000-0000-0000-000000000009'),
    ('a0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001'),
    ('a0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000002'),
    ('a0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000001'),
    ('a0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000004'),
    ('a0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000002'),
    ('a0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000003'),
    ('a0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000004'),
    ('a0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000006'),
    ('a0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000005'),
    ('a0000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000007'),
    ('a0000000-0000-0000-0000-000000000012', 'c0000000-0000-0000-0000-000000000001'),
    ('a0000000-0000-0000-0000-000000000012', 'c0000000-0000-0000-0000-000000000002')
ON CONFLICT (user_id, category_id) DO NOTHING;


-- ── 5. SOCIAL FEED POSTS ────────────────────────────────────────────────────
INSERT INTO "posts" (id, author_id, category_id, title, content, media_url, reaction_count, comment_count, share_count, status, created_at, updated_at) VALUES
    ('f0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001',
     'គន្លឹះដោះស្រាយសមីការឌីផេរ៉ង់ស្យែលលំដាប់ទី២ សម្រាប់ប្រឡងបាក់ឌុប 📐',
     'សួស្តីអ្នកទាំងអស់គ្នា! ថ្ងៃនេះខ្ញុំសូមចែករំលែកវិធីសាស្ត្រងាយៗក្នុងការដោះស្រាយសមីការឌីផេរ៉ង់ស្យែលលំដាប់ទី២ អូម៉ូសែន៖ ay'''' + by'' + cy = 0 ។\n\nជំហានទី១៖ រកសមីការសំគាល់ ar^2 + br + c = 0\nជំហានទី២៖ គណនា Delta = b^2 - 4ac\n- បើ Delta > 0: y = c1*e^(r1*x) + c2*e^(r2*x)\n- បើ Delta = 0: y = (c1 + c2*x)*e^(r*x)\n- បើ Delta < 0: y = e^(alpha*x)*(c1*cos(beta*x) + c2*sin(beta*x))\n\nសង្ឃឹមថាជួយបានច្រើនសម្រាប់ការប្រឡងខាងមុខ!',
     'https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=600', 48, 8, 12, 'VISIBLE', now() - interval '2 hours', now() - interval '2 hours'),

    ('f0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000002',
     'Mastering State Management in Flutter: Riverpod vs Bloc in 2026 🚀',
     'For Cambodian mobile developers building scalable Flutter apps, Riverpod provides compile-safe dependency injection and state management without the verbose boilerplate of older patterns.\n\nKey advantages:\n1. No BuildContext required for state reads/listeners\n2. Built-in caching, autoDispose, and debounce\n3. Effortless testing with container overrides\n\nWho here is using Riverpod in production? Let me know your experiences!',
     'https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=600', 65, 14, 18, 'VISIBLE', now() - interval '5 hours', now() - interval '5 hours'),

    ('f0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000003',
     'រចនាសម្ព័ន្ធកំណាព្យខ្មែរ៖ បទពាក្យ៧ និងក្បួនចួនកាព្យ 📜',
     'នៅក្នុងអក្សរសាស្ត្រខ្មែរ បទពាក្យ៧ គឺជាបទកំណាព្យដ៏ពេញនិយមបំផុតដែលមានក្បួនចួនច្បាស់លាស់៖\n- ពាក្យទី៧ នៃឃ្លាទី១ ត្រូវចួននឹងពាក្យទី៤ នៃឃ្លាទី២\n- ពាក្យទី៧ នៃឃ្លាទី២ ត្រូវចួននឹងពាក្យទី៧ នៃឃ្លាទី៣ និងពាក្យទី៤ នៃឃ្លាទី៤\n\nតើបងប្អូនណាខ្លះចូលចិត្តតែងកំណាព្យ? សូមចែករំលែកស្នាដៃនៅខាងក្រោម!',
     'https://images.unsplash.com/photo-1455390582262-044cdead277a?w=600', 32, 6, 5, 'VISIBLE', now() - interval '10 hours', now() - interval '10 hours'),

    ('f0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000004',
     'ច្បាប់ញូតុនទាំង ៣ និងការអនុវត្តជាក់ស្តែងក្នុងជីវភាពប្រចាំថ្ងៃ 🚲',
     'ច្បាប់ទី១ (ច្បាប់និចលភាព)៖ វត្ថុមួយរក្សាភាពនៅស្ងៀម ឬចលនាត្រង់ស្មើ លុះត្រាតែមានកម្លាំងក្រៅមកមានអំពើលើវា។\nច្បាប់ទី២៖ F = m * a (កម្លាំងស្មើនឹងម៉ាសគុណនឹងសំទុះ)\nច្បាប់ទី៣៖ សកម្មភាព និងប្រតិកម្ម (Action = -Reaction)\n\nពេលយើងជិះកង់ ឬជិះម៉ូតូ ច្បាប់ទាំងបីនេះដំណើរការជានិច្ច!',
     'https://images.unsplash.com/photo-1636466497217-26a8cbeaf0aa?w=600', 41, 7, 9, 'VISIBLE', now() - interval '1 day', now() - interval '1 day'),

    ('f0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000005',
     'Top 10 Collocations for IELTS Academic Writing Band 7.5+ ✍️',
     'Boost your Lexical Resource score with these natural academic collocations:\n1. Play a pivotal role in...\n2. Exert a profound influence on...\n3. Foster an environment of...\n4. Reap substantial benefits...\n5. Pose a grave threat to...\n\nSave this post for your revision!',
     'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=600', 57, 11, 23, 'VISIBLE', now() - interval '1 day', now() - interval '1 day'),

    ('f0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000012', 'c0000000-0000-0000-0000-000000000001',
     'របៀបរៀបចំកាលវិភាគរៀនត្រៀមប្រឡងឆមាស និងបាក់ឌុបឲ្យមានប្រសិទ្ធភាព 📅',
     'សួស្តីសិស្សានុសិស្សទាំងអស់គ្នា! កុំរៀនទន្ទេញតែមួយយប់មុនប្រឡង។\nអនុវត្តបច្ចេកទេស Pomodoro (រៀន ២៥នាទី សម្រាក ៥នាទី) និង Spaced Repetition ដើម្បីចងចាំបានយូរ។ បង្កើតទម្លាប់រៀនរាល់ថ្ងៃយ៉ាងហោចណាស់ ៤៥នាទីនៅលើ Riksa!',
     'https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=600', 79, 16, 31, 'VISIBLE', now() - interval '2 days', now() - interval '2 days'),

    ('f0000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000009',
     'Weekly Study Challenge: 14 Days Streak Goal! 🔥 Are you in?',
     'Consistency beats talent when talent doesn''t work consistently! Join my 14-day study streak challenge on Riksa. Drop your current streak in the comments and let''s motivate each other! 💪',
     'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=600', 92, 22, 15, 'VISIBLE', now() - interval '3 days', now() - interval '3 days')
ON CONFLICT (id) DO UPDATE
SET title = EXCLUDED.title,
    content = EXCLUDED.content,
    media_url = EXCLUDED.media_url,
    reaction_count = EXCLUDED.reaction_count,
    comment_count = EXCLUDED.comment_count,
    share_count = EXCLUDED.share_count,
    updated_at = now();


-- ── 6. POST COMMENTS & REPLIES ──────────────────────────────────────────────
INSERT INTO "comments" (id, post_id, author_id, parent_id, content, status, created_at, updated_at) VALUES
    -- Comments on Post 1 (Math)
    ('e0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002', NULL,
     'អរគុណច្រើនបង Sokha! ចំណុចករណី Delta < 0 ខ្ញុំតែងតែច្រឡំ alpha និង beta ឥឡូវយល់ច្បាស់ហើយ ✨', 'VISIBLE', now() - interval '90 minutes', now() - interval '90 minutes'),
    ('e0000000-0000-0000-0000-000000000002', 'f0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001',
     'រីករាយដែលបានជួយ Bopha! ចាំចាំថា alpha = -b/(2a) ហើយ beta = sqrt(|Delta|)/(2a) ណា៎ 😊', 'VISIBLE', now() - interval '75 minutes', now() - interval '75 minutes'),
    ('e0000000-0000-0000-0000-000000000003', 'f0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000011', NULL,
     'អត្ថបទមានប្រយោជន៍ខ្លាំងណាស់ ខ្ញុំបាន Bookmark ទុកមើលពេលរំលឹកមេរៀន!', 'VISIBLE', now() - interval '45 minutes', now() - interval '45 minutes'),

    -- Comments on Post 2 (Flutter)
    ('e0000000-0000-0000-0000-000000000004', 'f0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000011', NULL,
     'Great post! We are building the Riksa Flutter client with Riverpod and it has been super clean and modular 🚀', 'VISIBLE', now() - interval '4 hours', now() - interval '4 hours'),
    ('e0000000-0000-0000-0000-000000000005', 'f0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000003', 'e0000000-0000-0000-0000-000000000004',
     'Awesome! The riverpod_generator package makes code generation and async value handling seamless too.', 'VISIBLE', now() - interval '3 hours', now() - interval '3 hours'),

    -- Comments on Post 7 (Streak Challenge)
    ('e0000000-0000-0000-0000-000000000006', 'f0000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000001', NULL,
     'Current streak: 42 days! Aiming for 60 days Milestone! 🔥', 'VISIBLE', now() - interval '2 days', now() - interval '2 days'),
    ('e0000000-0000-0000-0000-000000000007', 'f0000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000011', NULL,
     'Count me in! Day 14 today and pushing strong! 💪', 'VISIBLE', now() - interval '1 day', now() - interval '1 day')
ON CONFLICT (id) DO UPDATE
SET content = EXCLUDED.content,
    updated_at = now();


-- ── 7. POST & COMMENT REACTIONS ─────────────────────────────────────────────
INSERT INTO "reactions" (id, user_id, post_id, comment_id, type, created_at) VALUES
    ('fa000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000011', 'f0000000-0000-0000-0000-000000000001', NULL, 'LOVE', now() - interval '1 hour'),
    ('fa000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002', 'f0000000-0000-0000-0000-000000000001', NULL, 'UPVOTE', now() - interval '80 minutes'),
    ('fa000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000011', 'f0000000-0000-0000-0000-000000000002', NULL, 'UPVOTE', now() - interval '4 hours'),
    ('fa000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000002', NULL, 'LOVE', now() - interval '4 hours'),
    ('fa000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000007', NULL, 'LOVE', now() - interval '1 day'),
    ('fa000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000011', NULL, 'e0000000-0000-0000-0000-000000000001', 'UPVOTE', now() - interval '50 minutes')
ON CONFLICT (id) DO NOTHING;


-- ── 8. BLOG POSTS ───────────────────────────────────────────────────────────
INSERT INTO "blog_posts" (id, title, about, content, cover_url, banner_url, author_id, category_id, status, view_count, like_count, is_ai_generated, created_at, updated_at) VALUES
    ('bb000000-0000-0000-0000-000000000001',
     'Mastering the BakII Examination: A Complete Strategic Guide 🎓',
     'A step-by-step roadmap for Grade 12 students covering time management, core subject formulas, and stress reduction.',
     '# Mastering the BakII Examination in 2026\n\nThe National High School Exam (បាក់ឌុប) is a critical milestone for every Cambodian student. With the right strategy and systematic daily practice, earning an **Grade A** is fully within your reach.\n\n## 1. Core Subject Priority Weighting\n- **Mathematics:** Focus heavily on Integrals, Complex Numbers, Differential Equations, and Probability.\n- **Physics & Chemistry:** Memorize essential reaction formulas and Newton/Faraday laws.\n- **Khmer Literature:** Practice standard essay structure: opening, body arguments with historical/literary quotes, and reflective conclusion.\n\n## 2. The 3-Month Study Timeline\n1. **Month 1:** Comprehensive theory review and formula summary sheets.\n2. **Month 2:** Solving past 10 years exam papers under timed conditions.\n3. **Month 3:** Mock tests, identifying weak topics, and active rest.\n\n> "Consistency is not about perfection; it is about showing up every single day."\n\nStay focused, maintain your study streaks on Riksa, and best of luck to all candidates!',
     'https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=600',
     'https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=1200',
     'a0000000-0000-0000-0000-000000000012', 'c0000000-0000-0000-0000-000000000001', 'APPROVED', 1420, 89, false, now() - interval '5 days', now() - interval '5 days'),

    ('bb000000-0000-0000-0000-000000000002',
     'Why Flutter is Transforming Mobile Development in Cambodia 📱',
     'An in-depth look at Flutter and Dart ecosystems, local developer demand, and building modern apps for iOS and Android.',
     '# Why Flutter is the Top Choice for Cambodian Developers\n\nCross-platform development has revolutionized how tech startups and enterprises deliver mobile applications in Southeast Asia. Flutter, powered by Google and Dart, offers unparalleled advantages:\n\n### Key Highlights:\n- **Single Codebase:** 95%+ code sharing across iOS, Android, and Web.\n- **High Performance:** Native compiled ARM code running smoothly at 60/120 FPS.\n- **Rich UI Ecosystem:** Customizable Material 3 and Cupertino widgets.\n- **Thriving Local Community:** Growing demand in Phnom Penh tech hubs for skilled Flutter engineers.\n\nStart your mobile engineering journey today by following our interactive CS modules on Riksa!',
     'https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=600',
     'https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=1200',
     'a0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000002', 'APPROVED', 980, 74, false, now() - interval '4 days', now() - interval '4 days'),

    ('bb000000-0000-0000-0000-000000000003',
     'The Cultural Genius of Angkorian Architecture & Engineering 🏛️',
     'Exploring the hydraulic engineering, astronomical alignments, and sandstone masonry of Angkor Wat.',
     '# Angkor Wat: Ancient Engineering Marvel\n\nBuilt in the early 12th century under King Suryavarman II, Angkor Wat remains the largest religious monument in the world. Modern LIDAR scans and archaeological studies reveal remarkable sophistication:\n\n1. **Hydraulic Grids:** Massive reservoirs (Barays) and moats engineered to stabilize the sandstone foundation groundwater.\n2. **Solar Equinox Alignment:** The central tower perfectly aligns with the rising sun during the spring and autumn equinoxes.\n3. **Bas-Relief Storytelling:** Over 1,200 square meters of intricate historical carvings.\n\nUnderstanding our heritage empowers us to innovate for our future.',
     'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=600',
     'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=1200',
     'a0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000008', 'APPROVED', 650, 52, false, now() - interval '3 days', now() - interval '3 days'),

    ('bb000000-0000-0000-0000-000000000004',
     '10 Habit-Building Techniques to Maintain a 100-Day Study Streak 🔥',
     'Practical behavioral psychology techniques to build discipline, defeat procrastination, and sustain learning velocity.',
     '# How to Build an Unbreakable Daily Learning Habit\n\nMotivation gets you started; habit keeps you going. Here are 5 battle-tested rules from top students:\n\n- **The 2-Minute Rule:** When starting feels daunting, commit to studying for just 2 minutes.\n- **Habit Stacking:** Attach your study time immediately after an existing habit (e.g. Right after morning coffee).\n- **Visual Tracking:** Watching your streak counter increase triggers positive dopamine loops.\n- **Pre-commitment:** Plan tomorrow''s lessons before going to bed.\n\nTrack your progress daily on Riksa and watch your XP climb!',
     'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=600',
     'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=1200',
     'a0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000009', 'APPROVED', 1890, 125, false, now() - interval '2 days', now() - interval '2 days'),

    ('bb000000-0000-0000-0000-000000000005',
     'Mastering IELTS Academic Vocabulary: High-Frequency Collocations 📝',
     'Elevate your speaking and writing band scores with contextual idioms and natural academic expressions.',
     '# Essential Academic Collocations for IELTS Band 7.5+\n\nExaminers assess Lexical Resource by looking for precise word partnerships rather than obscure, awkwardly forced vocabulary.\n\n### Must-Know Collocations:\n- **Substantial evidence** (instead of *big proof*)\n- **Exert a significant impact on** (instead of *have a big effect*)\n- **Mitigate climate risks** (instead of *make climate problems smaller*)\n- **A hotly debated issue** (instead of *people argue a lot*)\n\nPractice writing 3 sentences using these phrases in the comments below!',
     'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=600',
     'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=1200',
     'a0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000005', 'APPROVED', 1150, 93, false, now() - interval '1 day', now() - interval '1 day')
ON CONFLICT (id) DO UPDATE
SET title = EXCLUDED.title,
    about = EXCLUDED.about,
    content = EXCLUDED.content,
    cover_url = EXCLUDED.cover_url,
    banner_url = EXCLUDED.banner_url,
    view_count = EXCLUDED.view_count,
    like_count = EXCLUDED.like_count,
    updated_at = now();

-- Blog likes for demo user and others
INSERT INTO "blog_likes" (blog_post_id, user_id, liked_at) VALUES
    ('bb000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000011', now() - interval '2 days'),
    ('bb000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000011', now() - interval '1 day'),
    ('bb000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000011', now() - interval '12 hours')
ON CONFLICT (blog_post_id, user_id) DO NOTHING;


-- ── 9. LESSONS & EXERCISES ──────────────────────────────────────────────────
INSERT INTO "lessons" (id, title, description, difficulty, base_xp, category_id, reflection_prompt, created_at, updated_at) VALUES
    ('1e550000-0000-0000-0000-000000000001', 'Derivatives & Tangent Line Equations (ដេរីវេ)',
     'Learn the fundamental derivative rules, power rule, product rule, and how to find the equation of a tangent line.',
     'EASY', 20, 'c0000000-0000-0000-0000-000000000001',
     'How does the derivative geometrically describe instantaneous rate of change in real life?',
     now() - interval '10 days', now() - interval '10 days'),

    ('1e550000-0000-0000-0000-000000000002', 'Definite Integrals & Area Under Curves (អាំងតេក្រាល)',
     'Calculate definite integrals using the Fundamental Theorem of Calculus and compute bounded areas between functions.',
     'MEDIUM', 30, 'c0000000-0000-0000-0000-000000000001',
     'Explain the relationship between differentiation and integration as inverse mathematical operations.',
     now() - interval '9 days', now() - interval '9 days'),

    ('1e550000-0000-0000-0000-000000000003', 'Introduction to Dart & Flutter Widget Trees',
     'Understand StatelessWidget, StatefulWidget, build contexts, and composing modern responsive layouts.',
     'EASY', 25, 'c0000000-0000-0000-0000-000000000002',
     'Why is the concept of declarative UI different from traditional imperative UI programming?',
     now() - interval '8 days', now() - interval '8 days'),

    ('1e550000-0000-0000-0000-000000000004', 'State Management & Async Data Streams in Flutter',
     'Manage global app state, handle async networking with Riverpod, and stream real-time updates.',
     'HARD', 40, 'c0000000-0000-0000-0000-000000000002',
     'What are the key trade-offs between local widget state and application-level global state?',
     now() - interval '7 days', now() - interval '7 days'),

    ('1e550000-0000-0000-0000-000000000005', 'Newtonian Mechanics & Force Equilibrium (ច្បាប់ញូតុន)',
     'Analyze free-body diagrams, static equilibrium, and dynamic force equations for multi-body systems.',
     'EASY', 20, 'c0000000-0000-0000-0000-000000000004',
     'How does friction alter the net acceleration of an object on an inclined plane?',
     now() - interval '6 days', now() - interval '6 days'),

    ('1e550000-0000-0000-0000-000000000006', 'Khmer Classical Poetry & Rhyme Meter (ក្បួនចួនកំណាព្យ)',
     'Explore the structural rhyme rules and metric meters of Pathyak 7, Brahmagit, and Kakagate poetry.',
     'EASY', 20, 'c0000000-0000-0000-0000-000000000003',
     'How does internal rhyme rhythm enhance the emotional resonance of Khmer classical poems?',
     now() - interval '5 days', now() - interval '5 days'),

    ('1e550000-0000-0000-0000-000000000007', 'English Conditional Sentences (Types 0, 1, 2, 3 & Mixed)',
     'Master all conditional forms for expressing facts, possibilities, hypothetical situations, and past regrets.',
     'EASY', 20, 'c0000000-0000-0000-0000-000000000005',
     'When would you choose a Mixed Conditional over a standard Third Conditional?',
     now() - interval '4 days', now() - interval '4 days'),

    ('1e550000-0000-0000-0000-000000000008', 'Chemical Equilibrium & Le Chatelier''s Principle',
     'Predict shifts in reversible chemical reactions when temperature, pressure, or concentration changes occur.',
     'HARD', 40, 'c0000000-0000-0000-0000-000000000006',
     'How does Le Chatelier''s principle apply to the industrial synthesis of ammonia via the Haber process?',
     now() - interval '3 days', now() - interval '3 days')
ON CONFLICT (id) DO UPDATE
SET title = EXCLUDED.title,
    description = EXCLUDED.description,
    difficulty = EXCLUDED.difficulty,
    base_xp = EXCLUDED.base_xp,
    category_id = EXCLUDED.category_id,
    reflection_prompt = EXCLUDED.reflection_prompt,
    updated_at = now();


-- ── 10. EXERCISES ───────────────────────────────────────────────────────────
INSERT INTO "exercises" (id, lesson_id, question, answer_key, difficulty, points, created_at, updated_at) VALUES
    ('ee000000-0000-0000-0000-000000000001', '1e550000-0000-0000-0000-000000000001',
     'What is the derivative of f(x) = 3x^4 - 5x^2 + 7?', '12x^3 - 10x', 'EASY', 5, now() - interval '10 days', now() - interval '10 days'),
    ('ee000000-0000-0000-0000-000000000002', '1e550000-0000-0000-0000-000000000001',
     'Find the slope of the tangent line to y = x^2 at the point x = 3.', '6', 'EASY', 5, now() - interval '10 days', now() - interval '10 days'),

    ('ee000000-0000-0000-0000-000000000003', '1e550000-0000-0000-0000-000000000002',
     'Calculate the definite integral of 2x dx from x = 0 to x = 4.', '16', 'MEDIUM', 10, now() - interval '9 days', now() - interval '9 days'),
    ('ee000000-0000-0000-0000-000000000004', '1e550000-0000-0000-0000-000000000002',
     'What is the integral of e^(2x) dx from 0 to 1? (Exact value: (e^2 - 1)/2)', '(e^2-1)/2', 'MEDIUM', 10, now() - interval '9 days', now() - interval '9 days'),

    ('ee000000-0000-0000-0000-000000000005', '1e550000-0000-0000-0000-000000000003',
     'Which Flutter widget is immutable and rebuilds when its parent configures new properties?', 'StatelessWidget', 'EASY', 5, now() - interval '8 days', now() - interval '8 days'),
    ('ee000000-0000-0000-0000-000000000006', '1e550000-0000-0000-0000-000000000003',
     'What method in State<T> is triggered when you need the UI to repaint after a value change?', 'setState', 'EASY', 5, now() - interval '8 days', now() - interval '8 days'),

    ('ee000000-0000-0000-0000-000000000007', '1e550000-0000-0000-0000-000000000007',
     'Complete the Second Conditional: "If I _____ (have) more time, I would learn Flutter."', 'had', 'EASY', 5, now() - interval '4 days', now() - interval '4 days'),
    ('ee000000-0000-0000-0000-000000000008', '1e550000-0000-0000-0000-000000000007',
     'Complete the Third Conditional: "If they had studied harder, they _____ (pass) the exam."', 'would have passed', 'EASY', 5, now() - interval '4 days', now() - interval '4 days')
ON CONFLICT (id) DO UPDATE
SET question = EXCLUDED.question,
    answer_key = EXCLUDED.answer_key,
    difficulty = EXCLUDED.difficulty,
    points = EXCLUDED.points,
    updated_at = now();


-- ── 11. LESSON COMPLETIONS (DEMO USER) ──────────────────────────────────────
INSERT INTO "lesson_completions" (id, user_id, lesson_id, xp_earned, completed_at) VALUES
    ('1c000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000011', '1e550000-0000-0000-0000-000000000001', 30, now() - interval '7 days'),
    ('1c000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000011', '1e550000-0000-0000-0000-000000000002', 50, now() - interval '5 days'),
    ('1c000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000011', '1e550000-0000-0000-0000-000000000003', 35, now() - interval '3 days'),
    ('1c000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000011', '1e550000-0000-0000-0000-000000000007', 30, now() - interval '1 day'),
    ('1c000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000001', '1e550000-0000-0000-0000-000000000001', 30, now() - interval '10 days'),
    ('1c000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000001', '1e550000-0000-0000-0000-000000000002', 50, now() - interval '8 days'),
    ('1c000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000001', '1e550000-0000-0000-0000-000000000004', 60, now() - interval '4 days')
ON CONFLICT (user_id, lesson_id) DO NOTHING;


-- ── 12. XP TRANSACTIONS LEDGER (FOR DEMO USER) ──────────────────────────────
INSERT INTO "xp_transactions" (id, user_id, amount, source_type, source_id, running_balance, created_at) VALUES
    ('da000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000011', 100, 'WELCOME_BONUS', NULL, 100, now() - interval '14 days'),
    ('da000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000011',  50, 'ACHIEVEMENT_REWARD', 'bebf28a0-2f40-4657-bef8-148e3a659ceb', 150, now() - interval '13 days'),
    ('da000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000011',  30, 'LESSON_COMPLETION', '1e550000-0000-0000-0000-000000000001', 180, now() - interval '7 days'),
    ('da000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000011',  50, 'MAINTAIN_STREAK', '83fd22e0-1061-4218-afbc-54df35612995', 230, now() - interval '7 days'),
    ('da000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000011',  50, 'LESSON_COMPLETION', '1e550000-0000-0000-0000-000000000002', 280, now() - interval '5 days'),
    ('da000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000011', 150, 'REACH_RANK', '3c7e64af-5831-4408-9b97-c97a6320b2c4', 430, now() - interval '4 days'),
    ('da000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000011',  35, 'LESSON_COMPLETION', '1e550000-0000-0000-0000-000000000003', 465, now() - interval '3 days'),
    ('da000000-0000-0000-0000-000000000008', 'a0000000-0000-0000-0000-000000000011', 400, 'REACH_RANK', 'c6c641aa-1dc1-4c5d-8f01-bb48b55d6eb2', 865, now() - interval '2 days'),
    ('da000000-0000-0000-0000-000000000009', 'a0000000-0000-0000-0000-000000000011', 100, 'MAINTAIN_STREAK', 'e12f3b19-abcb-40c9-9037-793159533207', 965, now() - interval '1 day'),
    ('da000000-0000-0000-0000-000000000010', 'a0000000-0000-0000-0000-000000000011', 555, 'DAILY_PRACTICE_STREAK', NULL, 1520, now() - interval '30 minutes')
ON CONFLICT (id) DO NOTHING;


-- ── 13. USER ACHIEVEMENTS ───────────────────────────────────────────────────
INSERT INTO "user_achievements" (user_id, achievement_id, unlocked_at) VALUES
    -- Demo User achievements
    ('a0000000-0000-0000-0000-000000000011', 'bebf28a0-2f40-4657-bef8-148e3a659ceb', now() - interval '13 days'), -- First Lesson
    ('a0000000-0000-0000-0000-000000000011', 'fb16b4fc-64cd-44d7-8fb6-8de5ef7ec91d', now() - interval '11 days'), -- First Steps (3-day streak)
    ('a0000000-0000-0000-0000-000000000011', '83fd22e0-1061-4218-afbc-54df35612995', now() - interval '7 days'),  -- Week Warrior (7-day streak)
    ('a0000000-0000-0000-0000-000000000011', '2154c7ca-c3b7-4d26-b841-8e9020c2b4e2', now() - interval '6 days'),  -- Silver Tier
    ('a0000000-0000-0000-0000-000000000011', '3c7e64af-5831-4408-9b97-c97a6320b2c4', now() - interval '4 days'),  -- Gold Tier
    ('a0000000-0000-0000-0000-000000000011', 'c6c641aa-1dc1-4c5d-8f01-bb48b55d6eb2', now() - interval '2 days'),  -- Platinum Tier
    ('a0000000-0000-0000-0000-000000000011', 'e12f3b19-abcb-40c9-9037-793159533207', now() - interval '1 day'),   -- Fortnight Fighter (14-day streak)
    ('a0000000-0000-0000-0000-000000000011', 'd77bc77e-7fa2-4c90-ae03-614047ff408d', now() - interval '1 day'),   -- XP Hunter (1000 XP)

    -- Top User Sokha achievements
    ('a0000000-0000-0000-0000-000000000001', 'bebf28a0-2f40-4657-bef8-148e3a659ceb', now() - interval '40 days'),
    ('a0000000-0000-0000-0000-000000000001', '29e24858-c8cc-4517-9b95-c1fbb34850aa', now() - interval '12 days'), -- Monthly Master
    ('a0000000-0000-0000-0000-000000000001', '70ed5869-0560-43fa-a9b5-961bad74e6c4', now() - interval '5 days'),  -- XP Master (5000 XP)
    ('a0000000-0000-0000-0000-000000000001', 'd234cc10-3976-427d-a2bf-3d216538d87c', now() - interval '2 days')   -- Legend Tier
ON CONFLICT (user_id, achievement_id) DO NOTHING;


-- ── 14. RANK HISTORY ────────────────────────────────────────────────────────
INSERT INTO "rank_history" (id, user_id, scope, old_rank, new_rank, changed_at) VALUES
    ('de000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000011', 'COUNTRY', 'BRONZE', 'SILVER', now() - interval '6 days'),
    ('de000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000011', 'COUNTRY', 'SILVER', 'GOLD', now() - interval '4 days'),
    ('de000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000011', 'COUNTRY', 'GOLD', 'PLATINUM', now() - interval '2 days'),
    ('de000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', 'COUNTRY', 'PLATINUM', 'DIAMOND', now() - interval '15 days'),
    ('de000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000001', 'COUNTRY', 'DIAMOND', 'LEGEND', now() - interval '2 days')
ON CONFLICT (id) DO NOTHING;


-- ── 15. LEADERBOARD SNAPSHOTS (POSTGRESQL SOURCE) ───────────────────────────
-- Populate country and province snapshots for CURRENT_DATE
DELETE FROM "leaderboard_snapshots" WHERE snapshot_date = CURRENT_DATE;

-- Insert COUNTRY scope snapshot
INSERT INTO "leaderboard_snapshots" (id, user_id, scope, scope_id, rank, score, ranking_tier, snapshot_date, created_at)
SELECT
    uuid_generate_v4(),
    ug.user_id,
    'COUNTRY',
    'CAMBODIA',
    RANK() OVER (ORDER BY (ug.total_xp + (ug.current_streak * 5)) DESC) AS rank,
    (ug.total_xp + (ug.current_streak * 5)) AS score,
    rt.tier_name,
    CURRENT_DATE,
    now()
FROM user_gamification ug
JOIN LATERAL (
    SELECT tier_name
    FROM rank_tiers
    WHERE min_xp <= ug.total_xp
    ORDER BY min_xp DESC
    LIMIT 1
) rt ON TRUE;

-- Insert PROVINCE scope snapshot
INSERT INTO "leaderboard_snapshots" (id, user_id, scope, scope_id, rank, score, ranking_tier, snapshot_date, created_at)
SELECT
    uuid_generate_v4(),
    u.id AS user_id,
    'PROVINCE',
    u.province_id::text,
    RANK() OVER (PARTITION BY u.province_id ORDER BY (ug.total_xp + (ug.current_streak * 5)) DESC) AS rank,
    (ug.total_xp + (ug.current_streak * 5)) AS score,
    rt.tier_name,
    CURRENT_DATE,
    now()
FROM users u
JOIN user_gamification ug ON ug.user_id = u.id
JOIN LATERAL (
    SELECT tier_name
    FROM rank_tiers
    WHERE min_xp <= ug.total_xp
    ORDER BY min_xp DESC
    LIMIT 1
) rt ON TRUE
WHERE u.province_id IS NOT NULL;
