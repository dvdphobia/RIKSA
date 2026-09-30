-- Older demo posts stored escaped newline sequences as visible text. Convert
-- them to real line breaks so every client receives valid Markdown.
UPDATE "posts"
SET content = replace(replace(content, '\r\n', E'\n'), '\n', E'\n'),
    updated_at = now()
WHERE content LIKE '%\n%' OR content LIKE '%\r\n%';

-- Give the mathematics demo post explicit Markdown structure and protect
-- equations from emphasis parsing by rendering them as inline code.
UPDATE "posts"
SET content = $markdown$
សួស្តីអ្នកទាំងអស់គ្នា! ថ្ងៃនេះខ្ញុំសូមចែករំលែកវិធីសាស្ត្រងាយៗក្នុងការដោះស្រាយសមីការឌីផេរ៉ង់ស្យែលលំដាប់ទី២ អូម៉ូសែន៖

`ay'' + by' + cy = 0`

### ជំហានទី១៖ រកសមីការសម្គាល់

`ar^2 + br + c = 0`

### ជំហានទី២៖ គណនា Delta

`Delta = b^2 - 4ac`

- បើ `Delta > 0`: `y = c1*e^(r1*x) + c2*e^(r2*x)`
- បើ `Delta = 0`: `y = (c1 + c2*x)*e^(r*x)`
- បើ `Delta < 0`: `y = e^(alpha*x)*(c1*cos(beta*x) + c2*sin(beta*x))`

សង្ឃឹមថាជួយបានច្រើនសម្រាប់ការប្រឡងខាងមុខ!
$markdown$,
    updated_at = now()
WHERE id = 'f0000000-0000-0000-0000-000000000001';
