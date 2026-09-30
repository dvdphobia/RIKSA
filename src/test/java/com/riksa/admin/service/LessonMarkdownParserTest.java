package com.riksa.admin.service;

import com.riksa.admin.dto.LessonCreateRequest;
import com.riksa.common.enums.DifficultyLevel;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LessonMarkdownParserTest {

    private final LessonMarkdownParser parser = new LessonMarkdownParser();

    @Test
    void parse_fullDocument_mapsAllFields() {
        UUID categoryId = UUID.randomUUID();
        String md = """
                ---
                title: Introduction to Variables
                difficulty: MEDIUM
                baseXp: 25
                categoryId: %s
                reflectionPrompt: What surprised you?
                exercises:
                  - question: What is a variable?
                    answerKey: A named storage location
                    difficulty: HARD
                    points: 10
                  - question: What is 2+2?
                    answerKey: "4"
                ---

                # Intro

                Lesson **content** here.
                """.formatted(categoryId);

        LessonCreateRequest req = parser.parse(md);

        assertThat(req.getTitle()).isEqualTo("Introduction to Variables");
        assertThat(req.getDifficulty()).isEqualTo(DifficultyLevel.MEDIUM);
        assertThat(req.getBaseXp()).isEqualTo(25);
        assertThat(req.getCategoryId()).isEqualTo(categoryId);
        assertThat(req.getReflectionPrompt()).isEqualTo("What surprised you?");
        assertThat(req.getDescription()).contains("# Intro").contains("Lesson **content** here.");
        assertThat(req.getExercises()).hasSize(2);
        assertThat(req.getExercises().get(0).getQuestion()).isEqualTo("What is a variable?");
        assertThat(req.getExercises().get(0).getAnswerKey()).isEqualTo("A named storage location");
        assertThat(req.getExercises().get(0).getDifficulty()).isEqualTo(DifficultyLevel.HARD);
        assertThat(req.getExercises().get(0).getPoints()).isEqualTo(10);
        assertThat(req.getExercises().get(1).getDifficulty()).isEqualTo(DifficultyLevel.EASY);
        assertThat(req.getExercises().get(1).getPoints()).isEqualTo(5);
    }

    @Test
    void parse_minimalDocument_appliesDefaults() {
        String md = """
                ---
                title: Minimal
                exercises:
                  - question: Q?
                    answerKey: A
                ---
                Body text.
                """;

        LessonCreateRequest req = parser.parse(md);

        assertThat(req.getDifficulty()).isEqualTo(DifficultyLevel.EASY);
        assertThat(req.getBaseXp()).isEqualTo(10);
        assertThat(req.getCategoryId()).isNull();
        assertThat(req.getReflectionPrompt()).isNull();
        assertThat(req.getDescription()).isEqualTo("Body text.");
        assertThat(req.getExercises()).hasSize(1);
    }

    @Test
    void parse_emptyBody_descriptionIsNull() {
        String md = """
                ---
                title: No Body
                exercises:
                  - question: Q?
                    answerKey: A
                ---
                """;

        assertThat(parser.parse(md).getDescription()).isNull();
    }

    @Test
    void parse_missingFrontmatter_throws() {
        assertThatThrownBy(() -> parser.parse("# Just markdown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("frontmatter");
    }

    @Test
    void parse_unterminatedFrontmatter_throws() {
        assertThatThrownBy(() -> parser.parse("---\ntitle: X\nbody without closing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("closing ---");
    }

    @Test
    void parse_missingTitle_throws() {
        String md = """
                ---
                exercises:
                  - question: Q?
                    answerKey: A
                ---
                body
                """;
        assertThatThrownBy(() -> parser.parse(md))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }

    @Test
    void parse_missingExercises_throws() {
        String md = """
                ---
                title: No Exercises
                ---
                body
                """;
        assertThatThrownBy(() -> parser.parse(md))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exercises");
    }

    @Test
    void parse_exerciseMissingAnswerKey_throws() {
        String md = """
                ---
                title: Bad Exercise
                exercises:
                  - question: Q?
                ---
                body
                """;
        assertThatThrownBy(() -> parser.parse(md))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("answerKey");
    }

    @Test
    void parse_invalidDifficulty_throws() {
        String md = """
                ---
                title: Bad Difficulty
                difficulty: IMPOSSIBLE
                exercises:
                  - question: Q?
                    answerKey: A
                ---
                body
                """;
        assertThatThrownBy(() -> parser.parse(md))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid difficulty");
    }

    @Test
    void parse_invalidCategoryId_throws() {
        String md = """
                ---
                title: Bad Category
                categoryId: not-a-uuid
                exercises:
                  - question: Q?
                    answerKey: A
                ---
                body
                """;
        assertThatThrownBy(() -> parser.parse(md))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("categoryId");
    }

    @Test
    void parse_roadmapUnitAndMinutes_areRead() {
        UUID unitId = UUID.randomUUID();
        String md = """
                ---
                title: Enzymes
                estimatedMinutes: 14
                roadmapUnitId: %s
                exercises:
                  - question: Q?
                    answerKey: A
                ---
                body
                """.formatted(unitId);

        LessonCreateRequest req = parser.parse(md);

        assertThat(req.getRoadmapUnitId()).isEqualTo(unitId);
        assertThat(req.getEstimatedMinutes()).isEqualTo(14);
    }

    @Test
    void parse_withoutRoadmapUnitOrMinutes_leavesThemUnset() {
        String md = """
                ---
                title: Loose
                exercises:
                  - question: Q?
                    answerKey: A
                ---
                body
                """;

        LessonCreateRequest req = parser.parse(md);

        assertThat(req.getRoadmapUnitId()).isNull();
        assertThat(req.getEstimatedMinutes()).isNull();
    }

    @Test
    void parse_invalidRoadmapUnitId_throws() {
        String md = """
                ---
                title: Bad Unit
                roadmapUnitId: unit-4
                exercises:
                  - question: Q?
                    answerKey: A
                ---
                body
                """;
        assertThatThrownBy(() -> parser.parse(md))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("roadmapUnitId");
    }

    @Test
    void parse_invalidYaml_throws() {
        assertThatThrownBy(() -> parser.parse("---\ntitle: [unclosed\n---\nbody"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("YAML");
    }

    @Test
    void parse_emptyContent_throws() {
        assertThatThrownBy(() -> parser.parse("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }
}
