-- Computer Skills starter roadmap: a bilingual category, one unit, five short
-- lessons, and three multiple-choice exercises per lesson.
--
-- Endpoint smoke check after applying V38 (use an authenticated learner token):
-- GET /api/v1/lessons/roadmap?categoryId=c3800000-0000-0000-0000-000000000001
-- Expect the Computer Skills category, one Digital basics unit, and 5 lessons.
-- For each unit lesson id, GET /api/v1/lessons/{id}; expect a bilingual lesson
-- description and 3 MULTIPLE_CHOICE exercises.

INSERT INTO "categories" (id, name, slug, description, icon_url, official)
VALUES (
    'c3800000-0000-0000-0000-000000000001',
    'Computer Skills / ជំនាញកុំព្យូទ័រ',
    'computer-skills',
    'Practical digital skills for study and everyday life. / ជំនាញឌីជីថលសម្រាប់ការសិក្សា និងជីវិតប្រចាំថ្ងៃ។',
    'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=150',
    TRUE
)
ON CONFLICT (slug) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    icon_url = EXCLUDED.icon_url,
    official = TRUE,
    updated_at = now();

INSERT INTO "roadmap_units" (id, category_id, title, description, position)
VALUES (
    '38000000-0000-0000-0000-000000000001',
    (SELECT id FROM "categories" WHERE slug = 'computer-skills'),
    'Digital basics / មូលដ្ឋានឌីជីថល',
    'Build useful habits for using a computer and the web. / បង្កើតទម្លាប់ល្អក្នុងការប្រើកុំព្យូទ័រ និងអ៊ីនធឺណិត។',
    1
)
ON CONFLICT (id) DO UPDATE SET
    category_id = EXCLUDED.category_id,
    title = EXCLUDED.title,
    description = EXCLUDED.description,
    position = EXCLUDED.position,
    updated_at = now();

