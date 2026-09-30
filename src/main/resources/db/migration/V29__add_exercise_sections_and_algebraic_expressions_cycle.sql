ALTER TABLE "exercises"
    ADD COLUMN IF NOT EXISTS section_index INTEGER NOT NULL DEFAULT 1;

ALTER TABLE "exercises"
    DROP CONSTRAINT IF EXISTS chk_exercises_section_index_positive;
ALTER TABLE "exercises"
    ADD CONSTRAINT chk_exercises_section_index_positive CHECK (section_index > 0);

-- Replace the small demo algebra lesson with the first complete, sectioned
-- learning cycle. Existing attempts are removed because the exercise content
-- is intentionally being replaced.
DELETE FROM "exercises"
WHERE lesson_id = '24000000-0000-0000-0000-000000000001';

UPDATE "lessons"
SET description = $$# Algebraic Expressions

## Variables & Constants

An **algebraic expression** combines numbers, variables, and operations, without an equals sign. A **variable** is a letter that can stand for a changing value, such as `x` in `4x + 7`. A **constant** is a number on its own, such as `7`.

The number multiplying a variable is its **coefficient**. In `4x + 7`, the coefficient of `x` is `4`; in `-3y`, the coefficient of `y` is `-3`. Reading each part carefully helps you identify what can be combined later.

## Like Terms

**Like terms** have exactly the same variable part, including exponents. For example, `3x` and `-5x` are like terms, and `2a²` and `7a²` are like terms. Their coefficients may be different.

Variables and exponents must match: `x` and `x²` are not like terms, and `3a` and `3b` are not like terms. Constants are also like terms with other constants.

## Simplifying Expressions

To simplify, combine like terms by adding or subtracting their coefficients. For example, `5x + 2x - 3` becomes `7x - 3`.

The distributive property can create like terms: `2(x + 3) + x` becomes `2x + 6 + x`, then `3x + 6`. Work one step at a time and keep unlike terms separate.

## Final Review

This challenge mixes everything from the lesson. Identify variables, constants, and coefficients; recognise like terms; then simplify accurately. No major new idea is introduced here.$$,
    difficulty = 'EASY',
    base_xp = 60,
    reflection_prompt = 'Explain in your own words how you identify and combine like terms in an algebraic expression. Give one example.',
    updated_at = now()
WHERE id = '24000000-0000-0000-0000-000000000001';

INSERT INTO "exercises" (id, lesson_id, question, answer_key, difficulty, points, section_index, type, options, hint, explanation) VALUES
-- Section 1: Variables & Constants
('26000000-0000-0000-0000-000000000001', '24000000-0000-0000-0000-000000000001', 'In `-7p + 12`, what is the coefficient of `p`?', '-7', 'EASY', 10, 1, 'TEXT', '[]', 'The coefficient includes the sign immediately before the variable.', 'The coefficient is `-7`, because `-7` multiplies `p`. The constant is `12`.'),
('26000000-0000-0000-0000-000000000002', '24000000-0000-0000-0000-000000000001', 'Which part of `5m - 9` is the constant term?', '-9', 'EASY', 10, 1, 'MULTIPLE_CHOICE', '["5m","-9","m","5"]', 'A constant has no variable attached to it.', '`-9` is the constant because it stands alone. `5m` contains the variable `m`.'),
-- Section 2: Like Terms
('26000000-0000-0000-0000-000000000003', '24000000-0000-0000-0000-000000000001', 'Which pair is made of like terms?', '4x and -9x', 'EASY', 12, 2, 'MULTIPLE_CHOICE', '["4x and -9x","3a and 3b","2y and 2y²","5 and 5z"]', 'Compare both the variable and its exponent.', '`4x` and `-9x` both have `x` to the first power, so they are like terms.'),
('26000000-0000-0000-0000-000000000004', '24000000-0000-0000-0000-000000000001', 'The terms `3x` and `3x²` are like terms.', 'False', 'EASY', 12, 2, 'TRUE_FALSE', '["True","False"]', 'Check whether the exponents match.', 'They are not like terms: `3x` has `x¹`, while `3x²` has `x²`.'),
-- Section 3: Simplifying Expressions
('26000000-0000-0000-0000-000000000005', '24000000-0000-0000-0000-000000000001', 'Simplify `4x + 6x`.', '10x', 'EASY', 15, 3, 'TEXT', '[]', 'The terms have the same variable part, so add their coefficients.', '`4x` and `6x` are like terms. Adding 4 and 6 gives `10x`.'),
('26000000-0000-0000-0000-000000000006', '24000000-0000-0000-0000-000000000001', 'What is the simplified form of `2(x + 3) + x`?', '3x + 6', 'MEDIUM', 15, 3, 'MULTIPLE_CHOICE', '["3x + 6","2x + 3","3x + 3","2x + 6"]', 'Distribute 2 first, then combine the `x` terms.', '`2(x + 3)` is `2x + 6`. Adding the remaining `x` gives `3x + 6`.'),
-- Section 4: Final Review
('26000000-0000-0000-0000-000000000007', '24000000-0000-0000-0000-000000000001', 'In `8a - 3`, the term `-3` is a constant.', 'True', 'EASY', 12, 4, 'TRUE_FALSE', '["True","False"]', 'A constant has no variable.', '`-3` has no variable, so it is the constant term.'),
('26000000-0000-0000-0000-000000000008', '24000000-0000-0000-0000-000000000001', 'What is the coefficient of `y` in `-2 + 8y`?', '8', 'EASY', 12, 4, 'TEXT', '[]', 'Find the number multiplying `y`, not the standalone number.', '`8` multiplies `y`, so it is the coefficient. `-2` is the constant.'),
('26000000-0000-0000-0000-000000000009', '24000000-0000-0000-0000-000000000001', 'Which expression correctly combines `6p - 2p + 5`?', '4p + 5', 'MEDIUM', 15, 4, 'MULTIPLE_CHOICE', '["4p + 5","8p + 5","4p","6p - 2p + 5p"]', 'Combine only the terms with `p`; keep the constant unchanged.', '`6p` and `-2p` are like terms, so their coefficients combine to `4p`. The `+ 5` remains.'),
('26000000-0000-0000-0000-000000000010', '24000000-0000-0000-0000-000000000001', 'Simplify `9m - 4m`.', '5m', 'MEDIUM', 15, 4, 'TEXT', '[]', 'Subtract the coefficients because both terms contain `m`.', '`9m - 4m` equals `(9 - 4)m`, which is `5m`.'),
('26000000-0000-0000-0000-000000000011', '24000000-0000-0000-0000-000000000001', 'Which is equivalent to `3(2x - 1) + x`?', '7x - 3', 'MEDIUM', 18, 4, 'MULTIPLE_CHOICE', '["7x - 3","6x - 3","7x - 1","6x - 2"]', 'Distribute 3 to both terms inside the parentheses before combining.', '`3(2x - 1)` becomes `6x - 3`. Adding `x` gives `7x - 3`.')
ON CONFLICT (id) DO UPDATE SET
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
