-- Roadmap units that the lesson markdown files refer to but no migration
-- created. Without them an uploaded lesson had no unit and never reached the
-- app's roadmap. Each unit follows the subject's existing ones; ids match the
-- `roadmapUnitId` in the files so they upload as they are.
INSERT INTO "roadmap_units" (id, category_id, title, description, position)
SELECT v.id::uuid, v.category_id::uuid, v.title, v.description,
       COALESCE((SELECT max(u.position) FROM "roadmap_units" u
                 WHERE u.category_id = v.category_id::uuid), 0) + v.ord
FROM (VALUES
    ('2a490000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000004', 1, 'Heat & Thermodynamics', 'Temperature, heat transfer and the behaviour of gases'),
    ('2a490000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000004', 2, 'Waves & Light', 'How waves travel, interfere and bend'),
    ('2a490000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000004', 3, 'Physics Challenge', 'Bring every unit together in one challenge'),
    ('2a490000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000007', 1, 'Ecology & Ecosystems', 'How energy flows and matter cycles through living systems'),
    ('2a490000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000007', 2, 'Human Body Systems', 'How organ systems keep the body alive and in balance'),
    ('2a490000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000007', 3, 'Biology Challenge', 'Bring every unit together in one challenge'),
    ('2a490000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000006', 1, 'Chemical Reactions & Stoichiometry', 'Balancing reactions and counting particles with the mole'),
    ('2a490000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000006', 2, 'Acids & Bases', 'pH, neutralization and everyday acids and bases'),
    ('2a490000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000006', 3, 'Organic Chemistry', 'Hydrocarbons and the chemistry of carbon'),
    ('2a490000-0000-0000-0000-00000000000a', 'c0000000-0000-0000-0000-000000000006', 4, 'Chemistry Challenge', 'Bring every unit together in one challenge'),
    ('2a490000-0000-0000-0000-00000000000b', 'c0000000-0000-0000-0000-000000000002', 1, 'Algorithms', 'Searching and sorting data efficiently'),
    ('2a490000-0000-0000-0000-00000000000c', 'c0000000-0000-0000-0000-000000000002', 2, 'Software & the Web', 'Version control and how the web works'),
    ('2a490000-0000-0000-0000-00000000000d', 'c0000000-0000-0000-0000-000000000002', 3, 'Computer Science Challenge', 'Bring every unit together in one challenge'),
    ('2a490000-0000-0000-0000-00000000000e', 'c3800000-0000-0000-0000-000000000001', 1, 'Documents / ការរៀបចំឯកសារ', 'Layout, headings, tables, images and PDF export'),
    ('2a490000-0000-0000-0000-00000000000f', 'c3800000-0000-0000-0000-000000000001', 2, 'Spreadsheets / តារាងទិន្នន័យ', 'Cells, formulas, sorting, filtering and charts'),
    ('2a490000-0000-0000-0000-000000000010', 'c3800000-0000-0000-0000-000000000001', 3, 'Email & Online Safety / អ៊ីមែល និងសុវត្ថិភាពអនឡាញ', 'Professional email, passwords and phishing'),
    ('2a490000-0000-0000-0000-000000000011', 'c0000000-0000-0000-0000-000000000005', 1, 'Grammar Foundations', 'Parts of speech and the core tenses'),
    ('2a490000-0000-0000-0000-000000000012', 'c0000000-0000-0000-0000-000000000005', 2, 'Everyday Communication', 'Routines, time expressions and polite requests'),
    ('2a490000-0000-0000-0000-000000000013', 'c0000000-0000-0000-0000-000000000005', 3, 'Writing Skills', 'Paragraphs, topic sentences and transitions'),
    ('2a490000-0000-0000-0000-000000000014', 'c0000000-0000-0000-0000-000000000005', 4, 'English Challenge', 'Bring every unit together in one challenge'),
    ('2a490000-0000-0000-0000-000000000015', 'c0000000-0000-0000-0000-000000000003', 1, 'វេយ្យាករណ៍ខ្មែរ', 'ព្យញ្ជនៈ ស្រៈ និងថ្នាក់ពាក្យ'),
    ('2a490000-0000-0000-0000-000000000016', 'c0000000-0000-0000-0000-000000000003', 2, 'ការវិភាគស្នាដៃអក្សរសិល្ប៍', 'ប្រលោមលោក និងរឿងបុរាណខ្មែរ'),
    ('2a490000-0000-0000-0000-000000000017', 'c0000000-0000-0000-0000-000000000003', 3, 'ការតែងសេចក្តី និងកាព្យ', 'សេចក្តីពន្យល់ ប្រៀបធៀប និងក្បួនកាព្យ'),
    ('2a490000-0000-0000-0000-000000000018', 'c0000000-0000-0000-0000-000000000003', 4, 'វិញ្ញាសាសាកល្បង', 'ត្រៀមប្រឡងអក្សរសាស្ត្រខ្មែរ')
) AS v(id, category_id, ord, title, description)
WHERE EXISTS (SELECT 1 FROM "categories" c WHERE c.id = v.category_id::uuid)
  AND NOT EXISTS (SELECT 1 FROM "roadmap_units" u WHERE u.id = v.id::uuid);

