package com.riksa.admin.service;

import com.riksa.admin.dto.ExerciseRequest;
import com.riksa.admin.dto.LessonCreateRequest;
import com.riksa.common.enums.DifficultyLevel;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Parses a lesson markdown file with YAML frontmatter into a {@link LessonCreateRequest}.
 *
 * <pre>
 * ---
 * title: Introduction to Variables
 * difficulty: EASY
 * baseXp: 10
 * position: 1
 * categoryId: 550e8400-e29b-41d4-a716-446655440000
 * reflectionPrompt: What surprised you?
 * exercises:
 *   - question: What is a variable?
 *     answerKey: A named storage location
 *     difficulty: EASY
 *     points: 5
 *     type: MULTIPLE_CHOICE
 *     options: [A, B, C]
 *     hint: Think about storage.
 *     explanation: Variables keep values for later use.
 * ---
 *
 * Lesson body in markdown becomes the description.
 * </pre>
 */
@Component
public class LessonMarkdownParser {

    private static final String DELIMITER = "---";

    public LessonCreateRequest parse(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            throw new IllegalArgumentException("Markdown content is empty");
        }

        String[] lines = markdown.replace("\r\n", "\n").stripLeading().split("\n", -1);
        if (!DELIMITER.equals(lines[0].trim())) {
            throw new IllegalArgumentException("Markdown must start with YAML frontmatter (---)");
        }

        int closing = -1;
        for (int i = 1; i < lines.length; i++) {
            if (DELIMITER.equals(lines[i].trim())) {
                closing = i;
                break;
            }
        }
        if (closing == -1) {
            throw new IllegalArgumentException("Unterminated YAML frontmatter (missing closing ---)");
        }

        String yamlBlock = String.join("\n", Arrays.copyOfRange(lines, 1, closing));
        String body = String.join("\n", Arrays.copyOfRange(lines, closing + 1, lines.length)).trim();

        Map<String, Object> meta = parseYaml(yamlBlock);

        LessonCreateRequest req = new LessonCreateRequest();
        req.setTitle(requireString(meta, "title"));
        req.setDescription(body.isEmpty() ? null : body);
        req.setDifficulty(parseDifficulty(meta.get("difficulty"), DifficultyLevel.EASY));
        req.setBaseXp(asInt(meta.get("baseXp"), 10));
        req.setPosition(meta.containsKey("position") ? asInt(meta.get("position"), 1) : null);
        req.setCategoryId(parseCategoryId(meta.get("categoryId")));
        req.setReflectionPrompt(asString(meta.get("reflectionPrompt")));
        req.setExercises(parseExercises(meta.get("exercises")));
        return req;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseYaml(String yamlBlock) {
        Object parsed;
        try {
            parsed = new Yaml(new SafeConstructor(new LoaderOptions())).load(yamlBlock);
        } catch (YAMLException e) {
            throw new IllegalArgumentException("Invalid YAML frontmatter: " + e.getMessage());
        }
        if (!(parsed instanceof Map)) {
            throw new IllegalArgumentException("Frontmatter must be a YAML mapping of key/value pairs");
        }
        return (Map<String, Object>) parsed;
    }

    @SuppressWarnings("unchecked")
    private List<ExerciseRequest> parseExercises(Object raw) {
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            throw new IllegalArgumentException("Frontmatter must contain at least one entry under 'exercises'");
        }
        List<ExerciseRequest> exercises = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (!(item instanceof Map)) {
                throw new IllegalArgumentException("Exercise #" + (i + 1) + " must be a YAML mapping");
            }
            Map<String, Object> map = (Map<String, Object>) item;
            ExerciseRequest ex = new ExerciseRequest();
            ex.setQuestion(requireString(map, "question", i));
            ex.setAnswerKey(requireString(map, "answerKey", i));
            ex.setDifficulty(parseDifficulty(map.get("difficulty"), DifficultyLevel.EASY));
            ex.setPoints(asInt(map.get("points"), 5));
            ex.setType(parseExerciseType(map.get("type")));
            ex.setOptions(asStringList(map.get("options")));
            ex.setHint(asString(map.get("hint")));
            ex.setExplanation(asString(map.get("explanation")));
            exercises.add(ex);
        }
        return exercises;
    }

    private DifficultyLevel parseDifficulty(Object raw, DifficultyLevel fallback) {
        if (raw == null) return fallback;
        String value = raw.toString().trim().toUpperCase();
        try {
            return DifficultyLevel.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid difficulty '" + raw + "'. Expected one of: "
                    + Arrays.toString(DifficultyLevel.values()));
        }
    }

    private com.riksa.learning.entity.ExerciseType parseExerciseType(Object raw) {
        if (raw == null) return com.riksa.learning.entity.ExerciseType.TEXT;
        try {
            return com.riksa.learning.entity.ExerciseType.valueOf(raw.toString().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid exercise type '" + raw + "'");
        }
    }

    private List<String> asStringList(Object raw) {
        if (raw == null) return List.of();
        if (!(raw instanceof List<?> values)) {
            throw new IllegalArgumentException("Exercise options must be a YAML list");
        }
        return values.stream().map(this::asString).filter(java.util.Objects::nonNull).toList();
    }

    private UUID parseCategoryId(Object raw) {
        if (raw == null) return null;
        String value = raw.toString().trim();
        if (value.isEmpty()) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid categoryId '" + value + "', expected a UUID");
        }
    }

    private String requireString(Map<String, Object> map, String key) {
        String value = asString(map.get(key));
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required frontmatter field: '" + key + "'");
        }
        return value;
    }

    private String requireString(Map<String, Object> map, String key, int index) {
        String value = asString(map.get(key));
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Exercise #" + (index + 1) + " is missing required field: '" + key + "'");
        }
        return value;
    }

    private String asString(Object raw) {
        if (raw == null) return null;
        String value = raw.toString().trim();
        return value.isEmpty() ? null : value;
    }

    private int asInt(Object raw, int fallback) {
        if (raw == null) return fallback;
        if (raw instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(raw.toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer value: '" + raw + "'");
        }
    }
}
