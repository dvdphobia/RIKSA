CREATE TABLE "roadmap_units" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category_id UUID NOT NULL REFERENCES "categories"(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    position INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (category_id, position)
);

ALTER TABLE "lessons"
    ADD COLUMN roadmap_unit_id UUID REFERENCES "roadmap_units"(id) ON DELETE SET NULL,
    ADD COLUMN position INTEGER,
    ADD COLUMN estimated_minutes INTEGER NOT NULL DEFAULT 8;

CREATE UNIQUE INDEX idx_lessons_roadmap_unit_position
    ON "lessons"(roadmap_unit_id, position)
    WHERE roadmap_unit_id IS NOT NULL AND position IS NOT NULL;

CREATE TABLE "user_lesson_progress" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    lesson_id UUID NOT NULL REFERENCES "lessons"(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_accessed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, lesson_id),
    CONSTRAINT chk_user_lesson_progress_status CHECK (status IN ('IN_PROGRESS'))
);

CREATE TABLE "roadmap_unit_skips" (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    roadmap_unit_id UUID NOT NULL REFERENCES "roadmap_units"(id) ON DELETE CASCADE,
    skipped_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, roadmap_unit_id)
);

CREATE INDEX idx_roadmap_units_category_position ON "roadmap_units"(category_id, position);
CREATE INDEX idx_user_lesson_progress_user ON "user_lesson_progress"(user_id, updated_at DESC);
CREATE INDEX idx_roadmap_unit_skips_user ON "roadmap_unit_skips"(user_id);

