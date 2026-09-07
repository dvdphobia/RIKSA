-- Phase 2: concise high-school curriculum for the Category -> Lesson -> Exercise flow.
-- The existing DifficultyLevel enum uses EASY and MEDIUM; they represent BEGINNER and INTERMEDIATE.

-- Keep existing demo lessons, but reserve positions 1-6 in these three categories for this curriculum.
UPDATE "lessons" lesson
SET position = position + 100
WHERE lesson.position <= 6
  AND lesson.category_id IN (SELECT id FROM "categories" WHERE slug IN ('mathematics', 'physics', 'biology'))
  AND lesson.id NOT IN (
      '24000000-0000-0000-0000-000000000001', '24000000-0000-0000-0000-000000000002',
      '24000000-0000-0000-0000-000000000003', '24000000-0000-0000-0000-000000000004',
      '24000000-0000-0000-0000-000000000005', '24000000-0000-0000-0000-000000000006',
      '24000000-0000-0000-0000-000000000007', '24000000-0000-0000-0000-000000000008',
      '24000000-0000-0000-0000-000000000009', '24000000-0000-0000-0000-000000000010',
      '24000000-0000-0000-0000-000000000011', '24000000-0000-0000-0000-000000000012',
      '24000000-0000-0000-0000-000000000013', '24000000-0000-0000-0000-000000000014',
      '24000000-0000-0000-0000-000000000015', '24000000-0000-0000-0000-000000000016',
      '24000000-0000-0000-0000-000000000017', '24000000-0000-0000-0000-000000000018'
  );

-- Reuse V22 categories by slug, refreshing their short learning descriptions and compatible icon URLs.
INSERT INTO "categories" (id, name, slug, description, icon_url) VALUES
 ('c0000000-0000-0000-0000-000000000001', 'Mathematics', 'mathematics', 'Build confident problem-solving skills, one idea at a time.', 'https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=150'),
 ('c0000000-0000-0000-0000-000000000004', 'Physics', 'physics', 'Explore motion, forces, energy, and everyday physical science.', 'https://images.unsplash.com/photo-1636466497217-26a8cbeaf0aa?w=150'),
 ('c0000000-0000-0000-0000-000000000007', 'Biology', 'biology', 'Discover living systems, cells, heredity, and ecosystems.', 'https://images.unsplash.com/photo-1530026405186-ed1f139313f8?w=150')
ON CONFLICT (slug) DO UPDATE SET
 name = EXCLUDED.name, description = EXCLUDED.description, icon_url = EXCLUDED.icon_url, updated_at = now();

