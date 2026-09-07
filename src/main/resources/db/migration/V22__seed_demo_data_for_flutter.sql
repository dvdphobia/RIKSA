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
ON CONFLICT DO NOTHING;


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
ON CONFLICT DO NOTHING;


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
ON CONFLICT (user_id) DO NOTHING;


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
ON CONFLICT (id) DO NOTHING;


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
ON CONFLICT (id) DO NOTHING;


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
ON CONFLICT (id) DO NOTHING;

-- Blog likes for demo user and others
INSERT INTO "blog_likes" (blog_post_id, user_id, liked_at) VALUES
    ('bb000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000011', now() - interval '2 days'),
    ('bb000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000011', now() - interval '1 day'),
    ('bb000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000011', now() - interval '12 hours')
ON CONFLICT (blog_post_id, user_id) DO NOTHING;


-- ── 9. LESSONS ─────────────────────────────────────────────────────────────
INSERT INTO "lessons" (id, title, description, difficulty, base_xp, category_id, reflection_prompt, created_at, updated_at) VALUES
    -- 1. Math - Derivatives
    ('1e550000-0000-0000-0000-000000000001',
     'ដេរីវេ និងសមីការបន្ទាត់ប៉ះ (Derivatives & Tangent Lines)',
     E'# មូលដ្ឋានគ្រឹះនៃដេរីវេ និងសមីការបន្ទាត់ប៉ះ (Derivatives & Tangent Lines)\n\n**ដេរីវេ (Derivative)** គឺជាឧបករណ៍គណិតវិទ្យាដ៏សំខាន់សម្រាប់វាស់វែង **អត្រាបម្រែបម្រួលភ្លាមៗ (Instantaneous Rate of Change)** នៃអនុគមន៍មួយធៀបនឹងអថេររបស់វា។\n\n---\n\n### ១. និយមន័យដេរីវេ\nសម្រាប់អនុគមន៍ $y = f(x)$ ដេរីវេត្រង់ចំណុច $x = x_0$ កំណត់ដោយ៖\n$$f''(x_0) = \\lim_{\\Delta x \\to 0} \\frac{f(x_0 + \\Delta x) - f(x_0)}{\\Delta x}$$\n\n---\n\n### ២. រូបមន្តគ្រឹះសំខាន់ៗ\n- **ដេរីវេនៃស្វ័យគុណ:** $(x^n)'' = n \\cdot x^{n-1}$\n- **ដេរីវេនៃផលបូក:** $(u + v)'' = u'' + v''$\n- **ដេរីវេនៃផលគុណ:** $(u \\cdot v)'' = u''v + uv''$\n- **ដេរីវេនៃផលចែក:** $\\left(\\frac{u}{v}\\right)'' = \\frac{u''v - uv''}{v^2}$\n- **ដេរីវេនៃចំនួនថេរ:** $(c)'' = 0$\n\n---\n\n### ៣. សមីការបន្ទាត់ប៉ះនឹងក្រាប (Tangent Line Equation)\nសមីការបន្ទាត់ប៉ះនឹងក្រាបតាងអនុគមន៍ $y = f(x)$ ត្រង់ចំណុច $M(x_0, y_0)$ គឺ៖\n$$y - y_0 = f''(x_0)(x - x_0)$$\nដែល $f''(x_0)$ ជា **មេគុណប្រាប់ទិស (Slope)** នៃបន្ទាត់ប៉ះ។',
     'EASY', 25, 'c0000000-0000-0000-0000-000000000001',
     'តើដេរីវេជួយយើងក្នុងការវិភាគអត្រាបម្រែបម្រួលនៃសេដ្ឋកិច្ច និងចលនារូបវិទ្យាយ៉ាងដូចម្តេច?',
     now() - interval '10 days', now()),

    -- 2. Math - Integrals
    ('1e550000-0000-0000-0000-000000000002',
     'អាំងតេក្រាលកំណត់ និងការគណនាផ្ទៃក្រឡា (Definite Integrals & Area Calculation)',
     E'# អាំងតេក្រាលកំណត់ និងការគណនាផ្ទៃក្រឡា (Definite Integrals & Area)\n\n**អាំងតេក្រាល (Integral)** គឺជាប្រតិបត្តិការច្រាសនៃដេរីវេ ដែលប្រើប្រាស់សម្រាប់គណនាផលបូកបន្តបន្ទាប់ ផ្ទៃក្រឡា និងមាឌនៃរូបធរណីមាត្រស្មុគស្មាញ។\n\n---\n\n### ១. ទ្រឹស្តីបទគ្រឹះនៃការគណនាអាំងតេក្រាល (Newton-Leibniz)\nបើ $F(x)$ ជាព្រីមីទីវនៃ $f(x)$ នោះអាំងតេក្រាលកំណត់ពី $a$ ទៅ $b$ គឺ៖\n$$\\int_{a}^{b} f(x)\\,dx = [F(x)]_{a}^{b} = F(b) - F(a)$$\n\n---\n\n### ២. រូបមន្តព្រីមីទីវគ្រឹះ\n- $\\int x^n\\,dx = \\frac{x^{n+1}}{n+1} + C \\quad (n \\neq -1)$\n- $\\int \\frac{1}{x}\\,dx = \\ln|x| + C$\n- $\\int e^x\\,dx = e^x + C$\n- $\\int \\cos(x)\\,dx = \\sin(x) + C$\n- $\\int \\sin(x)\\,dx = -\\cos(x) + C$\n\n---\n\n### ៣. ការគណនាផ្ទៃក្រឡារវាងខ្សែកោង\nផ្ទៃក្រឡា $S$ នៃដែនកំណត់ដោយក្រាប $y = f(x)$, អ័ក្សអាប់ស៊ីស $(Ox)$, និងបន្ទាត់ឈរ $x = a$, $x = b$ កំណត់ដោយ៖\n$$S = \\int_{a}^{b} |f(x)|\\,dx$$',
     'MEDIUM', 35, 'c0000000-0000-0000-0000-000000000001',
     'ហេតុអ្វីបានជាអាំងតេក្រាលជាឧបករណ៍គណនាផ្ទៃក្រឡា និងមាឌដ៏មានឥទ្ធិពលបំផុត?',
     now() - interval '9 days', now()),

    -- 3. STEM / Computer Science - Programming Fundamentals
    ('1e550000-0000-0000-0000-000000000003',
     'មូលដ្ឋានគ្រឹះសរសេរកូដ និងក្បួនដោះស្រាយ (Programming Fundamentals & Algorithms)',
     E'# មូលដ្ឋានគ្រឹះសរសេរកូដ និងក្បួនដោះស្រាយ (Programming & Algorithms)\n\nការសរសេរកូដគឺជាការណែនាំកុំព្យូទ័រឱ្យអនុវត្តការងារជាក់លាក់ណាមួយតាមរយៈភាសាសរសេរកម្មវិធី។\n\n---\n\n### ១. គោលគំនិតគ្រឹះក្នុងការសរសេរកូដ\n- **អថេរ (Variables) & ប្រភេទតម្លៃ (Data Types):** `int`, `double`, `String`, `bool` សម្រាប់រក្សាទុកព័ត៌មាន។\n- **លក្ខខណ្ឌ (Conditionals):** `if / else` សម្រាប់ធ្វើការសម្រេចចិត្តក្នុងកម្មវិធី។\n- **រង្វិលជុំ (Loops):** `for`, `while` សម្រាប់អនុវត្តសកម្មភាពដដែលៗដោយស្វ័យប្រវត្តិ។\n- **អនុគមន៍ (Functions):** ប្លុកកូដដែលមានឈ្មោះច្បាស់លាស់ អាចហៅមកប្រើឡើងវិញបានច្រើនដង។\n\n---\n\n### ២. ក្បួនដោះស្រាយ (Algorithms)\nក្បួនដោះស្រាយគឺជា **លំដាប់លំដោយនៃជំហានច្បាស់លាស់** ក្នុងការដោះស្រាយបញ្ហាណាមួយ។\nឧទាហរណ៍៖ ក្បួនតម្រៀបទិន្នន័យ (Sorting Algorithm) ឬក្បួនស្វែងរក (Search Algorithm)។',
     'EASY', 25, 'c0000000-0000-0000-0000-000000000002',
     'ហេតុអ្វីបានជាការគិតបែបក្បួនដោះស្រាយ (Computational Thinking) មានប្រយោជន៍សម្រាប់ការដោះស្រាយបញ្ហាគ្រប់មុខវិជ្ជា?',
     now() - interval '8 days', now()),

    -- 4. STEM / Computer Science - Computer Architecture & Networks
    ('1e550000-0000-0000-0000-000000000004',
     'ស្ថាបត្យកម្មកុំព្យូទ័រ និងបណ្តាញអ៊ីនធឺណិត (Computer Architecture & Internet Networks)',
     E'# ស្ថាបត្យកម្មប្រព័ន្ធកុំព្យូទ័រ និងបណ្តាញ (Computer Architecture & Networks)\n\nការយល់ដឹងពីផ្នែករឹង (Hardware) និងប្រព័ន្ធបណ្តាញ (Networking) គឺជាគ្រឹះស្នូលនៃវិស័យ STEM និងបច្ចេកវិទ្យាព័ត៌មានវិទ្យា។\n\n---\n\n### ១. ផ្នែករឹងសំខាន់ៗនៃកុំព្យូទ័រ (Computer Hardware)\n- **CPU (Central Processing Unit):** ខួរក្បាលគណនា និងប្រតិបត្តិកូដកម្មវិធី។\n- **RAM (Random Access Memory):** អង្គចងចាំបណ្តោះអាសន្នល្បឿនលឿនសម្រាប់កម្មវិធីដែលកំពុងដំណើរការ។\n- **Storage (SSD / HDD):** អង្គចងចាំអចិន្ត្រៃយ៍សម្រាប់រក្សាទុកទិន្នន័យ និងប្រព័ន្ធប្រតិបត្តិការ (OS)។\n\n---\n\n### ២. របៀបដែលអ៊ីនធឺណិតដំណើរការ (How the Internet Works)\n- **IP Address:** អាសយដ្ឋានឌីជីថលសម្គាល់ឧបករណ៍នីមួយៗនៅលើបណ្តាញ (ឧ. `192.168.1.1` ឬ IPv6)។\n- **DNS (Domain Name System):** សៀវភៅទូរស័ព្ទនៃអ៊ីនធឺណិត ដែលបំប្លែងឈ្មោះគេហទំព័រ (ឧ. `riksa.edu.kh`) ទៅជា IP Address។\n- **HTTP / HTTPS:** ពិធីការទំនាក់ទំនងទិន្នន័យរវាង Browser និង Web Server ប្រកបដោយសុវត្ថិភាព។',
     'MEDIUM', 35, 'c0000000-0000-0000-0000-000000000002',
     'តើទិន្នន័យធ្វើដំណើរឆ្លងកាត់ពិភពលោកតាមរយៈប្រព័ន្ធបណ្តាញអ៊ីនធឺណិតយ៉ាងដូចម្តេច?',
     now() - interval '7 days', now()),

    -- 5. Physics - Newton's Laws
    ('1e550000-0000-0000-0000-000000000005',
     'ច្បាប់ចលនាញូតុន និងកម្លាំង (Newton''s Laws of Motion & Forces)',
     E'# ច្បាប់ចលនាញូតុនទាំង ៣ (Newton''s Laws of Motion)\n\nច្បាប់ចលនារបស់លោក **អ៊ីសាក់ ញូតុន (Isaac Newton)** គឺជាគ្រឹះដ៏សំខាន់នៃរូបវិទ្យាមេកានិចបុរាណ ដែលពន្យល់ពីទំនាក់ទំនងរវាងកម្លាំង និងចលនារបស់អង្គធាតុ។\n\n---\n\n### ១. ច្បាប់ទី១ ញូតុន (ច្បាប់និចលភាព - Law of Inertia)\nអង្គធាតុមួយរក្សាភាពនៅស្ងៀម ឬចលនាត្រង់ស្មើជានិច្ច លុះត្រាតែមានកម្លាំងក្រៅគ្មានតុល្យភាពមកមានអំពើលើវា៖\n$$\\sum \\vec{F} = \\vec{0} \\implies \\vec{v} = \\text{const}$$\n\n---\n\n### ២. ច្បាប់ទី២ ញូតុន (ច្បាប់មូលដ្ឋានឌីណាមិច)\nសំទុះរបស់អង្គធាតុមួយសមាមាត្រត្រង់ទៅនឹងផលបូកកម្លាំងដែលមានអំពើលើវា និងសមាមាត្រច្រាសទៅនឹងម៉ាសរបស់វា៖\n$$\\sum \\vec{F} = m \\cdot \\vec{a}$$\n- $F$ ៖ កម្លាំង (គិតជា ញូតុន - N)\n- $m$ ៖ ម៉ាសអង្គធាតុ (គិតជា គីឡូក្រាម - kg)\n- $a$ ៖ សំទុះ (គិតជា $\\text{m/s}^2$)\n\n---\n\n### ៣. ច្បាប់ទី៣ ញូតុន (ច្បាប់សកម្មភាព និងប្រតិកម្ម)\nគ្រប់កម្លាំងសកម្មភាពតែងតែមានកម្លាំងប្រតិកម្មស្មើទំហំ និងមានទិសដៅផ្ទុយគ្នាជានិច្ច៖\n$$\\vec{F}_{\\text{action}} = -\\vec{F}_{\\text{reaction}}$$',
     'EASY', 25, 'c0000000-0000-0000-0000-000000000004',
     'ចូរលើកឧទាហរណ៍ជាក់ស្តែងមួយក្នុងជីវភាពប្រចាំថ្ងៃដែលបង្ហាញពីច្បាប់ទី ៣ របស់ញូតុន (Action-Reaction)។',
     now() - interval '6 days', now()),

    -- 6. Khmer Literature
    ('1e550000-0000-0000-0000-000000000006',
     'ក្បួនចួន និងរចនាសម្ព័ន្ធកំណាព្យខ្មែរ (Khmer Classical Poetry & Rhyme Meter)',
     E'# ក្បួនចួន និងរចនាសម្ព័ន្ធកំណាព្យខ្មែរ (Khmer Classical Poetry)\n\nកំណាព្យខ្មែរជាសម្បត្តិវប្បធម៌ដ៏វិចិត្រ ដែលមានក្បួនខ្នាត ចំនួនព្យាង្គ និងក្បួនចួនយ៉ាងម៉ត់ចត់។\n\n---\n\n### ១. បទពាក្យ ៧ (Pathyak 7)\n- ក្នុងមួយល្បះមាន ៤ ឃ្លា ហើយក្នុងមួយឃ្លាមាន ៧ ព្យាង្គ។\n- **ក្បួនចួន:**\n  - ព្យាង្គទី៧ នៃឃ្លាទី១ ចួននឹង ព្យាង្គទី៤ នៃឃ្លាទី២\n  - ព្យាង្គទី៧ នៃឃ្លាទី២ ចួននឹង ព្យាង្គទី៧ នៃឃ្លាទី៣ និងព្យាង្គទី៤ នៃឃ្លាទី៤\n  - ព្យាង្គទី៧ នៃឃ្លាទី៤ ជាពាក្យចង្អុលជើងមេឃ សម្រាប់ចួនឆ្លងល្បះ។\n\n---\n\n### ២. បទកាកគតិ និងបទព្រហ្មគីតិ\n- **បទកាកគតិ:** ក្នុងមួយល្បះមាន ៧ ឃ្លា មួយឃ្លាមាន ៤ ព្យាង្គ សរុប ២៨ ព្យាង្គ។\n- **បទព្រហ្មគីតិ:** ឃ្លាទី១ និងទី៣ មាន ៥ ព្យាង្គ, ឃ្លាទី២ និងទី៤ មាន ៦ ព្យាង្គ។',
     'EASY', 20, 'c0000000-0000-0000-0000-000000000003',
     'តើក្បួនចួនក្នុងកំណាព្យជួយបង្កើនសោភ័ណភាព និងសម្រស់អក្សរសាស្ត្រខ្មែរយ៉ាងដូចម្តេច?',
     now() - interval '5 days', now()),

    -- 7. English Learning
    ('1e550000-0000-0000-0000-000000000007',
     'ប្រយោគលក្ខខណ្ឌភាសាអង់គ្លេស (English Conditional Sentences: Types 0, 1, 2, 3)',
     E'# ប្រយោគលក្ខខណ្ឌក្នុងភាសាអង់គ្លេស (English Conditional Sentences)\n\nប្រយោគលក្ខខណ្ឌ (Conditionals) ត្រូវបានប្រើដើម្បីបង្ហាញពីសកម្មភាពមួយដែលអាស្រ័យលើលក្ខខណ្ឌមួយផ្សេងទៀត។\n\n---\n\n### ១. Zero Conditional (ការពិតទូទៅ ឬច្បាប់វិទ្យាសាស្ត្រ)\n- **ទម្រង់:** `If + Present Simple, ... Present Simple`\n- **ឧទាហរណ៍:** *If you heat ice, it melts.*\n\n---\n\n### ២. First Conditional (លទ្ធភាពពិតប្រាកដនៅពេលអនាគត)\n- **ទម្រង់:** `If + Present Simple, ... will + V_inf`\n- **ឧទាហរណ៍:** *If it rains tomorrow, we will stay at home.*\n\n---\n\n### ៣. Second Conditional (ស្ថានភាពសន្មត ឬមិនពិតនៅពេលបច្ចុប្បន្ន)\n- **ទម្រង់:** `If + Past Simple, ... would + V_inf`\n- **ឧទាហរណ៍:** *If I had more time, I would learn flutter.*\n\n---\n\n### ៤. Third Conditional (ការសោកស្តាយចំពោះរឿងអតីតកាលដែលកន្លងផុត)\n- **ទម្រង់:** `If + Past Perfect, ... would have + V_3 (Past Participle)`\n- **ឧទាហរណ៍:** *If they had studied harder, they would have passed the exam.*',
     'EASY', 20, 'c0000000-0000-0000-0000-000000000005',
     'តើនៅពេលណាដែលយើងគួរប្រើប្រាស់ Second Conditional ធៀបនឹង Third Conditional?',
     now() - interval '4 days', now()),

    -- 8. Chemistry - Periodic Table & Bonding
    ('1e550000-0000-0000-0000-000000000008',
     'តារាងខួបនៃធាតុគីមី និងសម្ព័ន្ធគីមី (Periodic Table & Chemical Bonding)',
     E'# តារាងខួបនៃធាតុគីមី និងសម្ព័ន្ធគីមី (Periodic Table & Chemical Bonding)\n\nការស្វែងយល់អំពីរចនាសម្ព័ន្ធអាតូម និងតារាងខួបជួយឱ្យយើងយល់ដឹងពីលក្ខណៈគីមី និងរបៀបដែលធាតុផ្សេងៗចងសម្ព័ន្ធគ្នាបង្កើតជាម៉ូលេគុលថ្មីៗ។\n\n---\n\n### ១. រចនាសម្ព័ន្ធតារាងខួប\n- **ខួប (Periods):** ជួរដេកទាំង ៧ ដែលបង្ហាញពីចំនួនស្រទាប់អេឡិចត្រុង (Electron shells)។\n- **ក្រុម (Groups):** ជួរឈរទាំង ១៨ ដែលធាតុក្នុងក្រុមតែមួយមានចំនួនអេឡិចត្រុងស្រទាប់ក្រៅបង្អស់ (Valence electrons) ស្មើគ្នា និងមានលក្ខណៈគីមីស្រដៀងគ្នា។\n\n---\n\n### ២. ប្រភេទសម្ព័ន្ធគីមីសំខាន់ៗ\n1. **សម្ព័ន្ធកូវ៉ាឡង់ (Covalent Bond):** កើតឡើងរវាងអលោហៈ និងអលោហៈ តាមរយៈការ **ចែករំលែកអេឡិចត្រុងរួមគ្នា** (ឧ. $\\text{H}_2\\text{O}, \\text{CO}_2, \\text{CH}_4$)។\n2. **សម្ព័ន្ធអ៊ីយ៉ុង (Ionic Bond):** កើតឡើងរវាងលោហៈ និងអលោហៈ តាមរយៈការ **បោះបង់ និងទទួលអេឡិចត្រុង** បង្កើតបានជាកាចុង (+) និងអានីយ៉ុង (-) ទាក់ទាញគ្នាដោយកម្លាំងអេឡិចត្រូស្តាទិច (ឧ. $\\text{NaCl}, \\text{MgO}$)។\n3. **សម្ព័ន្ធអ៊ីដ្រូសែន (Hydrogen Bond):** កម្លាំងទាក់ទាញខ្សោយរវាងម៉ូលេគុលប៉ូលែរ។',
     'EASY', 25, 'c0000000-0000-0000-0000-000000000006',
     'តើកម្លាំងទាក់ទាញរវាងអាតូមជួយបង្កើតសារធាតុចម្រុះក្នុងពិភពលោកយើងយ៉ាងដូចម្តេច?',
     now() - interval '3 days', now()),

    -- 9. Physics - Electricity & Circuits
    ('1e550000-0000-0000-0000-000000000011',
     'អគ្គិសនី ច្បាប់អូម និងសៀគ្វីអគ្គិសនី (Electricity, Ohm''s Law & Circuits)',
     E'# មូលដ្ឋានគ្រឹះអគ្គិសនី ច្បាប់អូម និងសៀគ្វី (Electricity & Ohm''s Law)\n\nអគ្គិសនីគឺជាទម្រង់ថាមពលដ៏សំខាន់ក្នុងជីវភាពរស់នៅ និងបច្ចេកវិទ្យាទំនើប។\n\n---\n\n### ១. បរិមាណអគ្គិសនីគ្រឹះ\n- **អាំងតង់ស៊ីតេចរន្ត ($I$):** បរិមាណបន្ទុកអគ្គិសនីឆ្លងកាត់មុខកាត់ខ្សែចម្លងក្នុងមួយឯកតាពេល (គិតជា អំពែរ - A)។\n- **តង់ស្យុង ឬផលសងប៉ូតង់ស្យែល ($U$ ឬ $V$):** កម្លាំងរុញច្រានបន្ទុកអគ្គិសនី (គិតជា វ៉ុល - V)។\n- **រេស៊ីស្តង់ ($R$):** កម្រិតរារាំងចរន្តអគ្គិសនីនៃសារធាតុចម្លង (គិតជា អូម - $\\Omega$)។\n\n---\n\n### ២. ច្បាប់អូម (Ohm''s Law)\nអាំងតង់ស៊ីតេចរន្តឆ្លងកាត់រេស៊ីស្ត័រមួយសមាមាត្រត្រង់ទៅនឹងតង់ស្យុង និងសមាមាត្រច្រាសនឹងរេស៊ីស្តង់៖\n$$U = I \\cdot R \\quad \\iff \\quad I = \\frac{U}{R}$$\n\n---\n\n### ៣. អានុភាព និងថាមពលអគ្គិសនី\n- **អានុភាពអគ្គិសនី ($P$):** $P = U \\cdot I = R \\cdot I^2$ (គិតជា វ៉ាត់ - W)\n- **ថាមពលអគ្គិសនី ($E$):** $E = P \\cdot t$ (គិតជា ហ្ស៊ូល - J ឬ គីឡូវ៉ាត់ម៉ោង - kWh)',
     'MEDIUM', 30, 'c0000000-0000-0000-0000-000000000004',
     'តើការយល់ដឹងពីសៀគ្វីខ្នែង និងស៊េរីមានសារៈសំខាន់យ៉ាងណាក្នុងការតបណ្តាញអគ្គិសនីតាមផ្ទះប្រកបដោយសុវត្ថិភាព?',
     now() - interval '2 days', now()),

    -- 10. Chemistry - Equilibrium & Kinetics
    ('1e550000-0000-0000-0000-000000000012',
     'សមតុល្យគីមី និងល្បឿនប្រតិកម្ម (Chemical Equilibrium & Reaction Kinetics)',
     E'# សមតុល្យគីមី និងល្បឿនប្រតិកម្ម (Chemical Equilibrium & Kinetics)\n\nក្នុងប្រតិកម្មគីមីត្រឡប់បាន សមតុល្យកើតឡើងនៅពេលដែលល្បឿនប្រតិកម្មទៅមុខស្មើនឹងល្បឿនប្រតិកម្មត្រឡប់មកវិញ។\n\n---\n\n### ១. សមតុល្យឌីណាមិច (Dynamic Equilibrium)\nសម្រាប់ប្រតិកម្មទូទៅ៖\n$$aA + bB \\rightleftharpoons cC + dD$$\nថេរលំនឹងគីមី $K_c$ កំណត់ដោយ៖\n$$K_c = \\frac{[C]^c [D]^d}{[A]^a [B]^b}$$\n\n---\n\n### ២. គោលការណ៍ឡឺសាតឺលីយេ (Le Chatelier''s Principle)\nនៅពេលប្រព័ន្ធមួយក្នុងលំនឹងរងឥទ្ធិពលពីកត្តាខាងក្រៅ (កំហាប់ សីតុណ្ហភាព សម្ពាធ) ប្រព័ន្ធនឹងរំកិលលំនឹងក្នុងទិសដៅមួយដើម្បី **កាត់បន្ថយឥទ្ធិពលនោះវិញ**៖\n- **ការបន្ថែមសារធាតុប្រតិករ:** លំនឹងរំកិលទៅខាងស្តាំ (បង្កើតអង្គធាតុកកើតបន្ថែម)។\n- **ការបង្កើនសម្ពាធ:** លំនឹងរំកិលទៅទិសខាងណាដែលមានចំនួនម៉ូលឧស្ម័នតិចជាង។\n- **ការបង្កើនសីតុណ្ហភាព:** ជំរុញប្រតិកម្មស្រូបកម្តៅ (Endothermic)។',
     'HARD', 40, 'c0000000-0000-0000-0000-000000000006',
     'តើគោលការណ៍សមតុល្យគីមីត្រូវបានអនុវត្តយ៉ាងដូចម្តេចក្នុងការផលិតជីគីមី និងឧស្សាហកម្មជាក់ស្តែង?',
     now() - interval '2 days', now()),

    -- 11. Biology - Cell Biology
    ('1e550000-0000-0000-0000-000000000013',
     'កោសិកា និងរចនាសម្ព័ន្ធជីវសាស្រ្ត (Cell Biology & Cell Structure)',
     E'# កោសិកា និងរចនាសម្ព័ន្ធជីវសាស្រ្ត (Cell Biology & Organelles)\n\n**កោសិកា (Cell)** គឺជាឯកតាគ្រឹះនៃទម្រង់ និងមុខងារនៃភាវៈរស់ទាំងអស់នៅលើផែនដី។\n\n---\n\n### ១. សមាសភាគសំខាន់ៗនៃកោសិកា\n- **ភ្នាសកោសិកា (Cell Membrane):** គ្រប់គ្រងការចេញចូលនៃសារធាតុចិញ្ចឹម និងកាកសំណល់។\n- **ស៊ីតូប្លាស (Cytoplasm):** វត្ថុរាវក្នុងកោសិកាដែលជាកន្លែងប្រតិកម្មជីវគីមីប្រព្រឹត្តទៅ។\n- **ណ្វៃយ៉ូ (Nucleus):** មជ្ឈមណ្ឌលបញ្ជាដែលមានផ្ទុកក្រូម៉ូសូម និង DNA តំណពូជ។\n- **មីតូកុងឌ្រី (Mitochondria):** រោងចក្រផលិតថាមពល (ATP) សម្រាប់កោសិកាតាមរយៈការដកដង្ហើមកោសិកា។\n- **រីបូសូម (Ribosomes):** រោងចក្រសំយោគប្រូតេអ៊ីន។\n\n---\n\n### ២. ភាពខុសគ្នារវាងកោសិការុក្ខជាតិ និងសត្វ\n| លក្ខណៈ | កោសិការុក្ខជាតិ | កោសិកាសត្វ |\n| :--- | :--- | :--- |\n| **ជញ្ជាំងកោសិកា (Cell Wall)** | មាន (រឹងមាំ) | គ្មាន |\n| **ក្លរ៉ូប្លាស (Chloroplast)** | មាន (ធ្វើរស្មីសំយោគ) | គ្មាន |\n| **វ៉ាគុយអូល (Vacuole)** | ធំតែមួយនៅកណ្តាល | តូចៗជាច្រើន |',
     'EASY', 25, 'c0000000-0000-0000-0000-000000000007',
     'តើកោសិកាដើរតួនាទីយ៉ាងសំខាន់ដូចម្តេចក្នុងការទ្រទ្រង់ដំណើរការជីវិតរបស់ភាវៈរស់?',
     now() - interval '1 days', now()),

    -- 12. Biology - Genetics & DNA
    ('1e550000-0000-0000-0000-000000000014',
     'ពន្ធុវិទ្យា និងក្រមពន្ធុ DNA (Genetics & DNA Structure)',
     E'# ពន្ធុវិទ្យា និងក្រមពន្ធុ DNA (Genetics & DNA Replication)\n\nពន្ធុវិទ្យាគឺជាការសិក្សាអំពីការចម្លងលក្ខណៈ និងបំរែបំរួលតំណពូជពីជំនាន់មួយទៅជំនាន់មួយទៀត។\n\n---\n\n### ១. រចនាសម្ព័ន្ធម៉ូលេគុល DNA\nDNA (Deoxyribonucleic Acid) មានរាងជាជណ្តើរ xo ពីរជាន់ (Double Helix) ដែលបង្កឡើងពី **នុយក្លេអូទីត (Nucleotides)**៖\n- **ស្ករដេអុកស៊ីរីបូស + ក្រុមផូស្វាត**\n- **បាសអាសូតចំនួន ៤ ប្រភេទ៖**\n  - $A$ (Adenine) ភ្ជាប់ជាមួយ $T$ (Thymine) ដោយសម្ព័ន្ធអ៊ីដ្រូសែន ២\n  - $G$ (Guanine) ភ្ជាប់ជាមួយ $C$ (Cytosine) ដោយសម្ព័ន្ធអ៊ីដ្រូសែន ៣\n\n---\n\n### ២. ច្បាប់តំណពូជរបស់លោក ម៉ង់ដែល (Mendel''s Laws)\n- **ច្បាប់ឯកសណ្ឋាននៃកូនកាត់ជំនាន់ទី១ ($F_1$):** អាឡែលលេច (Dominant) លុបលើអាឡែលអន់ (Recessive)។\n- **ច្បាប់បំណែងចែកអាឡែល:** ក្នុងដំណើរការកកើតហ្គាម៉ែត អាឡែលនីមួយៗបំបែកចេញពីគ្នា។',
     'MEDIUM', 30, 'c0000000-0000-0000-0000-000000000007',
     'ហេតុអ្វីបានជាការយល់ដឹងអំពីរចនាសម្ព័ន្ធ DNA មានសារៈសំខាន់សម្រាប់ការស្រាវជ្រាវវេជ្ជសាស្ត្រ និងកសិកម្ម?',
     now() - interval '1 days', now())
