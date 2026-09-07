-- Replace the remaining three-exercise mathematics demos with the same
-- four-part learning cycle used by Algebraic Expressions. Deleting exercises
-- also removes their dependent attempts through the existing FK cascade.
DELETE FROM "exercises"
WHERE lesson_id IN (
    '24000000-0000-0000-0000-000000000002',
    '24000000-0000-0000-0000-000000000003',
    '24000000-0000-0000-0000-000000000004',
    '24000000-0000-0000-0000-000000000005',
    '24000000-0000-0000-0000-000000000006'
);

UPDATE "lessons"
SET description = $$# Linear Equations

## Understanding Equations
An **equation** says two expressions have the same value. The equals sign means the left-hand side and right-hand side are balanced. In `x + 4 = 9`, `x + 4` is the left-hand side and `9` is the right-hand side. The variable `x` is the unknown value.

## One-Step Equations
Use **inverse operations** to undo what is attached to the variable. Subtract to undo addition, add to undo subtraction, divide to undo multiplication, and multiply to undo division. Always do the same operation to both sides.

## Multi-Step Equations
For a multi-step equation, simplify each side first. Combine like terms, then use inverse operations one at a time to isolate the variable. Check by substituting your answer into the original equation.

## Final Review
This review combines equation vocabulary, one-step solving, and multi-step solving.$$,
    difficulty = 'EASY', base_xp = 70,
    reflection_prompt = 'Explain how inverse operations help you solve a linear equation. Give one example.', updated_at = now()
WHERE id = '24000000-0000-0000-0000-000000000002';

UPDATE "lessons"
SET description = $$# Linear Inequalities

## Understanding Inequalities
An **inequality** compares values using `<`, `>`, `<=`, or `>=`. Instead of one exact answer, an inequality usually has a solution set: all values that make the comparison true.

## Solving Inequalities
Solve inequalities much like equations by adding, subtracting, multiplying, or dividing both sides. One special rule matters: reverse the inequality sign when multiplying or dividing by a negative number.

## Graphing Solutions
Show solutions on a number line. Use an open circle for `<` or `>` because the endpoint is excluded. Use a closed circle for `<=` or `>=` because the endpoint is included. Shade in the direction of all solutions.

## Final Review
This review mixes symbols, solving rules, and number-line representations.$$,
    difficulty = 'EASY', base_xp = 80,
    reflection_prompt = 'Explain one important difference between solving equations and solving inequalities.', updated_at = now()
WHERE id = '24000000-0000-0000-0000-000000000003';

UPDATE "lessons"
SET description = $$# Functions & Graphs

## Understanding Functions
A **function** assigns exactly one output to each input. A relation is a function only when no input is paired with two different outputs. Function notation such as `f(x)` means the output produced by the input `x`.

## Evaluating Functions
To evaluate a function, substitute the given input for the variable and calculate the output. Tables can organize several input-output pairs and help show a pattern.

## Reading Graphs
The horizontal axis is the x-axis and the vertical axis is the y-axis. A plotted point `(x, y)` gives an input and its output. Read points and patterns carefully from a coordinate plane.

## Final Review
This review connects function rules, tables, and graph reading.$$,
    difficulty = 'MEDIUM', base_xp = 100,
    reflection_prompt = 'Explain what a function does and how you can find an output from an input.', updated_at = now()
WHERE id = '24000000-0000-0000-0000-000000000004';

UPDATE "lessons"
SET description = $$# Quadratic Equations

## Understanding Quadratics
A quadratic expression or equation contains an `x^2` term. A quadratic equation can be written in standard form as `ax^2 + bx + c = 0`, where `a` is not zero.

## Factoring Simple Quadratics
For `x^2 + bx + c`, find two numbers whose product is `c` and whose sum is `b`. For example, `x^2 + 5x + 6` factors to `(x + 2)(x + 3)`. Multiply the factors to check your work.

## Solving by Factoring
Set the quadratic equal to zero, factor it, then use the **zero-product property**: if a product is zero, at least one factor is zero. Solve each factor to find the roots.

## Final Review
This review combines identifying quadratics, factoring, and finding roots.$$,
    difficulty = 'MEDIUM', base_xp = 120,
    reflection_prompt = 'Explain how factoring can help you solve a quadratic equation.', updated_at = now()