INSERT INTO "lessons" (id, title, description, difficulty, base_xp, position, category_id, reflection_prompt) VALUES
('24000000-0000-0000-0000-000000000001', 'Algebraic Expressions', $$# Algebraic Expressions

## Start here
An **expression** is a mathematical phrase made from numbers, variables, and operations. It has no equals sign. In `4x + 7`, `x` is a variable, `4` is its coefficient, and `7` is a constant.

## Build the idea
A **term** is a part separated by `+` or `-`. In `3a + 2a - 5`, the terms are `3a`, `2a`, and `-5`. Terms can be combined only when they are **like terms**: they have the same variable part.

For example, `3a + 2a - 5 = 5a - 5`. We add the coefficients 3 and 2, but the constant stays separate. `3a + 2b` cannot become `5ab` because `a` and `b` are different variables.

## Example
Simplify `6m - 2 + 3m + 5`:

`6m + 3m - 2 + 5 = 9m + 3`

## Takeaway
Identify each term first, then combine only like terms. This skill prepares you to solve equations and model real situations.$$, 'EASY', 60, 1, (SELECT id FROM "categories" WHERE slug = 'mathematics'), 'Which part of simplifying expressions feels most useful to practice again?'),
('24000000-0000-0000-0000-000000000002', 'Linear Equations', $$# Linear Equations

## Start here
An equation says that two expressions have the same value. A **linear equation** has a variable to the first power, such as `3x + 4 = 19`.

## Keep the balance
Think of the equals sign as a balanced scale. Whatever operation you perform on one side must also be performed on the other side. Use inverse operations to isolate the unknown.

To solve `3x + 4 = 19`, subtract 4 from both sides: `3x = 15`. Then divide both sides by 3: `x = 5`.

## Check your work
Put the answer back into the original equation: `3(5) + 4 = 19`. Since `19 = 19`, the solution works.

## Takeaway
Undo addition or subtraction first, then undo multiplication or division. A quick substitution check catches mistakes.$$, 'EASY', 70, 2, (SELECT id FROM "categories" WHERE slug = 'mathematics'), 'Where might solving for an unknown help in a real situation?'),
('24000000-0000-0000-0000-000000000003', 'Linear Inequalities', $$# Linear Inequalities

## Start here
Inequalities compare values instead of saying they are exactly equal. The symbols are `>`, `<`, `≥`, and `≤`. For example, `x > 4` means every number greater than 4.

## Solve them like equations
You can add, subtract, multiply, or divide both sides. There is one important rule: when you multiply or divide by a negative number, reverse the inequality sign.

Example: `-2x > 8`. Divide by `-2`, reverse the sign, and get `x < -4`.

## Number lines
Use an open circle for `<` or `>` because the endpoint is excluded. Use a filled circle for `≤` or `≥` because it is included. Shade toward values that make the statement true.

## Takeaway
Read the symbol carefully and remember the negative-number rule. The solution is usually a set of values, not one number.$$, 'EASY', 80, 3, (SELECT id FROM "categories" WHERE slug = 'mathematics'), 'Which inequality symbol do you want to remember more confidently?'),
('24000000-0000-0000-0000-000000000004', 'Functions & Graphs', $$# Functions & Graphs

## Start here
A **function** matches each input with exactly one output. We often write `f(x)`, read as “f of x.” If `f(x) = 2x + 1`, then `f(3) = 2(3) + 1 = 7`.

## Inputs and outputs
The input is the value of `x`; the output is the resulting `y`. A coordinate such as `(3, 7)` records input 3 and output 7.

## Linear graphs
For `y = 2x + 1`, the number 2 is the slope: each step right moves the graph two steps up. The `+1` is the y-intercept, where the line crosses the vertical axis.

Make a small table: for `x = 0, 1, 2`, the outputs are `1, 3, 5`. Plot `(0,1)`, `(1,3)`, and `(2,5)`, then draw a straight line.

## Takeaway
Functions connect inputs to outputs. Tables, equations, and graphs are three ways to describe the same relationship.$$, 'MEDIUM', 100, 4, (SELECT id FROM "categories" WHERE slug = 'mathematics'), 'How does a graph help you understand an equation differently?'),
('24000000-0000-0000-0000-000000000005', 'Quadratic Equations', $$# Quadratic Equations

## Start here
A quadratic equation includes a squared variable. Its standard form is `ax² + bx + c = 0`, where `a` is not zero.

## Factor to solve
For simple quadratics, factorisation finds values that make the equation true. In `x² + 5x + 6 = 0`, look for two numbers that multiply to 6 and add to 5: 2 and 3.

So `(x + 2)(x + 3) = 0`. If a product is zero, at least one factor is zero. Therefore `x = -2` or `x = -3`. These solutions are called **roots**.

## Check
Substitute `-2`: `(-2)² + 5(-2) + 6 = 4 - 10 + 6 = 0`.

## Takeaway
Put the equation in standard form, factor when possible, then set each factor equal to zero. Quadratics often have two roots.$$, 'MEDIUM', 120, 5, (SELECT id FROM "categories" WHERE slug = 'mathematics'), 'What clue tells you that an equation is quadratic?'),
('24000000-0000-0000-0000-000000000006', 'Pythagorean Theorem', $$# Pythagorean Theorem

## Start here
The Pythagorean theorem works only for a **right triangle**, a triangle with a 90° angle. Its longest side, opposite the right angle, is the **hypotenuse**.

## The formula
`a² + b² = c²`

Here `a` and `b` are the shorter sides and `c` is the hypotenuse. Always identify `c` before calculating.

## Example
A right triangle has sides 3 cm and 4 cm around the right angle. Find the hypotenuse:

`c² = 3² + 4² = 9 + 16 = 25`, so `c = 5 cm`.

This is useful for finding a ladder length, a diagonal path, or the distance across a rectangular field.

## Takeaway
Square the two shorter sides, add them, then take the square root. Check that the triangle is right-angled first.$$, 'MEDIUM', 150, 6, (SELECT id FROM "categories" WHERE slug = 'mathematics'), 'Where could you use a right-triangle calculation outside the classroom?'),
('24000000-0000-0000-0000-000000000007', 'Measurement & SI Units', $$# Measurement & SI Units

## Start here
Physics describes the world using **physical quantities** such as length, mass, time, and temperature. A measurement needs both a number and a unit.

## SI units
The International System of Units (SI) provides shared standards. Length is measured in metres (m), mass in kilograms (kg), time in seconds (s), and temperature in kelvin (K). In school science, degrees Celsius is also common for temperature.

## Convert carefully
Prefixes show powers of ten. `1 kilometre = 1000 metres`, and `1 centimetre = 0.01 metre`. To change 2.5 km to metres, multiply by 1000: `2500 m`.

## Good measurement
Choose a suitable tool and unit. A ruler is appropriate for a book’s length; a stopwatch is appropriate for time. Record units every time.

## Takeaway
Units give numbers meaning. Convert before comparing or using a formula.$$, 'EASY', 60, 1, (SELECT id FROM "categories" WHERE slug = 'physics'), 'Which SI unit do you use most often in daily life?'),
('24000000-0000-0000-0000-000000000008', 'Speed, Distance & Time', $$# Speed, Distance & Time

## Start here
Speed tells us how quickly distance changes. The key relationship is:

`Speed = Distance / Time`

Use metres and seconds to get metres per second (m/s).

## Rearrange the formula
If you know speed and time, `Distance = Speed × Time`. If you know distance and speed, `Time = Distance / Speed`.

## Example
A cyclist travels 120 m in 20 s. `Speed = 120 / 20 = 6 m/s`.

For an average speed, divide the total distance by the total time. Do not average separate speeds unless the time intervals are equal.

## Takeaway
Write the known quantities with their units, choose the rearranged formula, and check that the answer’s unit makes sense.$$, 'EASY', 70, 2, (SELECT id FROM "categories" WHERE slug = 'physics'), 'When have you noticed speed affecting your everyday travel?'),
('24000000-0000-0000-0000-000000000009', 'Velocity & Acceleration', $$# Velocity & Acceleration

## Start here
**Velocity** is speed with direction. A car moving at 15 m/s east has a velocity; saying only 15 m/s gives its speed.

## Changing motion
**Acceleration** is the rate at which velocity changes. An object accelerates when it speeds up, slows down, or changes direction.

The relationship is `acceleration = change in velocity / time`. Its SI unit is m/s².

## Example
A runner changes velocity from 2 m/s to 8 m/s in 3 s. The change is 6 m/s, so acceleration is `6 / 3 = 2 m/s²`.

Braking is negative acceleration because velocity decreases. Turning around also changes velocity because direction changes.

## Takeaway
Remember: speed has size only; velocity has size and direction. Acceleration measures any change in velocity.$$, 'MEDIUM', 80, 3, (SELECT id FROM "categories" WHERE slug = 'physics'), 'How can an object accelerate even if its speed stays the same?'),
('24000000-0000-0000-0000-000000000010', 'Forces & Newton''s Laws', $$# Forces & Newton''s Laws

## Start here
A **force** is a push or pull. Forces can change motion or shape. The SI unit of force is the newton (N).

## Net force
Balanced forces give zero net force, so an object stays at rest or moves at constant velocity. Unbalanced forces cause acceleration.

Newton’s second law connects force, mass, and acceleration:

`F = ma`

For a 2 kg object accelerating at 3 m/s², `F = 2 × 3 = 6 N`.

Newton’s first law explains inertia: objects resist changes in motion. Newton’s third law says forces come in equal and opposite pairs, such as a foot pushing the ground while the ground pushes back.

## Takeaway
Draw or imagine all forces, find the net force, and use `F = ma` when mass and acceleration are known.$$, 'MEDIUM', 100, 4, (SELECT id FROM "categories" WHERE slug = 'physics'), 'Which example of action and reaction do you notice in everyday movement?'),
('24000000-0000-0000-0000-000000000011', 'Work, Energy & Power', $$# Work, Energy & Power

## Start here
In physics, **work** happens when a force moves an object in the force’s direction. Work is measured in joules (J).

## Energy forms
Energy is the ability to do work. **Kinetic energy** is energy of motion; **potential energy** is stored energy, such as an object held above the ground.

Power tells us how quickly energy is transferred or work is done: `Power = Work / Time`. Its unit is the watt (W), where one watt equals one joule per second.

## Example
If a machine does 200 J of work in 10 s, its power is `200 / 10 = 20 W`.

## Takeaway
Work and energy use joules. Power compares how fast the work happens, not just how much work happens.$$, 'MEDIUM', 120, 5, (SELECT id FROM "categories" WHERE slug = 'physics'), 'Where do you see energy changing form in a daily activity?'),
('24000000-0000-0000-0000-000000000012', 'Electricity Basics', $$# Electricity Basics

## Start here
Electric current is the flow of electric charge through a circuit. Current is measured in amperes (A), voltage in volts (V), and resistance in ohms (Ω).

## A complete circuit
A simple circuit needs a source of energy, a conducting path, and a component such as a lamp. If the path is broken, charge cannot flow and the lamp will not light.

## Ohm’s law
Ohm’s law links voltage, current, and resistance:

`V = IR`

If a 2 A current passes through a 3 Ω resistor, the voltage is `2 × 3 = 6 V`.

Greater resistance makes current harder to flow for the same voltage. Use electrical equipment safely and never experiment with mains electricity.

## Takeaway
Know the three quantities and their units. A complete path is essential for current to flow.$$, 'MEDIUM', 150, 6, (SELECT id FROM "categories" WHERE slug = 'physics'), 'What household device helps you remember that circuits need a complete path?'),
('24000000-0000-0000-0000-000000000013', 'Characteristics of Living Things', $$# Characteristics of Living Things

## Start here
Living organisms share a set of life processes. A useful memory guide is that living things carry out nutrition, respiration, response, growth, reproduction, and excretion; they also move in some way.

## What the words mean
**Nutrition** provides materials and energy. **Respiration** releases usable energy from food. **Response** is reacting to a change in the environment, such as a plant growing toward light.

**Growth** is a permanent increase in size or cell number. **Reproduction** produces new organisms. **Excretion** removes waste products made by cells.

## Example
A seedling uses nutrients and water, grows roots and leaves, and bends toward light. These observations show several life processes at once.

## Takeaway
No single feature proves something is alive. Look for the pattern of life processes together.$$, 'EASY', 60, 1, (SELECT id FROM "categories" WHERE slug = 'biology'), 'Which life process is easiest for you to observe around you?'),
('24000000-0000-0000-0000-000000000014', 'Cell Structure & Function', $$# Cell Structure & Function

## Start here
Cells are the basic units of living things. Although cells vary in shape and job, many share important structures.

## Key structures
The **cell membrane** controls what enters and leaves. **Cytoplasm** is the jelly-like region where many reactions happen. The **nucleus** contains genetic material and helps control the cell.

**Mitochondria** release energy through respiration. **Ribosomes** build proteins needed for growth and repair.

## Think of a cell as a workplace
The membrane is like a security gate, the nucleus is an information centre, mitochondria are energy stations, and ribosomes are protein builders. The comparison is not exact, but it helps you remember jobs.

## Takeaway
Learn a structure and its function together. In biology, form supports function.$$, 'EASY', 70, 2, (SELECT id FROM "categories" WHERE slug = 'biology'), 'Which cell structure’s job is easiest for you to remember, and why?'),
('24000000-0000-0000-0000-000000000015', 'Plant vs Animal Cells', $$# Plant vs Animal Cells

## Start here
Plant and animal cells are both eukaryotic cells, so they share a cell membrane, cytoplasm, nucleus, mitochondria, and ribosomes.

## Plant-cell features
Plant cells also have a **cell wall** that supports the cell, **chloroplasts** that absorb light for photosynthesis, and a large permanent **vacuole** filled with cell sap.

Animal cells do not have a cell wall or chloroplasts. Their shapes are often more flexible because they only have a cell membrane.

## Compare clearly
If you see chloroplasts, it is a plant cell. If you see a thick cell wall and a large vacuole, that is also strong evidence for a plant cell.

## Takeaway
Start with shared structures, then use cell wall, chloroplasts, and vacuole to identify a plant cell.$$, 'EASY', 80, 3, (SELECT id FROM "categories" WHERE slug = 'biology'), 'Which feature makes a plant cell easiest to recognise in a diagram?'),
('24000000-0000-0000-0000-000000000016', 'Human Body Systems', $$# Human Body Systems

## Start here
Body systems are groups of organs that work together. They are connected: no system works completely alone.

## Four important systems
The **digestive system** breaks food into small molecules that can be absorbed. The **respiratory system** exchanges gases, taking in oxygen and removing carbon dioxide.

The **circulatory system** moves blood, oxygen, nutrients, and wastes around the body. The **nervous system** detects information and coordinates fast responses using the brain, spinal cord, and nerves.

## Working together
During exercise, the respiratory system supplies more oxygen, the circulatory system delivers it to muscles, and the nervous system coordinates movement.

## Takeaway
Learn each system’s main job, then look for how systems cooperate to keep the body alive.$$, 'MEDIUM', 100, 4, (SELECT id FROM "categories" WHERE slug = 'biology'), 'Which two body systems do you think work together most visibly during exercise?'),
('24000000-0000-0000-0000-000000000017', 'DNA, Genes & Heredity', $$# DNA, Genes & Heredity

## Start here
DNA is a molecule that stores biological instructions. It is organised into **genes**, which are sections of DNA that can influence characteristics.

## From DNA to traits
DNA is packaged into **chromosomes** in the nucleus of most cells. You inherit chromosomes from your parents, so you may inherit versions of genes that influence traits such as blood group.

Traits are not always controlled by one gene. Environment can also affect how some traits develop. **Variation** means individuals in a population are not identical; it can come from inherited differences and environmental effects.

## Example
Siblings share parents but are not identical because each receives a different combination of genetic information.

## Takeaway
DNA contains genes; genes are carried on chromosomes; inherited gene combinations contribute to variation.$$, 'MEDIUM', 120, 5, (SELECT id FROM "categories" WHERE slug = 'biology'), 'What is one difference between a gene and a chromosome that you want to remember?'),
('24000000-0000-0000-0000-000000000018', 'Ecosystems & Food Chains', $$# Ecosystems & Food Chains

## Start here
An **ecosystem** includes living organisms and their non-living environment, such as soil, water, light, and temperature.

## Roles in a food chain
**Producers** make their own food, usually using sunlight. **Consumers** get energy by eating other organisms. **Decomposers** break down dead material and return nutrients to the environment.

A simple chain is: grass → grasshopper → frog → snake. The arrows show the direction of energy transfer: from the food to the eater.

## Energy decreases
Only part of the energy at one level becomes biomass for the next level. Much is used for life processes or lost as heat, so food chains usually have fewer organisms at higher levels.

## Takeaway
Trace arrows as energy flow, identify each organism’s role, and remember that decomposers recycle materials.$$, 'MEDIUM', 150, 6, (SELECT id FROM "categories" WHERE slug = 'biology'), 'How might removing one organism affect a food chain?')
ON CONFLICT (id) DO UPDATE SET
 title = EXCLUDED.title, description = EXCLUDED.description, difficulty = EXCLUDED.difficulty,
 base_xp = EXCLUDED.base_xp, position = EXCLUDED.position, category_id = EXCLUDED.category_id,
 reflection_prompt = EXCLUDED.reflection_prompt, updated_at = now();

