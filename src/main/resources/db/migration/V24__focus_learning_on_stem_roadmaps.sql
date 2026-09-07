-- Learning is temporarily focused on STEM. Categories remain because they are
-- shared with community posts and blogs; only non-STEM lesson content is
-- removed.
DELETE FROM "lessons"
WHERE category_id IN (
    'c0000000-0000-0000-0000-000000000003', -- Khmer Literature
    'c0000000-0000-0000-0000-000000000005'  -- English Learning
);

INSERT INTO "roadmap_units" (id, category_id, title, description, position) VALUES
    ('2a200000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000002', 'Programming Foundations', 'Build algorithms and solve problems step by step', 1),
    ('2a200000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', 'Systems & Networks', 'Understand computers, networks, and the internet', 2),
    ('2a200000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000004', 'Mechanics & Motion', 'Connect forces, motion, and Newton''s laws', 1),
    ('2a200000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000004', 'Electricity & Circuits', 'Explore voltage, current, resistance, and circuits', 2),
    ('2a200000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000006', 'Atomic Structure & Bonding', 'Study the periodic table and how atoms bond', 1),
    ('2a200000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000006', 'Equilibrium & Kinetics', 'Understand reaction balance and reaction rates', 2),
    ('2a200000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000007', 'Cell Biology', 'Learn how cells and organelles sustain life', 1),
    ('2a200000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000007', 'Genetics & DNA', 'Explore inheritance, genes, and DNA', 2);

UPDATE "lessons"
SET roadmap_unit_id = '2a200000-0000-0000-0000-000000000001', position = 1, estimated_minutes = 12, updated_at = now()
WHERE id = '1e550000-0000-0000-0000-000000000003';

UPDATE "lessons"
SET roadmap_unit_id = '2a200000-0000-0000-0000-000000000002', position = 1, estimated_minutes = 12, updated_at = now()
WHERE id = '1e550000-0000-0000-0000-000000000004';

UPDATE "lessons"
SET roadmap_unit_id = '2a200000-0000-0000-0000-000000000003', position = 1, estimated_minutes = 12, updated_at = now()
WHERE id = '1e550000-0000-0000-0000-000000000005';

UPDATE "lessons"
SET roadmap_unit_id = '2a200000-0000-0000-0000-000000000004', position = 1, estimated_minutes = 12, updated_at = now()
WHERE id = '1e550000-0000-0000-0000-000000000011';

UPDATE "lessons"
SET roadmap_unit_id = '2a200000-0000-0000-0000-000000000005', position = 1, estimated_minutes = 12, updated_at = now()
WHERE id = '1e550000-0000-0000-0000-000000000008';

UPDATE "lessons"
SET roadmap_unit_id = '2a200000-0000-0000-0000-000000000006', position = 1, estimated_minutes = 12, updated_at = now()
WHERE id = '1e550000-0000-0000-0000-000000000012';

UPDATE "lessons"
SET roadmap_unit_id = '2a200000-0000-0000-0000-000000000007', position = 1, estimated_minutes = 12, updated_at = now()
WHERE id = '1e550000-0000-0000-0000-000000000013';

UPDATE "lessons"
SET roadmap_unit_id = '2a200000-0000-0000-0000-000000000008', position = 1, estimated_minutes = 12, updated_at = now()
WHERE id = '1e550000-0000-0000-0000-000000000014';