INSERT INTO "lessons" (
    id, title, description, difficulty, base_xp, category_id,
    roadmap_unit_id, position, estimated_minutes, reflection_prompt
) VALUES
(
    '38100000-0000-0000-0000-000000000001',
    'Keyboard shortcuts that save time / ផ្លូវកាត់ក្តារចុចដែលជួយសន្សំពេលវេលា',
    $$## 1. Edit with confidence / កែសម្រួលដោយទំនុកចិត្ត
Use Ctrl+C to copy, Ctrl+V to paste, and Ctrl+Z to undo your last change. On a Mac, use Command instead of Ctrl.
ប្រើ Ctrl+C ដើម្បីចម្លង, Ctrl+V ដើម្បីបិទភ្ជាប់ និង Ctrl+Z ដើម្បីត្រឡប់ការកែប្រែចុងក្រោយវិញ។ នៅលើ Mac ប្រើ Command ជំនួស Ctrl។

## 2. Save and find your work / រក្សាទុក និងស្វែងរកការងារ
Use Ctrl+S to save often. Ctrl+F finds a word on the current page or document. Shortcuts can differ between apps.
ប្រើ Ctrl+S ដើម្បីរក្សាទុកជាញឹកញាប់។ Ctrl+F ស្វែងរកពាក្យក្នុងទំព័រ ឬឯកសារបច្ចុប្បន្ន។ ផ្លូវកាត់អាចខុសគ្នាតាមកម្មវិធី។$$,
    'EASY', 15, (SELECT id FROM "categories" WHERE slug = 'computer-skills'),
    '38000000-0000-0000-0000-000000000001', 1, 4,
    'Which shortcut will you use most? / តើអ្នកនឹងប្រើផ្លូវកាត់មួយណាច្រើនជាងគេ?'
),
(
    '38100000-0000-0000-0000-000000000002',
    'Files and folders: organise your study files / ឯកសារ និងថត៖ រៀបចំឯកសារសិក្សារបស់អ្នក',
    $$## 1. Make a simple folder system / បង្កើតប្រព័ន្ធថតសាមញ្ញ
Create a main school folder, then a folder for each subject. Keep related notes, slides, and assignments together so you can find them later.
បង្កើតថតមេសម្រាប់ការសិក្សា រួចបង្កើតថតសម្រាប់មុខវិជ្ជានីមួយៗ។ ដាក់កំណត់ត្រា ស្លាយ និងកិច្ចការដែលពាក់ព័ន្ធនៅជាមួយគ្នា។

## 2. Name files clearly / ដាក់ឈ្មោះឯកសារឱ្យច្បាស់
Use names that include the subject and date, such as Biology_Notes_2026-09-24. Save a new version before a big change.
ដាក់ឈ្មោះឯកសារដោយបញ្ចូលមុខវិជ្ជា និងកាលបរិច្ឆេទ ដូចជា Biology_Notes_2026-09-24។ រក្សាទុកច្បាប់ចម្លងថ្មីមុនពេលកែប្រែធំៗ។$$,
    'EASY', 15, (SELECT id FROM "categories" WHERE slug = 'computer-skills'),
    '38000000-0000-0000-0000-000000000001', 2, 4,
    'What folder will help organise your study? / តើអ្នកនឹងបង្កើតថតអ្វីដើម្បីរៀបចំការសិក្សា?'
),
(
    '38100000-0000-0000-0000-000000000003',
    'Search the web well and spot fake information / ស្វែងរកព័ត៌មានតាមអ៊ីនធឺណិតឱ្យបានល្អ និងសម្គាល់ព័ត៌មានក្លែងក្លាយ',
    $$## 1. Search with a clear question / ស្វែងរកដោយប្រើសំណួរច្បាស់លាស់
Use a few specific words instead of a broad search. Add a date, place, or subject to narrow the results.
ប្រើពាក្យជាក់លាក់មួយចំនួន ជំនួសឱ្យការស្វែងរកទូលំទូលាយ។ បន្ថែមកាលបរិច្ឆេទ ទីកន្លែង ឬមុខវិជ្ជា ដើម្បីឱ្យលទ្ធផលកាន់តែច្បាស់។

## 2. Check before you share / ពិនិត្យមុនពេលចែករំលែក
Look for the source, author, date, and evidence. Compare important claims with more than one reliable source. A dramatic headline or many shares does not prove a story is true.
រកមើលប្រភព អ្នកនិពន្ធ កាលបរិច្ឆេទ និងភស្តុតាង។ ប្រៀបធៀបការអះអាងសំខាន់ៗជាមួយប្រភពដែលគួរឱ្យទុកចិត្តច្រើនជាងមួយ។ ចំណងជើងគួរឱ្យភ្ញាក់ផ្អើល ឬការចែករំលែកច្រើន មិនបញ្ជាក់ថាព័ត៌មាននោះពិតទេ។$$,
    'EASY', 15, (SELECT id FROM "categories" WHERE slug = 'computer-skills'),
    '38000000-0000-0000-0000-000000000001', 3, 5,
    'What will you check before sharing a claim? / តើអ្នកនឹងពិនិត្យអ្វីមុនពេលចែករំលែកការអះអាងមួយ?'
),
(
    '38100000-0000-0000-0000-000000000004',
    'Stay safe online: passwords, 2FA, and scams / សុវត្ថិភាពលើអ៊ីនធឺណិត៖ ពាក្យសម្ងាត់ ការផ្ទៀងផ្ទាត់ពីរជំហាន និងការបោកប្រាស់',
    $$## 1. Protect your accounts / ការពារគណនីរបស់អ្នក
Use a long, unique password for each account. Turn on two-factor authentication (2FA) when available; it asks for a second proof after your password.
ប្រើពាក្យសម្ងាត់វែង និងខុសគ្នាសម្រាប់គណនីនីមួយៗ។ បើអាច សូមបើកការផ្ទៀងផ្ទាត់ពីរជំហាន (2FA) ដែលស្នើសុំការបញ្ជាក់ទីពីរបន្ទាប់ពីពាក្យសម្ងាត់។

## 2. Pause when a message pressures you / ផ្អាកសិន នៅពេលសារដាក់សម្ពាធលើអ្នក
Telegram or Facebook scams may pretend to be a friend, ask for a login code, or rush you to click a link. Never share a one-time code; verify requests through a separate trusted channel.
ការបោកប្រាស់នៅ Telegram ឬ Facebook អាចធ្វើដូចជាមិត្តភក្តិ សុំលេខកូដចូលគណនី ឬបង្ខំឱ្យអ្នកចុចតំណភ្ជាប់ភ្លាមៗ។ កុំចែករំលែកលេខកូដប្រើតែម្តង ហើយពិនិត្យសំណើតាមមធ្យោបាយដែលអ្នកទុកចិត្តផ្សេងទៀត។$$,
    'EASY', 15, (SELECT id FROM "categories" WHERE slug = 'computer-skills'),
    '38000000-0000-0000-0000-000000000001', 4, 5,
    'What is one warning sign of a scam? / តើអ្វីជាសញ្ញាព្រមានមួយនៃការបោកប្រាស់?'
),
(
    '38100000-0000-0000-0000-000000000005',
    'Make a simple slide presentation / បង្កើតបទបង្ហាញស្លាយសាមញ្ញ',
    $$## 1. Give each slide one job / កំណត់គោលបំណងមួយឱ្យស្លាយនីមួយៗ
Start with a title slide, then put your main ideas in a clear order. Use one main idea per slide and short phrases instead of full paragraphs.
ចាប់ផ្តើមដោយស្លាយចំណងជើង រួចរៀបចំគំនិតសំខាន់ៗតាមលំដាប់ច្បាស់លាស់។ ដាក់គំនិតសំខាន់មួយក្នុងមួយស្លាយ ហើយប្រើឃ្លាខ្លីៗជំនួសកថាខណ្ឌវែង។

## 2. Make it easy to read / ធ្វើឱ្យងាយអាន
Choose large text and strong contrast. Use helpful images, finish with a short summary, and practise once before presenting.
ជ្រើសរើសអក្សរធំល្មម និងពណ៌ដែលខុសគ្នាច្បាស់។ ប្រើរូបភាពដែលជួយបញ្ជាក់គំនិត បញ្ចប់ដោយសេចក្តីសង្ខេបខ្លី ហើយហាត់បង្ហាញម្តងមុនពេលធ្វើបទបង្ហាញ។$$,
    'EASY', 15, (SELECT id FROM "categories" WHERE slug = 'computer-skills'),
    '38000000-0000-0000-0000-000000000001', 5, 5,
    'What change would make your next slide easier to read? / តើអ្នកនឹងកែអ្វីដើម្បីឱ្យស្លាយបន្ទាប់ងាយអានជាងមុន?'
)
ON CONFLICT (id) DO UPDATE SET
    title = EXCLUDED.title,
    description = EXCLUDED.description,
    difficulty = EXCLUDED.difficulty,
    base_xp = EXCLUDED.base_xp,
    category_id = EXCLUDED.category_id,
    roadmap_unit_id = EXCLUDED.roadmap_unit_id,
    position = EXCLUDED.position,
    estimated_minutes = EXCLUDED.estimated_minutes,
    reflection_prompt = EXCLUDED.reflection_prompt,
    updated_at = now();