-- Lessons already uploaded without their unit go where their file says.
UPDATE "lessons" l
SET roadmap_unit_id = v.unit_id::uuid,
    estimated_minutes = COALESCE(v.minutes, l.estimated_minutes)
FROM (VALUES
    ('c0000000-0000-0000-0000-000000000007', '2a200000-0000-0000-0000-000000000007', 14, 'Enzymes & Biochemical Reactions'),
    ('c0000000-0000-0000-0000-000000000007', '2a200000-0000-0000-0000-000000000008', 18, 'Transcription, Translation & Protein Synthesis'),
    ('c0000000-0000-0000-0000-000000000007', '2a490000-0000-0000-0000-000000000004', 12, 'Food Webs, Energy Trophic Levels & Biomass'),
    ('c0000000-0000-0000-0000-000000000007', '2a490000-0000-0000-0000-000000000004', 15, 'Biogeochemical Cycles & Biodiversity'),
    ('c0000000-0000-0000-0000-000000000007', '2a490000-0000-0000-0000-000000000005', 16, 'The Circulatory & Respiratory Systems'),
    ('c0000000-0000-0000-0000-000000000007', '2a490000-0000-0000-0000-000000000005', 18, 'The Nervous System, Reflexes & Homeostasis'),
    ('c0000000-0000-0000-0000-000000000007', '2a490000-0000-0000-0000-000000000006', 25, 'Biology Roadmap Capstone Challenge'),
    ('c0000000-0000-0000-0000-000000000006', '2a200000-0000-0000-0000-000000000005', 12, 'Atomic Models & Subatomic Particles'),
    ('c0000000-0000-0000-0000-000000000006', '2a200000-0000-0000-0000-000000000005', 15, 'Chemical Bonding: Ionic vs. Covalent'),
    ('c0000000-0000-0000-0000-000000000006', '2a490000-0000-0000-0000-000000000007', 14, 'Balancing Chemical Equations & Reaction Types'),
    ('c0000000-0000-0000-0000-000000000006', '2a490000-0000-0000-0000-000000000007', 18, 'The Mole Concept, Molar Mass & Stoichiometry'),
    ('c0000000-0000-0000-0000-000000000006', '2a200000-0000-0000-0000-000000000006', 16, 'Reaction Rates & Le Chatelier''s Principle'),
    ('c0000000-0000-0000-0000-000000000006', '2a490000-0000-0000-0000-000000000008', 15, 'Acids, Bases, pH & Neutralization'),
    ('c0000000-0000-0000-0000-000000000006', '2a490000-0000-0000-0000-000000000009', 15, 'Hydrocarbons: Alkanes, Alkenes & Alkynes'),
    ('c0000000-0000-0000-0000-000000000006', '2a490000-0000-0000-0000-00000000000a', 25, 'Chemistry Roadmap Capstone Challenge'),
    ('c0000000-0000-0000-0000-000000000002', '2a200000-0000-0000-0000-000000000001', 12, 'Loops & Iteration in Python'),
    ('c0000000-0000-0000-0000-000000000002', '2a200000-0000-0000-0000-000000000001', 14, 'Functions, Scope & Modularity'),
    ('c0000000-0000-0000-0000-000000000002', '2a200000-0000-0000-0000-000000000001', 15, 'Data Structures: Lists & Dictionaries'),
    ('c0000000-0000-0000-0000-000000000002', '2a490000-0000-0000-0000-00000000000b', 16, 'Search Algorithms: Linear vs. Binary Search'),
    ('c0000000-0000-0000-0000-000000000002', '2a490000-0000-0000-0000-00000000000b', 18, 'Sorting Algorithms: Bubble & Insertion Sort'),
    ('c0000000-0000-0000-0000-000000000002', '2a200000-0000-0000-0000-000000000002', 12, 'How Computers Compute: CPU, RAM & Storage'),
    ('c0000000-0000-0000-0000-000000000002', '2a200000-0000-0000-0000-000000000002', 15, 'Computer Networks, IP & the Internet'),
    ('c0000000-0000-0000-0000-000000000002', '2a490000-0000-0000-0000-00000000000c', 14, 'Version Control Fundamentals with Git'),
    ('c0000000-0000-0000-0000-000000000002', '2a490000-0000-0000-0000-00000000000c', 15, 'Web Architecture: HTTP, Clients & APIs'),
    ('c0000000-0000-0000-0000-000000000002', '2a490000-0000-0000-0000-00000000000d', 25, 'Computer Science Roadmap Capstone Challenge'),
    ('c3800000-0000-0000-0000-000000000001', '2a490000-0000-0000-0000-00000000000e', 12, 'Document Layout, Typography & Heading Styles'),
    ('c3800000-0000-0000-0000-000000000001', '2a490000-0000-0000-0000-00000000000e', 12, 'Working with Tables, Images & PDF Export'),
    ('c3800000-0000-0000-0000-000000000001', '2a490000-0000-0000-0000-00000000000f', 12, 'Spreadsheet Navigation & Cell References'),
    ('c3800000-0000-0000-0000-000000000001', '2a490000-0000-0000-0000-00000000000f', 15, 'Essential Formulas: SUM, AVERAGE, COUNT & IF'),
    ('c3800000-0000-0000-0000-000000000001', '2a490000-0000-0000-0000-00000000000f', 14, 'Data Sorting, Filtering & Chart Visualization'),
    ('c3800000-0000-0000-0000-000000000001', '2a490000-0000-0000-0000-000000000010', 12, 'Professional Email Composition & Etiquette'),
    ('c3800000-0000-0000-0000-000000000001', '2a490000-0000-0000-0000-000000000010', 14, 'Password Security & Phishing Defense'),
    ('c0000000-0000-0000-0000-000000000005', '2a490000-0000-0000-0000-000000000011', 10, 'Parts of Speech: Nouns, Verbs & Adjectives'),
    ('c0000000-0000-0000-0000-000000000005', '2a490000-0000-0000-0000-000000000011', 12, 'Present Simple vs. Present Continuous Tense'),
    ('c0000000-0000-0000-0000-000000000005', '2a490000-0000-0000-0000-000000000011', 12, 'Past Simple: Regular & Irregular Verbs'),
    ('c0000000-0000-0000-0000-000000000005', '2a490000-0000-0000-0000-000000000012', 10, 'Daily Routines, Schedules & Time Expressions'),
    ('c0000000-0000-0000-0000-000000000005', '2a490000-0000-0000-0000-000000000012', 12, 'Making Requests, Polite Inquiries & Assistance'),
    ('c0000000-0000-0000-0000-000000000005', '2a490000-0000-0000-0000-000000000013', 14, 'Paragraph Structure & Strong Topic Sentences'),
    ('c0000000-0000-0000-0000-000000000005', '2a490000-0000-0000-0000-000000000013', 14, 'Cohesive Transition Words & Connecting Ideas'),
    ('c0000000-0000-0000-0000-000000000005', '2a490000-0000-0000-0000-000000000014', 25, 'English Proficiency Comprehensive Challenge'),
    ('c0000000-0000-0000-0000-000000000003', '2a490000-0000-0000-0000-000000000015', 12, 'ព្យញ្ជនៈ ស្រៈនិស្ស័យ និងសញ្ញាវណ្ណយុត្តិខ្មែរ'),
    ('c0000000-0000-0000-0000-000000000003', '2a490000-0000-0000-0000-000000000015', 14, 'ថ្នាក់ពាក្យទាំង ៧ ក្នុងវេយ្យាករណ៍ខ្មែរ'),
    ('c0000000-0000-0000-0000-000000000003', '2a490000-0000-0000-0000-000000000016', 15, 'ការវិភាគប្រលោមលោក «កុលាបប៉ៃលិន» និងទស្សនៈសង្គម'),
    ('c0000000-0000-0000-0000-000000000003', '2a490000-0000-0000-0000-000000000016', 16, 'រឿង «ទុំទាវ» និងសោកនាដកម្មនៃស្នេហាបុរាណ'),
    ('c0000000-0000-0000-0000-000000000003', '2a490000-0000-0000-0000-000000000017', 15, 'វិធីសាស្រ្តតែងសេចក្តីពន្យល់ និងប្រៀបធៀប'),
    ('c0000000-0000-0000-0000-000000000003', '2a490000-0000-0000-0000-000000000017', 16, 'ក្បួនកាព្យខ្មែរ៖ បទកាកគតិ និងបទព្រហ្មគីតិ'),
    ('c0000000-0000-0000-0000-000000000003', '2a490000-0000-0000-0000-000000000018', 25, 'វិញ្ញាសាសាកល្បងអក្សរសាស្ត្រខ្មែរ និងអក្សរសិល្ប៍'),
    ('c0000000-0000-0000-0000-000000000001', '2a100000-0000-0000-0000-000000000003', 15, 'Derivative Rules: Power, Product & Quotient'),
    ('c0000000-0000-0000-0000-000000000001', '2a100000-0000-0000-0000-000000000003', 18, 'The Chain Rule & Composite Functions'),
    ('c0000000-0000-0000-0000-000000000001', '2a100000-0000-0000-0000-000000000004', 18, 'Related Rates & Motion Problems'),
    ('c0000000-0000-0000-0000-000000000001', '2a100000-0000-0000-0000-000000000005', 15, 'The Fundamental Theorem of Calculus'),
    ('c0000000-0000-0000-0000-000000000001', '2a100000-0000-0000-0000-000000000007', 16, 'Exponential Growth & Decay Models'),
    ('c0000000-0000-0000-0000-000000000004', '2a200000-0000-0000-0000-000000000003', 15, 'Momentum & Conservation of Momentum'),
    ('c0000000-0000-0000-0000-000000000004', '2a200000-0000-0000-0000-000000000004', 16, 'Series & Parallel Electric Circuits'),
    ('c0000000-0000-0000-0000-000000000004', '2a200000-0000-0000-0000-000000000004', 18, 'Magnetic Fields & Electromagnetic Induction'),
    ('c0000000-0000-0000-0000-000000000004', '2a490000-0000-0000-0000-000000000001', 12, 'Specific Heat Capacity & Calorimetry'),
    ('c0000000-0000-0000-0000-000000000004', '2a490000-0000-0000-0000-000000000001', 15, 'Ideal Gas Laws & Kinetic Molecular Theory'),
    ('c0000000-0000-0000-0000-000000000004', '2a490000-0000-0000-0000-000000000002', 12, 'Wave Properties, Speed & Superposition'),
    ('c0000000-0000-0000-0000-000000000004', '2a490000-0000-0000-0000-000000000002', 15, 'Light, Refraction & Snell''s Law'),
    ('c0000000-0000-0000-0000-000000000004', '2a490000-0000-0000-0000-000000000003', 25, 'Physics Roadmap Capstone Challenge')
) AS v(category_id, unit_id, minutes, title)
WHERE l.category_id = v.category_id::uuid
  AND l.title = v.title
  AND l.roadmap_unit_id IS NULL
  AND EXISTS (SELECT 1 FROM "roadmap_units" u
              WHERE u.id = v.unit_id::uuid AND u.category_id = l.category_id);