INSERT INTO "roadmap_units" (id, category_id, title, description, position) VALUES
    ('2a100000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'Foundations', 'Basic concepts, numbers, and algebraic fluency', 1),
    ('2a100000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000001', 'Functions & Graphs', 'Understand functions and their graphs', 2),
    ('2a100000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000001', 'Derivatives & Tangent Lines', 'Find rates of change and tangent lines', 3),
    ('2a100000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000001', 'Applications of Derivatives', 'Solve real-world optimization problems', 4),
    ('2a100000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000001', 'Integrals', 'Build an understanding of accumulation', 5),
    ('2a100000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000001', 'Integral Applications', 'Use integrals for area, volume, and motion', 6),
    ('2a100000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000001', 'Differential Equations', 'Model change with differential equations', 7),
    ('2a100000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000001', 'Mathematics Challenge', 'Test what you have learned across the roadmap', 8)
ON CONFLICT (category_id, position) DO UPDATE
SET title = EXCLUDED.title, description = EXCLUDED.description, updated_at = now();

INSERT INTO "lessons" (id, title, description, difficulty, base_xp, category_id, roadmap_unit_id, position, estimated_minutes, reflection_prompt) VALUES
    ('2e100000-0000-0000-0000-000000000001', 'Number Systems & Operations', 'Work confidently with integers, fractions, decimals, and order of operations.', 'EASY', 15, 'c0000000-0000-0000-0000-000000000001', '2a100000-0000-0000-0000-000000000001', 1, 8, 'Which number-system rule is most useful in everyday calculations?'),
    ('2e100000-0000-0000-0000-000000000002', 'Algebraic Expressions', 'Simplify, expand, and factor common algebraic expressions.', 'EASY', 20, 'c0000000-0000-0000-0000-000000000001', '2a100000-0000-0000-0000-000000000001', 2, 10, 'How does simplifying an expression make a problem easier to reason about?'),
    ('2e100000-0000-0000-0000-000000000003', 'Equations & Inequalities', 'Solve linear equations and represent inequality solutions.', 'EASY', 25, 'c0000000-0000-0000-0000-000000000001', '2a100000-0000-0000-0000-000000000001', 3, 12, 'What must remain balanced when solving an equation?'),
    ('2e100000-0000-0000-0000-000000000004', 'The Coordinate Plane', 'Plot points, measure slope, and read relationships on a coordinate plane.', 'EASY', 20, 'c0000000-0000-0000-0000-000000000001', '2a100000-0000-0000-0000-000000000001', 4, 10, 'How can a coordinate graph communicate a relationship more clearly than a table?'),
    ('2e100000-0000-0000-0000-000000000005', 'Exponents & Radicals', 'Apply exponent rules and simplify radical expressions.', 'MEDIUM', 30, 'c0000000-0000-0000-0000-000000000001', '2a100000-0000-0000-0000-000000000001', 5, 14, 'Describe one connection between exponents and radicals.'),
    ('2e100000-0000-0000-0000-000000000006', 'Introduction to Functions', 'Use function notation, domains, ranges, and input-output rules.', 'EASY', 25, 'c0000000-0000-0000-0000-000000000001', '2a100000-0000-0000-0000-000000000002', 1, 12, 'What makes a relation a function?'),
    ('2e100000-0000-0000-0000-000000000007', 'Reading and Transforming Graphs', 'Interpret key graph features and transformations.', 'MEDIUM', 30, 'c0000000-0000-0000-0000-000000000001', '2a100000-0000-0000-0000-000000000002', 2, 15, 'How does changing a function affect its graph?'),
    ('2e100000-0000-0000-0000-000000000008', 'Optimization with Derivatives', 'Find maxima and minima in practical optimization problems.', 'HARD', 40, 'c0000000-0000-0000-0000-000000000001', '2a100000-0000-0000-0000-000000000004', 1, 18, 'Why do critical points matter in optimization?'),
    ('2e100000-0000-0000-0000-000000000009', 'Area and Volume with Integrals', 'Calculate areas between curves and volumes of solids.', 'HARD', 40, 'c0000000-0000-0000-0000-000000000001', '2a100000-0000-0000-0000-000000000006', 1, 20, 'How does an integral turn slices into a total area or volume?'),
    ('2e100000-0000-0000-0000-000000000010', 'First-Order Differential Equations', 'Recognize and solve introductory separable differential equations.', 'DIFFICULT', 50, 'c0000000-0000-0000-0000-000000000001', '2a100000-0000-0000-0000-000000000007', 1, 24, 'What kind of real-world change can a differential equation model?'),
    ('2e100000-0000-0000-0000-000000000011', 'Mathematics Roadmap Challenge', 'Combine algebra, functions, derivatives, and integrals in a final challenge.', 'DIFFICULT', 60, 'c0000000-0000-0000-0000-000000000001', '2a100000-0000-0000-0000-000000000008', 1, 30, 'Which idea from this roadmap changed how you approach mathematical problems?')
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

UPDATE "lessons" SET roadmap_unit_id = '2a100000-0000-0000-0000-000000000003', position = 1, estimated_minutes = 16
WHERE id = '1e550000-0000-0000-0000-000000000001';
UPDATE "lessons" SET roadmap_unit_id = '2a100000-0000-0000-0000-000000000005', position = 1, estimated_minutes = 18
WHERE id = '1e550000-0000-0000-0000-000000000002';

INSERT INTO "exercises" (id, lesson_id, question, answer_key, difficulty, points) VALUES
    ('2f100000-0000-0000-0000-000000000001', '2e100000-0000-0000-0000-000000000001', 'Evaluate 8 - 3 × 2.', '2', 'EASY', 5),
    ('2f100000-0000-0000-0000-000000000002', '2e100000-0000-0000-0000-000000000001', 'Write 0.75 as a simplified fraction.', '3/4', 'EASY', 5),
    ('2f100000-0000-0000-0000-000000000003', '2e100000-0000-0000-0000-000000000002', 'Simplify 3x + 2x - 4.', '5x - 4', 'EASY', 5),
    ('2f100000-0000-0000-0000-000000000004', '2e100000-0000-0000-0000-000000000002', 'Expand 2(x + 3).', '2x + 6', 'EASY', 5),
    ('2f100000-0000-0000-0000-000000000005', '2e100000-0000-0000-0000-000000000003', 'Solve 2x + 3 = 11.', '4', 'EASY', 5),
    ('2f100000-0000-0000-0000-000000000006', '2e100000-0000-0000-0000-000000000003', 'Solve x - 2 > 5.', 'x > 7', 'EASY', 5),
    ('2f100000-0000-0000-0000-000000000007', '2e100000-0000-0000-0000-000000000004', 'Which quadrant contains (-2, 3)?', 'II', 'EASY', 5),
    ('2f100000-0000-0000-0000-000000000008', '2e100000-0000-0000-0000-000000000004', 'Find the slope from (0, 1) to (2, 5).', '2', 'EASY', 5),
    ('2f100000-0000-0000-0000-000000000009', '2e100000-0000-0000-0000-000000000005', 'Simplify 2^3 × 2^2.', '32', 'MEDIUM', 10),
    ('2f100000-0000-0000-0000-000000000010', '2e100000-0000-0000-0000-000000000005', 'Simplify √49.', '7', 'MEDIUM', 10)
ON CONFLICT (id) DO UPDATE SET
    question = EXCLUDED.question,
    answer_key = EXCLUDED.answer_key,
    difficulty = EXCLUDED.difficulty,
    points = EXCLUDED.points,
    updated_at = now();

INSERT INTO "user_lesson_progress" (user_id, lesson_id, status, started_at, last_accessed_at, updated_at)
SELECT user_id, lesson_id, 'IN_PROGRESS', completed_at, completed_at, completed_at
FROM "lesson_completions"
ON CONFLICT (user_id, lesson_id) DO NOTHING;