WHERE id = '24000000-0000-0000-0000-000000000005';

UPDATE "lessons"
SET description = $$# Pythagorean Theorem

## Right Triangles
The Pythagorean Theorem works only with a **right triangle**, which has one 90-degree angle. The two sides that meet at the right angle are legs. The longest side opposite that angle is the hypotenuse.

## The Formula
For a right triangle, `a^2 + b^2 = c^2`. The letters `a` and `b` represent the legs, while `c` represents the hypotenuse. Identify the hypotenuse before using the formula.

## Finding Missing Sides
Use the formula to find a missing hypotenuse or leg. After isolating the squared value, take a square root. This helps solve practical right-triangle problems, such as a ladder against a wall.

## Final Review
This review combines triangle vocabulary, the formula, and missing-side problems.$$,
    difficulty = 'MEDIUM', base_xp = 150,
    reflection_prompt = 'Explain when you can use the Pythagorean Theorem and what each part of a^2 + b^2 = c^2 represents.', updated_at = now()
WHERE id = '24000000-0000-0000-0000-000000000006';

INSERT INTO "exercises" (id, lesson_id, question, answer_key, difficulty, points, section_index, type, options, hint, explanation) VALUES
-- Linear Equations: Section 1
('27000000-0000-0000-0000-000000000001','24000000-0000-0000-0000-000000000002','In `3x + 2 = 14`, what is the right-hand side?','14','EASY',10,1,'TEXT','[]','Look to the right of the equals sign.','The right-hand side is `14`; `3x + 2` is on the left-hand side.'),
('27000000-0000-0000-0000-000000000002','24000000-0000-0000-0000-000000000002','Which statement describes an equation?','Two expressions have the same value','EASY',10,1,'MULTIPLE_CHOICE','["A variable is always zero","Two expressions have the same value","It has no variable","It is a graph"]','Focus on what the equals sign tells you.','An equation states that its two sides have the same value.'),
-- Linear Equations: Section 2
('27000000-0000-0000-0000-000000000003','24000000-0000-0000-0000-000000000002','Solve: `x - 7 = 5`.','12','EASY',12,2,'TEXT','[]','Use addition to undo subtracting 7.','Add 7 to both sides: `x = 12`.'),
('27000000-0000-0000-0000-000000000004','24000000-0000-0000-0000-000000000002','To solve `4x = 28`, divide both sides by 4.','True','EASY',12,2,'TRUE_FALSE','["True","False"]','Division undoes multiplication by 4.','Dividing both sides by 4 leaves `x = 7`.'),
-- Linear Equations: Section 3
('27000000-0000-0000-0000-000000000005','24000000-0000-0000-0000-000000000002','Solve: `2x + 5 = 17`.','6','MEDIUM',15,3,'TEXT','[]','Subtract 5 first, then divide by 2.','`2x = 12` after subtracting 5, so `x = 6`.'),
('27000000-0000-0000-0000-000000000006','24000000-0000-0000-0000-000000000002','What should you do first to solve `3x + 2x - 4 = 16`?','combine like terms','MEDIUM',15,3,'MULTIPLE_CHOICE','["divide by 3","combine like terms","add 4 to the left only","multiply by 16"]','Simplify the side containing two x terms before isolating x.','Combine `3x` and `2x` first to get `5x - 4 = 16`.'),
-- Linear Equations: Final Review
('27000000-0000-0000-0000-000000000007','24000000-0000-0000-0000-000000000002','The same operation must be performed on both sides of an equation.','True','EASY',12,4,'TRUE_FALSE','["True","False"]','Think of an equation as a balanced scale.','Doing the same operation to both sides preserves equality.'),
('27000000-0000-0000-0000-000000000008','24000000-0000-0000-0000-000000000002','Solve: `x / 3 = 8`.','24','EASY',12,4,'TEXT','[]','Undo division by 3 using multiplication.','Multiply both sides by 3, giving `x = 24`.'),
('27000000-0000-0000-0000-000000000009','24000000-0000-0000-0000-000000000002','Solve: `5x - 10 = 20`.','6','MEDIUM',15,4,'TEXT','[]','Add 10 first, then divide by 5.','Adding 10 gives `5x = 30`; dividing by 5 gives `x = 6`.'),
('27000000-0000-0000-0000-000000000010','24000000-0000-0000-0000-000000000002','Which value checks `3x + 1 = 16`?','5','MEDIUM',15,4,'MULTIPLE_CHOICE','["3","5","15","17"]','Try the value that makes `3x` equal 15.','For `x = 5`, `3(5) + 1 = 16`, so it checks.'),
('27000000-0000-0000-0000-000000000011','24000000-0000-0000-0000-000000000002','Solve: `4x + 3 = 2x + 13`.','5','MEDIUM',18,4,'TEXT','[]','Move the x terms to one side, then isolate x.','Subtract `2x` from both sides to get `2x + 3 = 13`; then `2x = 10`, so `x = 5`.'),
-- Linear Inequalities: Section 1
('27000000-0000-0000-0000-000000000012','24000000-0000-0000-0000-000000000003','Which symbol means greater than or equal to?','>=','EASY',10,1,'MULTIPLE_CHOICE','[">","<",">=","<="]','Choose the greater-than symbol with a line under it.','`>=` includes values greater than the boundary and the boundary itself.'),
('27000000-0000-0000-0000-000000000013','24000000-0000-0000-0000-000000000003','Does `x > 4` include the value 4?','False','EASY',10,1,'TRUE_FALSE','["True","False"]','A strict greater-than sign does not include its endpoint.','`x > 4` includes values larger than 4, but not 4 itself.'),
-- Linear Inequalities: Section 2
('27000000-0000-0000-0000-000000000014','24000000-0000-0000-0000-000000000003','Solve: `x + 3 < 9`.','x < 6','EASY',12,2,'TEXT','[]','Subtract 3 from both sides.','Subtracting 3 gives `x < 6`.'),
('27000000-0000-0000-0000-000000000015','24000000-0000-0000-0000-000000000003','When dividing an inequality by a negative number, reverse the inequality sign.','True','MEDIUM',15,2,'TRUE_FALSE','["True","False"]','A negative operation reverses the order of numbers.','For example, dividing `-2x > 8` by `-2` gives `x < -4`.'),
-- Linear Inequalities: Section 3
('27000000-0000-0000-0000-000000000016','24000000-0000-0000-0000-000000000003','Which graph matches `x <= 2`?','closed circle at 2 shaded left','EASY',15,3,'MULTIPLE_CHOICE','["open circle at 2 shaded right","closed circle at 2 shaded left","open circle at 2 shaded left","closed circle at 2 shaded right"]','`<=` includes 2 and all values smaller than 2.','Use a closed circle because 2 is included, and shade left for smaller values.'),
('27000000-0000-0000-0000-000000000017','24000000-0000-0000-0000-000000000003','What kind of circle is used for `x > -1` on a number line?','open circle','EASY',12,3,'TEXT','[]','The endpoint is not included for a strict inequality.','Use an open circle at `-1` because `x` must be greater than, not equal to, `-1`.'),
-- Linear Inequalities: Final Review
('27000000-0000-0000-0000-000000000018','24000000-0000-0000-0000-000000000003','Solve: `2x - 1 >= 7`.','x >= 4','MEDIUM',15,4,'TEXT','[]','Add 1, then divide by positive 2.','`2x >= 8`, so dividing by 2 gives `x >= 4`.'),
('27000000-0000-0000-0000-000000000019','24000000-0000-0000-0000-000000000003','Solve: `-3x < 12`.','x > -4','MEDIUM',18,4,'TEXT','[]','Divide by -3 and remember the special rule.','Dividing by `-3` reverses the sign: `x > -4`.'),
('27000000-0000-0000-0000-000000000020','24000000-0000-0000-0000-000000000003','A closed circle means the endpoint is included.','True','EASY',12,4,'TRUE_FALSE','["True","False"]','Compare closed circles with `<=` and `>=`.','A closed circle shows that the boundary value is part of the solution.'),
('27000000-0000-0000-0000-000000000021','24000000-0000-0000-0000-000000000003','Which value is a solution to `x < 2`?','1','EASY',12,4,'MULTIPLE_CHOICE','["2","3","1","4"]','Choose a number strictly less than 2.','`1` is less than 2, so it is in the solution set.'),
('27000000-0000-0000-0000-000000000022','24000000-0000-0000-0000-000000000003','Solve: `5 - x > 1`.','x < 4','MEDIUM',18,4,'TEXT','[]','Subtract 5, then multiply by -1 and reverse the sign.','`-x > -4`; multiplying by `-1` reverses the inequality to `x < 4`.'),
-- Functions & Graphs: Section 1
('27000000-0000-0000-0000-000000000023','24000000-0000-0000-0000-000000000004','In `f(x) = 2x + 1`, what does `x` represent?','input','EASY',10,1,'TEXT','[]','The value put into a function is called its input.','`x` is the input; the function rule produces an output from it.'),
('27000000-0000-0000-0000-000000000024','24000000-0000-0000-0000-000000000004','Which relation is a function?','Each input has exactly one output','EASY',12,1,'MULTIPLE_CHOICE','["One input has two outputs","Each input has exactly one output","There are no inputs","Every output has two inputs"]','Use the rule about a single output for each input.','A function may share an output, but every input must have exactly one output.'),
-- Functions & Graphs: Section 2
('27000000-0000-0000-0000-000000000025','24000000-0000-0000-0000-000000000004','If `f(x) = 3x - 2`, find `f(4)`.','10','EASY',15,2,'TEXT','[]','Replace x with 4 before calculating.','`f(4) = 3(4) - 2 = 10`.'),
('27000000-0000-0000-0000-000000000026','24000000-0000-0000-0000-000000000004','For `g(x) = x + 5`, the output when x is 2 is 7.','True','EASY',12,2,'TRUE_FALSE','["True","False"]','Substitute 2 into the rule.','`g(2) = 2 + 5 = 7`, so the statement is true.'),
-- Functions & Graphs: Section 3
('27000000-0000-0000-0000-000000000027','24000000-0000-0000-0000-000000000004','In the point `(3, -2)`, what is the y-coordinate?','-2','EASY',12,3,'TEXT','[]','Coordinates are written in the order `(x, y)`.','The second number is the y-coordinate, so it is `-2`.'),
('27000000-0000-0000-0000-000000000028','24000000-0000-0000-0000-000000000004','Which axis runs horizontally on a coordinate plane?','x-axis','EASY',12,3,'MULTIPLE_CHOICE','["x-axis","y-axis","origin","vertical axis"]','Horizontal is left-to-right.','The x-axis runs horizontally; the y-axis runs vertically.'),
-- Functions & Graphs: Final Review
('27000000-0000-0000-0000-000000000029','24000000-0000-0000-0000-000000000004','If `h(x) = x^2`, find `h(5)`.','25','MEDIUM',15,4,'TEXT','[]','Square the input value.','`h(5) = 5^2 = 25`.'),
('27000000-0000-0000-0000-000000000030','24000000-0000-0000-0000-000000000004','A vertical line crossing a graph more than once shows the graph is not a function.','True','MEDIUM',15,4,'TRUE_FALSE','["True","False"]','Use the vertical-line test: one input may not have two outputs.','If a vertical line hits twice, one x-value has two y-values, so it is not a function.'),
('27000000-0000-0000-0000-000000000031','24000000-0000-0000-0000-000000000004','Which ordered pair is on `y = x + 2`?','(2, 4)','MEDIUM',15,4,'MULTIPLE_CHOICE','["(2, 2)","(2, 4)","(3, 2)","(0, 0)"]','Substitute the x-value into the rule and compare y.','When `x = 2`, `y = 2 + 2 = 4`, so `(2, 4)` is on the graph.'),
('27000000-0000-0000-0000-000000000032','24000000-0000-0000-0000-000000000004','For `f(x) = 2x`, what output belongs in a table when the input is -3?','-6','MEDIUM',15,4,'TEXT','[]','Multiply the input by 2.','`f(-3) = 2(-3) = -6`.'),
('27000000-0000-0000-0000-000000000033','24000000-0000-0000-0000-000000000004','The point `(0, 5)` lies on the y-axis.','True','EASY',12,4,'TRUE_FALSE','["True","False"]','Points on the y-axis have x-coordinate 0.','The x-coordinate is 0, so `(0, 5)` lies on the y-axis.'),
-- Quadratic Equations: Section 1
('27000000-0000-0000-0000-000000000034','24000000-0000-0000-0000-000000000005','Which expression is quadratic?','x^2 - 4x + 1','EASY',12,1,'MULTIPLE_CHOICE','["3x + 1","x^2 - 4x + 1","x + y","7"]','Look for a variable squared.','A quadratic expression has a squared variable term, such as `x^2`.'),
('27000000-0000-0000-0000-000000000035','24000000-0000-0000-0000-000000000005','In `2x^2 + 3x - 4 = 0`, what is the coefficient a?','2','EASY',12,1,'TEXT','[]','Compare the equation with `ax^2 + bx + c = 0`.','The number multiplying `x^2` is `2`, so `a = 2`.'),
-- Quadratic Equations: Section 2
('27000000-0000-0000-0000-000000000036','24000000-0000-0000-0000-000000000005','Which factorization equals `x^2 + 5x + 6`?','(x + 2)(x + 3)','MEDIUM',15,2,'MULTIPLE_CHOICE','["(x + 1)(x + 6)","(x + 2)(x + 3)","(x - 2)(x - 3)","(x + 5)(x + 6)"]','Find numbers that multiply to 6 and add to 5.','`2` and `3` multiply to 6 and add to 5, so the factors are `(x + 2)(x + 3)`.'),
('27000000-0000-0000-0000-000000000037','24000000-0000-0000-0000-000000000005','The numbers used to factor `x^2 + 7x + 12` must multiply to 12 and add to 7.','True','MEDIUM',12,2,'TRUE_FALSE','["True","False"]','Use the middle coefficient and constant for a simple monic quadratic.','For this form, the factor numbers must have product 12 and sum 7.'),
-- Quadratic Equations: Section 3
('27000000-0000-0000-0000-000000000038','24000000-0000-0000-0000-000000000005','Solve: `(x - 4)(x + 1) = 0`. Give the positive root.','4','MEDIUM',15,3,'TEXT','[]','Set each factor equal to zero.','`x - 4 = 0` gives `x = 4`; the other root is `-1`.'),
('27000000-0000-0000-0000-000000000039','24000000-0000-0000-0000-000000000005','The zero-product property says that if a product is zero, at least one factor is zero.','True','MEDIUM',12,3,'TRUE_FALSE','["True","False"]','Think about what is required for multiplication to produce zero.','A product can equal zero only if one or more factors equal zero.'),
-- Quadratic Equations: Final Review
('27000000-0000-0000-0000-000000000040','24000000-0000-0000-0000-000000000005','Factor: `x^2 + 4x + 3`.','(x + 1)(x + 3)','MEDIUM',15,4,'TEXT','[]','Find two numbers with product 3 and sum 4.','`1` and `3` multiply to 3 and add to 4, giving `(x + 1)(x + 3)`.'),
('27000000-0000-0000-0000-000000000041','24000000-0000-0000-0000-000000000005','What are the roots of `x^2 - 9 = 0`?','3 and -3','MEDIUM',18,4,'MULTIPLE_CHOICE','["9 and -9","3 and -3","3 only","-3 only"]','Factor the difference of squares as `(x - 3)(x + 3)`.','Setting each factor equal to zero gives roots `3` and `-3`.'),
('27000000-0000-0000-0000-000000000042','24000000-0000-0000-0000-000000000005','An equation with an `x^2` term can be quadratic.','True','EASY',12,4,'TRUE_FALSE','["True","False"]','Look at the defining feature of a quadratic.','A nonzero squared-variable term is the key feature of a quadratic equation.'),
('27000000-0000-0000-0000-000000000043','24000000-0000-0000-0000-000000000005','Solve: `x^2 + x - 6 = 0`. Give the larger root.','2','MEDIUM',18,4,'TEXT','[]','Factor using numbers whose product is -6 and sum is 1.','`(x + 3)(x - 2) = 0`, so the roots are `-3` and `2`; the larger is `2`.'),
('27000000-0000-0000-0000-000000000044','24000000-0000-0000-0000-000000000005','Which step comes after factoring a quadratic equation set equal to zero?','set each factor equal to zero','MEDIUM',15,4,'MULTIPLE_CHOICE','["add the factors","set each factor equal to zero","divide by x","square both sides"]','Apply the zero-product property after factoring.','Once the product is zero, set every factor equal to zero and solve.'),
-- Pythagorean Theorem: Section 1
('27000000-0000-0000-0000-000000000045','24000000-0000-0000-0000-000000000006','What is the longest side of a right triangle called?','hypotenuse','EASY',10,1,'TEXT','[]','It lies opposite the 90-degree angle.','The hypotenuse is the longest side and is opposite the right angle.'),
('27000000-0000-0000-0000-000000000046','24000000-0000-0000-0000-000000000006','The Pythagorean Theorem applies to every triangle.','False','EASY',12,1,'TRUE_FALSE','["True","False"]','Check what kind of triangle the theorem requires.','It applies only to right triangles.'),
-- Pythagorean Theorem: Section 2
('27000000-0000-0000-0000-000000000047','24000000-0000-0000-0000-000000000006','In `a^2 + b^2 = c^2`, which letter represents the hypotenuse?','c','EASY',12,2,'TEXT','[]','The hypotenuse is placed alone on one side of the formula.','`c` is the hypotenuse; `a` and `b` are the legs.'),
('27000000-0000-0000-0000-000000000048','24000000-0000-0000-0000-000000000006','For legs of lengths 6 and 8, what is `c^2`?','100','MEDIUM',15,2,'MULTIPLE_CHOICE','["14","48","100","10"]','Square 6 and 8, then add the results.','`c^2 = 6^2 + 8^2 = 36 + 64 = 100`.'),
-- Pythagorean Theorem: Section 3
('27000000-0000-0000-0000-000000000049','24000000-0000-0000-0000-000000000006','A right triangle has legs 5 and 12. Find its hypotenuse.','13','MEDIUM',15,3,'TEXT','[]','Calculate `5^2 + 12^2`, then take a square root.','`25 + 144 = 169`, and `sqrt(169) = 13`.'),
('27000000-0000-0000-0000-000000000050','24000000-0000-0000-0000-000000000006','A right triangle has hypotenuse 10 and one leg 6. What is the other leg?','8','MEDIUM',18,3,'MULTIPLE_CHOICE','["4","6","8","16"]','Use `b^2 = c^2 - a^2`.','`b^2 = 100 - 36 = 64`, so `b = 8`.'),
-- Pythagorean Theorem: Final Review
('27000000-0000-0000-0000-000000000051','24000000-0000-0000-0000-000000000006','In a right triangle, the hypotenuse is opposite the right angle.','True','EASY',12,4,'TRUE_FALSE','["True","False"]','Locate the side across from the 90-degree angle.','The side opposite the right angle is always the hypotenuse.'),
('27000000-0000-0000-0000-000000000052','24000000-0000-0000-0000-000000000006','A ladder is 10 m long and its base is 6 m from a wall. How high does it reach?','8','MEDIUM',18,4,'TEXT','[]','The ladder is the hypotenuse; use the missing-leg form.','`height^2 = 10^2 - 6^2 = 64`, so the height is 8 m.'),
('27000000-0000-0000-0000-000000000053','24000000-0000-0000-0000-000000000006','Which set of side lengths makes a right triangle?','8, 15, 17','MEDIUM',15,4,'MULTIPLE_CHOICE','["3, 4, 6","5, 5, 5","8, 15, 17","2, 3, 4"]','Test whether the two shorter squares add to the longest square.','`8^2 + 15^2 = 64 + 225 = 289 = 17^2`, so these lengths make a right triangle.'),
('27000000-0000-0000-0000-000000000054','24000000-0000-0000-0000-000000000006','For a right triangle with legs 9 and 12, find the hypotenuse.','15','MEDIUM',18,4,'TEXT','[]','Add `9^2` and `12^2`, then take the square root.','`81 + 144 = 225`, and `sqrt(225) = 15`.'),
('27000000-0000-0000-0000-000000000055','24000000-0000-0000-0000-000000000006','Before using `a^2 + b^2 = c^2`, you should verify that the triangle is right-angled.','True','EASY',12,4,'TRUE_FALSE','["True","False"]','The theorem has a specific triangle requirement.','The Pythagorean Theorem is valid only for right triangles.')
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