INSERT INTO "exercises" (id, lesson_id, question, answer_key, difficulty, points, type, options, hint, explanation) VALUES
-- Mathematics 1: Algebraic Expressions
('25000000-0000-0000-0000-000000000001','24000000-0000-0000-0000-000000000001','In `4x + 7`, what is the coefficient of x?','4','EASY',10,'MULTIPLE_CHOICE','["4","7","x","11"]','Look for the number multiplying x.','The coefficient is the number that multiplies a variable, so it is 4.'),
('25000000-0000-0000-0000-000000000002','24000000-0000-0000-0000-000000000001','The terms 3a and 2a are like terms.','True','EASY',10,'TRUE_FALSE','["True","False"]','Compare the variable part of both terms.','Both terms contain a, so their coefficients can be combined.'),
('25000000-0000-0000-0000-000000000003','24000000-0000-0000-0000-000000000001','Simplify `5m + 2m`.','7m','EASY',15,'TEXT','[]','Add the coefficients because both terms use m.','5m and 2m are like terms, so 5 + 2 gives 7m.'),
-- Mathematics 2: Linear Equations
('25000000-0000-0000-0000-000000000004','24000000-0000-0000-0000-000000000002','Solve: `x + 6 = 14`.','8','EASY',15,'TEXT','[]','Undo the addition of 6.','Subtracting 6 from both sides gives x = 8.'),
('25000000-0000-0000-0000-000000000005','24000000-0000-0000-0000-000000000002','Which operation isolates x in `3x = 15`?','divide by 3','EASY',10,'MULTIPLE_CHOICE','["add 3","subtract 3","divide by 3","multiply by 15"]','x is multiplied by 3. Use the inverse operation.','Dividing both sides by 3 leaves x alone, so x = 5.'),
('25000000-0000-0000-0000-000000000006','24000000-0000-0000-0000-000000000002','You must do the same operation to both sides of an equation.','True','EASY',10,'TRUE_FALSE','["True","False"]','Think of an equation as balanced.','Doing the same operation to both sides keeps the equation balanced.'),
-- Mathematics 3: Linear Inequalities
('25000000-0000-0000-0000-000000000007','24000000-0000-0000-0000-000000000003','Which symbol means “greater than or equal to”?','≥','EASY',10,'MULTIPLE_CHOICE','[">","<","≥","≤"]','Look for a greater-than sign with a line underneath.','≥ includes values greater than the boundary and the boundary itself.'),
('25000000-0000-0000-0000-000000000008','24000000-0000-0000-0000-000000000003','Multiplying an inequality by a negative number reverses its sign.','True','EASY',15,'TRUE_FALSE','["True","False"]','Test what happens to the order of -1 and 1 after multiplying by -1.','A negative multiplication reverses the order, so the inequality sign must reverse.'),
('25000000-0000-0000-0000-000000000009','24000000-0000-0000-0000-000000000003','Solve: `x - 3 > 4`.','x > 7','EASY',10,'TEXT','[]','Add 3 to both sides.','Adding 3 gives x > 7.'),
-- Mathematics 4: Functions & Graphs
('25000000-0000-0000-0000-000000000010','24000000-0000-0000-0000-000000000004','If `f(x) = 2x + 1`, what is `f(3)`?','7','MEDIUM',15,'TEXT','[]','Substitute 3 for x before calculating.','f(3) = 2(3) + 1 = 7.'),
('25000000-0000-0000-0000-000000000011','24000000-0000-0000-0000-000000000004','In the coordinate `(2, 5)`, what is the y-value?','5','MEDIUM',10,'MULTIPLE_CHOICE','["2","5","7","x"]','Coordinates are written as (x, y).','The second coordinate is y, so the y-value is 5.'),
('25000000-0000-0000-0000-000000000012','24000000-0000-0000-0000-000000000004','A function gives exactly one output for each input.','True','MEDIUM',10,'TRUE_FALSE','["True","False"]','Recall the definition of a function.','An input cannot be paired with two different outputs in a function.'),
-- Mathematics 5: Quadratic Equations
('25000000-0000-0000-0000-000000000013','24000000-0000-0000-0000-000000000005','Which equation is quadratic?','x² + 3x + 2 = 0','MEDIUM',10,'MULTIPLE_CHOICE','["2x + 3 = 0","x² + 3x + 2 = 0","x + y = 4","5 = 5"]','Look for a variable squared.','A quadratic equation includes an x² term.'),
('25000000-0000-0000-0000-000000000014','24000000-0000-0000-0000-000000000005','Solve: `(x + 2)(x + 3) = 0`. Give the smaller root.','-3','MEDIUM',15,'TEXT','[]','Set each factor equal to zero.','x + 3 = 0 gives x = -3; the other root is -2.'),
('25000000-0000-0000-0000-000000000015','24000000-0000-0000-0000-000000000005','A quadratic equation can have two roots.','True','MEDIUM',10,'TRUE_FALSE','["True","False"]','Think about two factors becoming zero.','Many quadratics have two solutions, called roots.'),
-- Mathematics 6: Pythagorean Theorem
('25000000-0000-0000-0000-000000000016','24000000-0000-0000-0000-000000000006','Which side is c in `a² + b² = c²`?','hypotenuse','MEDIUM',10,'TEXT','[]','It is the longest side opposite the right angle.','c represents the hypotenuse of a right triangle.'),
('25000000-0000-0000-0000-000000000017','24000000-0000-0000-0000-000000000006','A right triangle has shorter sides 3 and 4. What is its hypotenuse?','5','MEDIUM',20,'MULTIPLE_CHOICE','["5","7","12","25"]','Calculate 3² + 4², then take the square root.','3² + 4² = 9 + 16 = 25, and √25 = 5.'),
('25000000-0000-0000-0000-000000000018','24000000-0000-0000-0000-000000000006','The Pythagorean theorem applies to every triangle.','False','MEDIUM',10,'TRUE_FALSE','["True","False"]','Check the condition in the theorem’s name and formula.','It applies only to right triangles.'),
-- Physics 1: Measurement & SI Units
('25000000-0000-0000-0000-000000000019','24000000-0000-0000-0000-000000000007','What is the SI unit of length?','metre','EASY',10,'TEXT','[]','Use the base SI unit, not centimetre or kilometre.','The metre (m) is the SI base unit for length.'),
('25000000-0000-0000-0000-000000000020','24000000-0000-0000-0000-000000000007','How many metres are in 2 kilometres?','2000','EASY',15,'MULTIPLE_CHOICE','["20","200","2000","20000"]','One kilometre equals 1000 metres.','2 × 1000 = 2000 metres.'),
('25000000-0000-0000-0000-000000000021','24000000-0000-0000-0000-000000000007','A measurement should include a number and a unit.','True','EASY',10,'TRUE_FALSE','["True","False"]','Ask whether “5” alone tells you what was measured.','A number needs its unit to communicate a physical quantity.'),
-- Physics 2: Speed, Distance & Time
('25000000-0000-0000-0000-000000000022','24000000-0000-0000-0000-000000000008','A cyclist travels 120 m in 20 s. What is the average speed?','6 m/s','EASY',20,'TEXT','[]','Use speed = distance ÷ time.','120 ÷ 20 = 6, so the average speed is 6 m/s.'),
('25000000-0000-0000-0000-000000000023','24000000-0000-0000-0000-000000000008','Which formula finds distance?','speed × time','EASY',10,'MULTIPLE_CHOICE','["speed ÷ time","speed × time","time ÷ distance","distance ÷ speed"]','Rearrange speed = distance ÷ time.','Multiplying speed by time gives distance.'),
('25000000-0000-0000-0000-000000000024','24000000-0000-0000-0000-000000000008','Average speed uses total distance divided by total time.','True','EASY',10,'TRUE_FALSE','["True","False"]','Use totals for the whole trip.','Average speed is total distance ÷ total time.'),
-- Physics 3: Velocity & Acceleration
('25000000-0000-0000-0000-000000000025','24000000-0000-0000-0000-000000000009','What does velocity include that speed does not?','direction','MEDIUM',10,'MULTIPLE_CHOICE','["mass","direction","time","temperature"]','Think of “10 m/s east” compared with “10 m/s.”','Velocity includes both speed and direction.'),
('25000000-0000-0000-0000-000000000026','24000000-0000-0000-0000-000000000009','What is the SI unit of acceleration?','m/s²','MEDIUM',15,'TEXT','[]','It describes a change in m/s each second.','Acceleration is measured in metres per second squared, m/s².'),
('25000000-0000-0000-0000-000000000027','24000000-0000-0000-0000-000000000009','Changing direction means an object is accelerating.','True','MEDIUM',10,'TRUE_FALSE','["True","False"]','Velocity changes when either speed or direction changes.','A change in direction changes velocity, so it is acceleration.'),
-- Physics 4: Forces & Newton's Laws
('25000000-0000-0000-0000-000000000028','24000000-0000-0000-0000-000000000010','What is the SI unit of force?','newton','MEDIUM',10,'TEXT','[]','Its symbol is N.','Force is measured in newtons (N).'),
('25000000-0000-0000-0000-000000000029','24000000-0000-0000-0000-000000000010','A 2 kg object accelerates at 3 m/s². What force acts on it?','6 N','MEDIUM',20,'MULTIPLE_CHOICE','["1.5 N","5 N","6 N","9 N"]','Use F = ma.','F = 2 × 3 = 6 N.'),
('25000000-0000-0000-0000-000000000030','24000000-0000-0000-0000-000000000010','Balanced forces produce zero net force.','True','MEDIUM',10,'TRUE_FALSE','["True","False"]','Balanced means the pushes and pulls cancel.','Balanced forces cancel, so there is no net force.'),
-- Physics 5: Work, Energy & Power
('25000000-0000-0000-0000-000000000031','24000000-0000-0000-0000-000000000011','What is the unit of energy?','joule','MEDIUM',10,'MULTIPLE_CHOICE','["watt","newton","joule","volt"]','It has the symbol J.','Energy and work are measured in joules.'),
('25000000-0000-0000-0000-000000000032','24000000-0000-0000-0000-000000000011','A machine does 200 J of work in 10 s. What is its power?','20 W','MEDIUM',20,'TEXT','[]','Use power = work ÷ time.','200 ÷ 10 = 20, so the power is 20 W.'),
('25000000-0000-0000-0000-000000000033','24000000-0000-0000-0000-000000000011','Kinetic energy is the energy of motion.','True','MEDIUM',10,'TRUE_FALSE','["True","False"]','Think of a moving bicycle.','A moving object has kinetic energy.'),
-- Physics 6: Electricity Basics
('25000000-0000-0000-0000-000000000034','24000000-0000-0000-0000-000000000012','Which quantity is measured in volts?','voltage','MEDIUM',10,'MULTIPLE_CHOICE','["current","voltage","resistance","power"]','The unit V gives its name.','Voltage is measured in volts (V).'),
('25000000-0000-0000-0000-000000000035','24000000-0000-0000-0000-000000000012','What is the resistance if V = 6 V and I = 2 A?','3 Ω','MEDIUM',20,'TEXT','[]','Rearrange V = IR to R = V ÷ I.','R = 6 ÷ 2 = 3 Ω.'),
('25000000-0000-0000-0000-000000000036','24000000-0000-0000-0000-000000000012','A broken circuit can still light a lamp.','False','MEDIUM',10,'TRUE_FALSE','["True","False"]','Current needs a complete path.','A broken path stops current, so the lamp cannot light.'),
-- Biology 1: Characteristics of Living Things
('25000000-0000-0000-0000-000000000037','24000000-0000-0000-0000-000000000013','Which process releases usable energy from food?','respiration','EASY',10,'TEXT','[]','It happens in cells and is not the same as breathing.','Respiration releases usable energy from food in cells.'),
('25000000-0000-0000-0000-000000000038','24000000-0000-0000-0000-000000000013','Which is a life process?','reproduction','EASY',10,'MULTIPLE_CHOICE','["rusting","reproduction","evaporation","melting"]','Choose something living organisms do to continue their species.','Reproduction is a life process that produces new organisms.'),
('25000000-0000-0000-0000-000000000039','24000000-0000-0000-0000-000000000013','Plants can respond to light.','True','EASY',15,'TRUE_FALSE','["True","False"]','Think about shoots bending toward a window.','Plants respond to light by growing toward it.'),
-- Biology 2: Cell Structure & Function
('25000000-0000-0000-0000-000000000040','24000000-0000-0000-0000-000000000014','Which structure controls what enters and leaves a cell?','cell membrane','EASY',10,'MULTIPLE_CHOICE','["nucleus","cell membrane","ribosome","mitochondrion"]','It forms the cell’s outer boundary.','The cell membrane controls movement of substances into and out of the cell.'),
('25000000-0000-0000-0000-000000000041','24000000-0000-0000-0000-000000000014','Which structure contains genetic material and helps control the cell?','nucleus','EASY',15,'TEXT','[]','It is often shown as a large round structure.','The nucleus contains genetic material and controls many cell activities.'),
('25000000-0000-0000-0000-000000000042','24000000-0000-0000-0000-000000000014','Ribosomes build proteins.','True','EASY',10,'TRUE_FALSE','["True","False"]','Recall the cell’s protein builders.','Ribosomes are the structures where proteins are made.'),
-- Biology 3: Plant vs Animal Cells
('25000000-0000-0000-0000-000000000043','24000000-0000-0000-0000-000000000015','Which structure is found in plant cells but not animal cells?','chloroplast','EASY',10,'TEXT','[]','It captures light for photosynthesis.','Chloroplasts are plant-cell structures used in photosynthesis.'),
('25000000-0000-0000-0000-000000000044','24000000-0000-0000-0000-000000000015','Which structure gives plant cells extra support?','cell wall','EASY',10,'MULTIPLE_CHOICE','["cell membrane","cell wall","nucleus","cytoplasm"]','It is outside the cell membrane.','The rigid cell wall supports and protects plant cells.'),
('25000000-0000-0000-0000-000000000045','24000000-0000-0000-0000-000000000015','Animal cells have chloroplasts.','False','EASY',15,'TRUE_FALSE','["True","False"]','Chloroplasts are for photosynthesis in plants.','Animal cells do not contain chloroplasts.'),
-- Biology 4: Human Body Systems
('25000000-0000-0000-0000-000000000046','24000000-0000-0000-0000-000000000016','Which system transports blood around the body?','circulatory system','MEDIUM',10,'MULTIPLE_CHOICE','["digestive system","circulatory system","respiratory system","nervous system"]','It includes the heart and blood vessels.','The circulatory system moves blood, oxygen, nutrients, and wastes.'),
('25000000-0000-0000-0000-000000000047','24000000-0000-0000-0000-000000000016','Which system exchanges oxygen and carbon dioxide?','respiratory system','MEDIUM',15,'TEXT','[]','It includes the lungs.','The respiratory system takes in oxygen and removes carbon dioxide.'),
('25000000-0000-0000-0000-000000000048','24000000-0000-0000-0000-000000000016','The nervous system helps coordinate quick responses.','True','MEDIUM',10,'TRUE_FALSE','["True","False"]','Think of the brain, spinal cord, and nerves.','The nervous system receives information and coordinates responses.'),
-- Biology 5: DNA, Genes & Heredity
('25000000-0000-0000-0000-000000000049','24000000-0000-0000-0000-000000000017','What molecule stores genetic instructions?','DNA','MEDIUM',10,'TEXT','[]','It is the abbreviation used in genetics.','DNA stores the instructions used by living organisms.'),
('25000000-0000-0000-0000-000000000050','24000000-0000-0000-0000-000000000017','Genes are sections of which molecule?','DNA','MEDIUM',10,'MULTIPLE_CHOICE','["water","DNA","protein","glucose"]','Genes are part of the genetic instruction molecule.','A gene is a section of DNA.'),
('25000000-0000-0000-0000-000000000051','24000000-0000-0000-0000-000000000017','Variation means individuals in a population are not identical.','True','MEDIUM',15,'TRUE_FALSE','["True","False"]','Compare siblings: they share parents but differ.','Variation describes differences between individuals of the same species.'),
-- Biology 6: Ecosystems & Food Chains
('25000000-0000-0000-0000-000000000052','24000000-0000-0000-0000-000000000018','Which organism makes its own food using sunlight?','producer','MEDIUM',10,'MULTIPLE_CHOICE','["consumer","producer","decomposer","predator"]','Think of plants at the start of many food chains.','Producers, such as green plants, make their own food using light.'),
('25000000-0000-0000-0000-000000000053','24000000-0000-0000-0000-000000000018','What do decomposers break down?','dead material','MEDIUM',15,'TEXT','[]','Think of fallen leaves and dead organisms.','Decomposers break down dead material and recycle nutrients.'),
('25000000-0000-0000-0000-000000000054','24000000-0000-0000-0000-000000000018','Food-chain arrows show the direction of energy transfer.','True','MEDIUM',10,'TRUE_FALSE','["True","False"]','Read an arrow from food to eater.','The arrows show energy moving from the organism eaten to the eater.')
ON CONFLICT (id) DO UPDATE SET
 lesson_id = EXCLUDED.lesson_id, question = EXCLUDED.question, answer_key = EXCLUDED.answer_key,
 difficulty = EXCLUDED.difficulty, points = EXCLUDED.points, type = EXCLUDED.type, options = EXCLUDED.options,
 hint = EXCLUDED.hint, explanation = EXCLUDED.explanation, updated_at = now();