INSERT INTO "exercises" (
    id, lesson_id, question, answer_key, difficulty, points,
    section_index, type, options, hint, explanation
) VALUES
('38200000-0000-0000-0000-000000000001', '38100000-0000-0000-0000-000000000001', 'Which shortcut saves the current file? / តើផ្លូវកាត់មួយណារក្សាទុកឯកសារបច្ចុប្បន្ន?', 'Ctrl+S (រក្សាទុក)', 'EASY', 5, 1, 'MULTIPLE_CHOICE', '["Ctrl+S (រក្សាទុក)","Ctrl+P (បោះពុម្ព)","Ctrl+N (ឯកសារថ្មី)","Ctrl+Z (ត្រឡប់វិញ)"]', 'Save often to keep recent work. / រក្សាទុកជាញឹកញាប់ ដើម្បីកុំឱ្យបាត់ការងារថ្មីៗ។', 'Ctrl+S saves the current file. / Ctrl+S រក្សាទុកឯកសារបច្ចុប្បន្ន។'),
('38200000-0000-0000-0000-000000000002', '38100000-0000-0000-0000-000000000001', 'Which shortcut finds a word in a document? / តើផ្លូវកាត់មួយណាស្វែងរកពាក្យក្នុងឯកសារ?', 'Ctrl+F (ស្វែងរក)', 'EASY', 5, 2, 'MULTIPLE_CHOICE', '["Ctrl+F (ស្វែងរក)","Ctrl+V (បិទភ្ជាប់)","Ctrl+S (រក្សាទុក)","Ctrl+Z (ត្រឡប់វិញ)"]', 'Find searches the current page or document. / Find ស្វែងរកក្នុងទំព័រ ឬឯកសារបច្ចុប្បន្ន។', 'Ctrl+F opens search in many apps. / Ctrl+F បើកមុខងារស្វែងរកក្នុងកម្មវិធីជាច្រើន។'),
('38200000-0000-0000-0000-000000000003', '38100000-0000-0000-0000-000000000001', 'What does Ctrl+Z usually do? / ជាទូទៅ តើ Ctrl+Z ធ្វើអ្វី?', 'Undo the last change (ត្រឡប់ការកែប្រែចុងក្រោយវិញ)', 'EASY', 5, 2, 'MULTIPLE_CHOICE', '["Undo the last change (ត្រឡប់ការកែប្រែចុងក្រោយវិញ)","Save the file (រក្សាទុកឯកសារ)","Print the page (បោះពុម្ពទំព័រ)","Open a new file (បើកឯកសារថ្មី)"]', 'It reverses your most recent edit. / វាត្រឡប់ការកែសម្រួលចុងក្រោយវិញ។', 'Ctrl+Z usually undoes your last change. / ជាទូទៅ Ctrl+Z ត្រឡប់ការកែប្រែចុងក្រោយវិញ។'),
('38200000-0000-0000-0000-000000000004', '38100000-0000-0000-0000-000000000002', 'What is a useful way to organise school files? / តើវិធីណាមានប្រយោជន៍សម្រាប់រៀបចំឯកសារសាលា?', 'Make a folder for each subject (បង្កើតថតសម្រាប់មុខវិជ្ជានីមួយៗ)', 'EASY', 5, 1, 'MULTIPLE_CHOICE', '["Make a folder for each subject (បង្កើតថតសម្រាប់មុខវិជ្ជានីមួយៗ)","Put every file on the desktop (ដាក់ឯកសារទាំងអស់លើ Desktop)","Use the same name for every file (ប្រើឈ្មោះដូចគ្នាសម្រាប់ឯកសារទាំងអស់)","Delete files after each class (លុបឯកសារបន្ទាប់ពីរៀនម្តងៗ)"]', 'Group related work together. / ដាក់ការងារដែលពាក់ព័ន្ធនៅជាមួយគ្នា។', 'A subject folder keeps related work together. / ថតតាមមុខវិជ្ជារក្សាការងារដែលពាក់ព័ន្ធនៅជាមួយគ្នា។'),
('38200000-0000-0000-0000-000000000005', '38100000-0000-0000-0000-000000000002', 'Which filename is easiest to recognise later? / តើឈ្មោះឯកសារមួយណាងាយសម្គាល់នៅពេលក្រោយ?', 'Biology_Notes_2026-09-24', 'EASY', 5, 2, 'MULTIPLE_CHOICE', '["Biology_Notes_2026-09-24","final_final_new","document1","stuff"]', 'Include a subject and date. / បញ្ចូលមុខវិជ្ជា និងកាលបរិច្ឆេទ។', 'A subject and date make the file easier to identify. / ឈ្មោះមុខវិជ្ជា និងកាលបរិច្ឆេទធ្វើឱ្យងាយសម្គាល់ឯកសារ។'),
('38200000-0000-0000-0000-000000000006', '38100000-0000-0000-0000-000000000002', 'What should you do before a big file change? / តើអ្នកគួរធ្វើអ្វីមុនពេលកែប្រែឯកសារធំៗ?', 'Save a new version (រក្សាទុកច្បាប់ចម្លងថ្មី)', 'EASY', 5, 2, 'MULTIPLE_CHOICE', '["Save a new version (រក្សាទុកច្បាប់ចម្លងថ្មី)","Rename every folder (ប្តូរឈ្មោះថតទាំងអស់)","Move it to the recycle bin (ផ្លាស់ទីទៅធុងសំរាម)","Remove its file extension (លុបផ្នែកបន្ថែមឯកសារ)"]', 'Keep a copy of the current file first. / រក្សាច្បាប់ចម្លងឯកសារបច្ចុប្បន្នជាមុនសិន។', 'A new version gives you a copy to return to. / ច្បាប់ចម្លងថ្មីអាចឱ្យអ្នកត្រឡប់ទៅប្រើវិញបាន។'),
('38200000-0000-0000-0000-000000000007', '38100000-0000-0000-0000-000000000003', 'What makes a web search more specific? / តើអ្វីធ្វើឱ្យការស្វែងរកតាមអ៊ីនធឺណិតជាក់លាក់ជាងមុន?', 'Add a subject, place, or date (បន្ថែមមុខវិជ្ជា ទីកន្លែង ឬកាលបរិច្ឆេទ)', 'EASY', 5, 1, 'MULTIPLE_CHOICE', '["Add a subject, place, or date (បន្ថែមមុខវិជ្ជា ទីកន្លែង ឬកាលបរិច្ឆេទ)","Use one broad word (ប្រើពាក្យទូលំទូលាយមួយ)","Search without a question (ស្វែងរកដោយគ្មានសំណួរ)","Choose the first result automatically (ជ្រើសលទ្ធផលដំបូងភ្លាមៗ)"]', 'Add details that narrow the results. / បន្ថែមព័ត៌មានលម្អិតដើម្បីកាត់បន្ថយលទ្ធផល។', 'Specific terms help focus results. / ពាក្យជាក់លាក់ជួយឱ្យលទ្ធផលផ្តោតជាងមុន។'),
('38200000-0000-0000-0000-000000000008', '38100000-0000-0000-0000-000000000003', 'What should you check before trusting a claim? / តើអ្នកគួរពិនិត្យអ្វីមុនពេលជឿការអះអាងមួយ?', 'The source, date, and evidence (ប្រភព កាលបរិច្ឆេទ និងភស្តុតាង)', 'EASY', 5, 2, 'MULTIPLE_CHOICE', '["The source, date, and evidence (ប្រភព កាលបរិច្ឆេទ និងភស្តុតាង)","Only the number of shares (តែចំនួនការចែករំលែក)","Only the headline (តែចំណងជើង)","The webpage colour (ពណ៌គេហទំព័រ)"]', 'A headline or many shares do not prove a claim. / ចំណងជើង ឬការចែករំលែកច្រើនមិនបញ្ជាក់ការអះអាងទេ។', 'Check the source, date, and supporting evidence. / ពិនិត្យប្រភព កាលបរិច្ឆេទ និងភស្តុតាងគាំទ្រ។'),
('38200000-0000-0000-0000-000000000009', '38100000-0000-0000-0000-000000000003', 'How can you check an important online claim? / តើអ្នកអាចពិនិត្យការអះអាងសំខាន់តាមអ៊ីនធឺណិតដោយរបៀបណា?', 'Compare it with other reliable sources (ប្រៀបធៀបជាមួយប្រភពគួរឱ្យទុកចិត្តផ្សេងទៀត)', 'EASY', 5, 2, 'MULTIPLE_CHOICE', '["Compare it with other reliable sources (ប្រៀបធៀបជាមួយប្រភពគួរឱ្យទុកចិត្តផ្សេងទៀត)","Share it before checking (ចែករំលែកមុនពេលពិនិត្យ)","Trust a dramatic story (ជឿព័ត៌មានគួរឱ្យភ្ញាក់ផ្អើល)","Ignore its date (មិនអើពើកាលបរិច្ឆេទ)"]', 'Look for independent confirmation. / រកមើលការបញ្ជាក់ពីប្រភពឯករាជ្យ។', 'Reliable independent sources can help confirm a claim. / ប្រភពឯករាជ្យដែលគួរឱ្យទុកចិត្តអាចជួយបញ្ជាក់ការអះអាង។'),
('38200000-0000-0000-0000-000000000010', '38100000-0000-0000-0000-000000000004', 'What is a good password habit? / តើទម្លាប់ប្រើពាក្យសម្ងាត់មួយណាល្អ?', 'Use a long, unique password for each account (ប្រើពាក្យសម្ងាត់វែង និងខុសគ្នាសម្រាប់គណនីនីមួយៗ)', 'EASY', 5, 1, 'MULTIPLE_CHOICE', '["Use a long, unique password for each account (ប្រើពាក្យសម្ងាត់វែង និងខុសគ្នាសម្រាប់គណនីនីមួយៗ)","Reuse one short password (ប្រើពាក្យសម្ងាត់ខ្លីតែមួយឡើងវិញ)","Send your password to a friend (ផ្ញើពាក្យសម្ងាត់ឱ្យមិត្តភក្តិ)","Post it publicly (បង្ហោះវាជាសាធារណៈ)"]', 'Avoid reusing the same password. / ជៀសវាងការប្រើពាក្យសម្ងាត់ដដែលឡើងវិញ។', 'A unique password limits the impact if another account is exposed. / ពាក្យសម្ងាត់ខុសគ្នាជួយកាត់បន្ថយផលប៉ះពាល់ ប្រសិនបើគណនីមួយផ្សេងទៀតត្រូវបានលួចចូល។'),
('38200000-0000-0000-0000-000000000011', '38100000-0000-0000-0000-000000000004', 'What does turning on 2FA add? / តើការបើក 2FA បន្ថែមអ្វី?', 'A second proof of identity (ភស្តុតាងបញ្ជាក់អត្តសញ្ញាណទីពីរ)', 'EASY', 5, 2, 'MULTIPLE_CHOICE', '["A second proof of identity (ភស្តុតាងបញ្ជាក់អត្តសញ្ញាណទីពីរ)","A public profile (ប្រវត្តិរូបសាធារណៈ)","A shorter password (ពាក្យសម្ងាត់ខ្លីជាងមុន)","Automatic messages (សារស្វ័យប្រវត្តិ)"]', 'It asks for another check after your password. / វាស្នើសុំការត្រួតពិនិត្យមួយទៀតបន្ទាប់ពីពាក្យសម្ងាត់។', '2FA requires a second proof at sign-in. / 2FA ត្រូវការភស្តុតាងទីពីរនៅពេលចូលគណនី។'),
('38200000-0000-0000-0000-000000000012', '38100000-0000-0000-0000-000000000004', 'A Telegram message asks for your one-time login code. What should you do? / សារនៅ Telegram សុំលេខកូដចូលគណនីប្រើតែម្តង។ តើអ្នកគួរធ្វើអ្វី?', 'Do not share it; verify another way (កុំចែករំលែកវា ហើយពិនិត្យតាមមធ្យោបាយផ្សេង)', 'EASY', 5, 2, 'MULTIPLE_CHOICE', '["Do not share it; verify another way (កុំចែករំលែកវា ហើយពិនិត្យតាមមធ្យោបាយផ្សេង)","Send it quickly (ផ្ញើវាភ្លាមៗ)","Post it in a group (បង្ហោះវាក្នុងក្រុម)","Click every link (ចុចតំណភ្ជាប់ទាំងអស់)"]', 'Scammers may pretend to be someone you know. / អ្នកបោកប្រាស់អាចធ្វើដូចជាមនុស្សដែលអ្នកស្គាល់។', 'Never share a one-time code; verify the request through a trusted channel. / កុំចែករំលែកលេខកូដប្រើតែម្តង ហើយពិនិត្យសំណើតាមមធ្យោបាយដែលអ្នកទុកចិត្ត។'),
('38200000-0000-0000-0000-000000000013', '38100000-0000-0000-0000-000000000005', 'What is a clear way to organise presentation content? / តើវិធីណាច្បាស់លាស់សម្រាប់រៀបចំខ្លឹមសារបទបង្ហាញ?', 'Put one main idea on each slide (ដាក់គំនិតសំខាន់មួយលើស្លាយនីមួយៗ)', 'EASY', 5, 1, 'MULTIPLE_CHOICE', '["Put one main idea on each slide (ដាក់គំនិតសំខាន់មួយលើស្លាយនីមួយៗ)","Put a full essay on one slide (ដាក់អត្ថបទវែងទាំងមូលលើស្លាយមួយ)","Use no title (មិនដាក់ចំណងជើង)","Mix unrelated ideas (លាយគំនិតដែលមិនពាក់ព័ន្ធ)"]', 'Short phrases are easier to follow than long paragraphs. / ឃ្លាខ្លីៗងាយតាមដានជាងកថាខណ្ឌវែង។', 'One idea per slide keeps the presentation clear. / គំនិតមួយក្នុងមួយស្លាយជួយឱ្យបទបង្ហាញច្បាស់។'),
('38200000-0000-0000-0000-000000000014', '38100000-0000-0000-0000-000000000005', 'What helps an audience read your slides? / តើអ្វីជួយឱ្យអ្នកស្តាប់អានស្លាយរបស់អ្នកបាន?', 'Large text with strong contrast (អក្សរធំល្មម និងពណ៌ខុសគ្នាច្បាស់)', 'EASY', 5, 2, 'MULTIPLE_CHOICE', '["Large text with strong contrast (អក្សរធំល្មម និងពណ៌ខុសគ្នាច្បាស់)","Tiny text and pale colours (អក្សរតូច និងពណ៌ស្រាល)","Long paragraphs on each slide (កថាខណ្ឌវែងលើស្លាយនីមួយៗ)","Many decorative fonts (ពុម្ពអក្សរតុបតែងច្រើន)"]', 'Make text easy to see from a distance. / ធ្វើឱ្យអក្សរងាយមើលពីចម្ងាយ។', 'Large text and strong contrast improve readability. / អក្សរធំល្មម និងពណ៌ខុសគ្នាច្បាស់ជួយឱ្យងាយអាន។'),
('38200000-0000-0000-0000-000000000015', '38100000-0000-0000-0000-000000000005', 'What is a useful final step before presenting? / តើជំហានណាមានប្រយោជន៍មុនពេលធ្វើបទបង្ហាញ?', 'Practise once and check the slides (ហាត់ម្តង និងពិនិត្យស្លាយ)', 'EASY', 5, 2, 'MULTIPLE_CHOICE', '["Practise once and check the slides (ហាត់ម្តង និងពិនិត្យស្លាយ)","Add every detail at the last minute (បន្ថែមព័ត៌មានទាំងអស់នៅនាទីចុងក្រោយ)","Skip the summary (រំលងសេចក្តីសង្ខេប)","Use the smallest text possible (ប្រើអក្សរតូចបំផុត)"]', 'Review the flow and readability. / ពិនិត្យលំដាប់ និងភាពងាយអាន។', 'Practising helps you notice unclear slides before presenting. / ការហាត់ជួយឱ្យអ្នករកឃើញស្លាយមិនច្បាស់មុនធ្វើបទបង្ហាញ។')
ON CONFLICT (id) DO UPDATE SET
    lesson_id = EXCLUDED.lesson_id,
    question = EXCLUDED.question,
    answer_key = EXCLUDED.answer_key,
    difficulty = EXCLUDED.difficulty,
    points = EXCLUDED.points,
    section_index = EXCLUDED.section_index,
    type = EXCLUDED.type,
    options = EXCLUDED.options,
    hint = EXCLUDED.hint,
    explanation = EXCLUDED.explanation,
    updated_at = now();