ON CONFLICT (id) DO NOTHING;



-- ── 10. EXERCISES ───────────────────────────────────────────────────────────
INSERT INTO "exercises" (id, lesson_id, question, answer_key, difficulty, points, created_at, updated_at) VALUES
    -- Exercises for Lesson 1 (Math - Derivatives)
    ('ee000000-0000-0000-0000-000000000001', '1e550000-0000-0000-0000-000000000001',
     'គណនាដេរីវេនៃអនុគមន៍ f(x) = 3x^4 - 5x^2 + 7',
     '12x^3 - 10x', 'EASY', 5, now(), now()),
    ('ee000000-0000-0000-0000-000000000002', '1e550000-0000-0000-0000-000000000001',
     'រកមេគុណប្រាប់ទិសនៃបន្ទាត់ប៉ះនឹងក្រាប y = x^2 ត្រង់ចំណុច x = 3',
     '6', 'EASY', 5, now(), now()),
    ('ee000000-0000-0000-0000-000000000021', '1e550000-0000-0000-0000-000000000001',
     'តើដេរីវេនៃចំនួនថេរ c (Constant) ស្មើនឹងប៉ុន្មាន?',
     '0', 'EASY', 5, now(), now()),

    -- Exercises for Lesson 2 (Math - Integrals)
    ('ee000000-0000-0000-0000-000000000003', '1e550000-0000-0000-0000-000000000002',
     'គណនាអាំងតេក្រាលកំណត់ ∫ ពី 0 ទៅ 4 នៃ 2x dx',
     '16', 'MEDIUM', 8, now(), now()),
    ('ee000000-0000-0000-0000-000000000004', '1e550000-0000-0000-0000-000000000002',
     'គណនាអាំងតេក្រាលកំណត់ ∫ ពី 0 ទៅ 2 នៃ 3x^2 dx',
     '8', 'MEDIUM', 8, now(), now()),
    ('ee000000-0000-0000-0000-000000000022', '1e550000-0000-0000-0000-000000000002',
     'តើព្រីមីទីវនៃអនុគមន៍ f(x) = cos(x) គឺជាអ្វី?',
     'sin(x) + C', 'EASY', 5, now(), now()),

    -- Exercises for Lesson 3 (STEM - Programming)
    ('ee000000-0000-0000-0000-000000000005', '1e550000-0000-0000-0000-000000000003',
     'នៅក្នុងការសរសេរកូដ តើរចនាសម្ព័ន្ធណាដែលប្រើដើម្បីអនុវត្តកូដដដែលៗរហូតដល់លក្ខខណ្ឌឈប់?',
     'Loops (រង្វិលជុំ ដូចជា for ឬ while)', 'EASY', 5, now(), now()),
    ('ee000000-0000-0000-0000-000000000006', '1e550000-0000-0000-0000-000000000003',
     'តើអ្វីជាសំណុំនៃជំហានច្បាស់លាស់សម្រាប់ដោះស្រាយបញ្ហា ឬដំណើរការកិច្ចការមួយក្នុងកុំព្យូទ័រ?',
     'Algorithm (ក្បួនដោះស្រាយ)', 'EASY', 5, now(), now()),
    ('ee000000-0000-0000-0000-000000000023', '1e550000-0000-0000-0000-000000000003',
     'តើប្រភេទតម្លៃ (Data Type) ណាដែលប្រើសម្រាប់រក្សាទុកតម្លៃពិត ឬមិនពិត (True / False)?',
     'Boolean (bool)', 'EASY', 5, now(), now()),

    -- Exercises for Lesson 4 (STEM - Systems & Networks)
    ('ee000000-0000-0000-0000-000000000024', '1e550000-0000-0000-0000-000000000004',
     'តើផ្នែករឹងកុំព្យូទ័រណាដែលត្រូវបានគេប្រៀបធៀបទៅនឹងខួរក្បាលរបស់កុំព្យូទ័រក្នុងការគណនា?',
     'CPU (Central Processing Unit)', 'EASY', 5, now(), now()),
    ('ee000000-0000-0000-0000-000000000025', '1e550000-0000-0000-0000-000000000004',
     'តើប្រព័ន្ធដែលបំប្លែងឈ្មោះគេហទំព័រ (Domain Name) ទៅជាលេខ IP Address ហៅថាអ្វី?',
     'DNS (Domain Name System)', 'MEDIUM', 8, now(), now()),
    ('ee000000-0000-0000-0000-000000000026', '1e550000-0000-0000-0000-000000000004',
     'តើអង្គចងចាំប្រភេទណាដែលបាត់បង់ទិន្នន័យនៅពេលបិទភ្លើង ឬបិទកុំព្យូទ័រ?',
     'RAM (Random Access Memory)', 'EASY', 5, now(), now()),

    -- Exercises for Lesson 5 (Physics - Newton's Laws)
    ('ee000000-0000-0000-0000-000000000027', '1e550000-0000-0000-0000-000000000005',
     'សរសេរបង្ហាញពីរូបមន្តច្បាប់ទី២ របស់ញូតុនដែលទាក់ទងរវាងកម្លាំង (F), ម៉ាស (m) និងសំទុះ (a)',
     'F = m * a', 'EASY', 5, now(), now()),
    ('ee000000-0000-0000-0000-000000000028', '1e550000-0000-0000-0000-000000000005',
     'បើកម្លាំង 20N មានអំពើលើវត្ថុដែលមានម៉ាស 4kg តើសំទុះរបស់វត្ថុនោះស្មើនឹងប៉ុន្មាន m/s^2?',
     '5 m/s^2', 'EASY', 5, now(), now()),
    ('ee000000-0000-0000-0000-000000000029', '1e550000-0000-0000-0000-000000000005',
     'តើច្បាប់ទី១ របស់ញូតុនមានឈ្មោះហៅក្រៅថាជាច្បាប់អ្វី?',
     'ច្បាប់និចលភាព (Law of Inertia)', 'EASY', 5, now(), now()),

    -- Exercises for Lesson 6 (Khmer Literature)
    ('ee000000-0000-0000-0000-000000000030', '1e550000-0000-0000-0000-000000000006',
     'នៅក្នុងបទពាក្យ៧ តើព្យាង្គទី៧ នៃឃ្លាទី១ ត្រូវចួននឹងព្យាង្គទីប៉ុន្មាននៃឃ្លាទី២?',
     'ព្យាង្គទី៤', 'EASY', 5, now(), now()),
    ('ee000000-0000-0000-0000-000000000031', '1e550000-0000-0000-0000-000000000006',
     'តើក្នុងមួយល្បះនៃបទកាកគតិ មានចំនួនសរុបប៉ុន្មានព្យាង្គ?',
     '២៨ ព្យាង្គ (៧ ឃ្លា ក្នុងមួយឃ្លាមាន ៤ ព្យាង្គ)', 'EASY', 5, now(), now()),

    -- Exercises for Lesson 7 (English)
    ('ee000000-0000-0000-0000-000000000007', '1e550000-0000-0000-0000-000000000007',
     'បំពេញចន្លោះ Second Conditional: "If I _____ (have) more time, I would learn Flutter."',
     'had', 'EASY', 5, now(), now()),
    ('ee000000-0000-0000-0000-000000000008', '1e550000-0000-0000-0000-000000000007',
     'បំពេញចន្លោះ Third Conditional: "If they had studied harder, they _____ (pass) the exam."',
     'would have passed', 'EASY', 5, now(), now()),

    -- Exercises for Lesson 8 (Chemistry - Periodic Table & Bonding)
    ('ee000000-0000-0000-0000-000000000032', '1e550000-0000-0000-0000-000000000008',
     'តើសម្ព័ន្ធប្រភេទណាដែលកើតឡើងពីការចែករំលែកអេឡិចត្រុងរួមគ្នារវាងអាតូមពីរ?',
     'សម្ព័ន្ធកូវ៉ាឡង់ (Covalent Bond)', 'EASY', 5, now(), now()),
    ('ee000000-0000-0000-0000-000000000033', '1e550000-0000-0000-0000-000000000008',
     'តើធាតុដែលមានលេខអាតូម Z=1 ក្នុងតារាងខួបជាធាតុអ្វី?',
     'អ៊ីដ្រូសែន (Hydrogen - H)', 'EASY', 5, now(), now()),
    ('ee000000-0000-0000-0000-000000000034', '1e550000-0000-0000-0000-000000000008',
     'តើ NaCl (អំបិលសម្ល) កើតឡើងពីសម្ព័ន្ធគីមីប្រភេទណា?',
     'សម្ព័ន្ធអ៊ីយ៉ុង (Ionic Bond)', 'EASY', 5, now(), now()),

    -- Exercises for Lesson 9 (Physics - Electricity)
    ('ee000000-0000-0000-0000-000000000035', '1e550000-0000-0000-0000-000000000011',
     'សរសេររូបមន្តច្បាប់អូម (Ohm''s Law) ដែលបង្ហាញពីទំនាក់ទំនងរវាងតង់ស្យុង (U), ចរន្ត (I) និងរេស៊ីស្តង់ (R)',
     'U = I * R', 'EASY', 5, now(), now()),
    ('ee000000-0000-0000-0000-000000000036', '1e550000-0000-0000-0000-000000000011',
     'បើតង់ស្យុង U = 12V និងរេស៊ីស្តង់ R = 4 Ohm តើអាំងតង់ស៊ីតេចរន្ត I ស្មើនឹងប៉ុន្មាន Ampere?',
     '3 A', 'EASY', 5, now(), now()),
    ('ee000000-0000-0000-0000-000000000037', '1e550000-0000-0000-0000-000000000011',
     'តើឯកតាអន្តរជាតិនៃអានុភាពអគ្គិសនី (Electrical Power) គឺជាអ្វី?',
     'វ៉ាត់ (Watt - W)', 'EASY', 5, now(), now()),

    -- Exercises for Lesson 10 (Chemistry - Equilibrium & Kinetics)
    ('ee000000-0000-0000-0000-000000000038', '1e550000-0000-0000-0000-000000000012',
     'តើការបន្ថែមសារធាតុកាតាលីករ (Catalyst) ជួយអ្វីខ្លះដល់ប្រតិកម្មគីមី?',
     'បង្កើនល្បឿនប្រតិកម្មដោយបន្ថយថាមពលសកម្មកម្ម (Activation Energy)', 'MEDIUM', 8, now(), now()),
    ('ee000000-0000-0000-0000-000000000039', '1e550000-0000-0000-0000-000000000012',
     'យោងតាមគោលការណ៍ឡឺសាតឺលីយេ បើបង្កើនកំហាប់អង្គធាតុកកើត តើសមតុល្យនឹងរំកិលទៅទិសណា?',
     'រំកិលទៅទិសខាងឆ្វេង (បង្កើតអង្គធាតុប្រតិករ)', 'HARD', 10, now(), now()),

    -- Exercises for Lesson 11 (Biology - Cell Biology)
    ('ee000000-0000-0000-0000-000000000040', '1e550000-0000-0000-0000-000000000013',
     'តើអង្គធាតុណាខ្លះជាមជ្ឈមណ្ឌលផលិតថាមពល (Powerhouse) របស់កោសិកា?',
     'មីតូកុងឌ្រី (Mitochondria)', 'EASY', 5, now(), now()),
    ('ee000000-0000-0000-0000-000000000041', '1e550000-0000-0000-0000-000000000013',
     'ចូរប្រាប់លក្ខណៈពិសេសមួយដែលកោសិការុក្ខជាតិមាន ប៉ុន្តែកោសិកាសត្វគ្មាន',
     'ជញ្ជាំងកោសិកា (Cell Wall) និង ក្លរ៉ូប្លាស (Chloroplast)', 'EASY', 5, now(), now()),
    ('ee000000-0000-0000-0000-000000000042', '1e550000-0000-0000-0000-000000000013',
     'តើ organelles ណាដែលមាននាទីស្តុកទុកព័ត៌មានពន្ធុវិទ្យា (DNA)?',
     'ណ្វៃយ៉ូ (Nucleus)', 'EASY', 5, now(), now()),

    -- Exercises for Lesson 12 (Biology - Genetics & DNA)
    ('ee000000-0000-0000-0000-000000000043', '1e550000-0000-0000-0000-000000000014',
     'នៅក្នុងម៉ូលេគុល DNA តើបាសអាសូត A (Adenine) តែងតែភ្ជាប់ជាមួយបាសអាសូតមួយណា?',
     'បាស T (Thymine)', 'EASY', 5, now(), now()),
    ('ee000000-0000-0000-0000-000000000044', '1e550000-0000-0000-0000-000000000014',
     'តើអ្នកវិទ្យាសាស្ត្រណាដែលត្រូវបានចាត់ទុកថាជាបិតានៃពន្ធុវិទ្យាទំនើប?',
     'Gregor Mendel (ហ្គ្រេហ្គ័រ ម៉ង់ដែល)', 'EASY', 5, now(), now()),
    ('ee000000-0000-0000-0000-000000000045', '1e550000-0000-0000-0000-000000000014',
     'តើម៉ូលេគុល DNA មានរចនាសម្ព័ន្ធទម្រង់ជារូបរាងអ្វី?',
     'រាងជណ្តើរ xo ពីរជាន់ (Double Helix)', 'EASY', 5, now(), now())
ON CONFLICT (id) DO NOTHING;


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
    ('da000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000011',  50, 'ACHIEVEMENT_REWARD', (SELECT id FROM "achievements" WHERE name = 'First Lesson'), 150, now() - interval '13 days'),
    ('da000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000011',  30, 'LESSON_COMPLETION', '1e550000-0000-0000-0000-000000000001', 180, now() - interval '7 days'),
    ('da000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000011',  50, 'MAINTAIN_STREAK', (SELECT id FROM "achievements" WHERE name = 'Week Warrior'), 230, now() - interval '7 days'),
    ('da000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000011',  50, 'LESSON_COMPLETION', '1e550000-0000-0000-0000-000000000002', 280, now() - interval '5 days'),
    ('da000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000011', 150, 'REACH_RANK', (SELECT id FROM "achievements" WHERE name = 'Gold Tier'), 430, now() - interval '4 days'),
    ('da000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000011',  35, 'LESSON_COMPLETION', '1e550000-0000-0000-0000-000000000003', 465, now() - interval '3 days'),
    ('da000000-0000-0000-0000-000000000008', 'a0000000-0000-0000-0000-000000000011', 400, 'REACH_RANK', (SELECT id FROM "achievements" WHERE name = 'Platinum Tier'), 865, now() - interval '2 days'),
    ('da000000-0000-0000-0000-000000000009', 'a0000000-0000-0000-0000-000000000011', 100, 'MAINTAIN_STREAK', (SELECT id FROM "achievements" WHERE name = 'Fortnight Fighter'), 965, now() - interval '1 day'),
    ('da000000-0000-0000-0000-000000000010', 'a0000000-0000-0000-0000-000000000011', 555, 'DAILY_PRACTICE_STREAK', NULL, 1520, now() - interval '30 minutes')
ON CONFLICT (id) DO NOTHING;


-- ── 13. USER ACHIEVEMENTS ───────────────────────────────────────────────────
WITH earned_achievements (user_id, achievement_name, unlocked_at) AS (VALUES
    -- Demo User achievements
    ('a0000000-0000-0000-0000-000000000011', 'First Lesson', now() - interval '13 days'),
    ('a0000000-0000-0000-0000-000000000011', 'First Steps', now() - interval '11 days'),
    ('a0000000-0000-0000-0000-000000000011', 'Week Warrior', now() - interval '7 days'),
    ('a0000000-0000-0000-0000-000000000011', 'Silver Tier', now() - interval '6 days'),
    ('a0000000-0000-0000-0000-000000000011', 'Gold Tier', now() - interval '4 days'),
    ('a0000000-0000-0000-0000-000000000011', 'Platinum Tier', now() - interval '2 days'),
    ('a0000000-0000-0000-0000-000000000011', 'Fortnight Fighter', now() - interval '1 day'),
    ('a0000000-0000-0000-0000-000000000011', 'XP Hunter', now() - interval '1 day'),

    -- Top User Sokha achievements
    ('a0000000-0000-0000-0000-000000000001', 'First Lesson', now() - interval '40 days'),
    ('a0000000-0000-0000-0000-000000000001', 'Monthly Master', now() - interval '12 days'),
    ('a0000000-0000-0000-0000-000000000001', 'XP Master', now() - interval '5 days'),
    ('a0000000-0000-0000-0000-000000000001', 'Legend Tier', now() - interval '2 days')
)
INSERT INTO "user_achievements" (user_id, achievement_id, unlocked_at)
SELECT earned.user_id::uuid, achievement.id, earned.unlocked_at
FROM earned_achievements earned
JOIN "achievements" achievement ON achievement.name = earned.achievement_name
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
) rt ON TRUE
ON CONFLICT (user_id, scope, scope_id, snapshot_date) DO NOTHING;

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
WHERE u.province_id IS NOT NULL
ON CONFLICT (user_id, scope, scope_id, snapshot_date) DO NOTHING;
