package com.riksa.learning.service;

import com.riksa.learning.entity.Lesson;
import com.riksa.social.entity.Category;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LessonBackgroundsTest {

    @Test
    void resolvePrefersLessonThenCategoryThenNull() {
        assertThat(LessonBackgrounds.resolve("math", null, "physics", "https://subject.example/a.png"))
                .isEqualTo(new LessonBackgrounds.Resolved("math", "https://subject.example/a.png"));
        assertThat(LessonBackgrounds.resolve(null, "https://lesson.example/a.png", "physics", "https://subject.example/a.png"))
                .isEqualTo(new LessonBackgrounds.Resolved("physics", "https://lesson.example/a.png"));
        assertThat(LessonBackgrounds.resolve(null, null, "biology", null))
                .isEqualTo(new LessonBackgrounds.Resolved("biology", null));
        assertThat(LessonBackgrounds.resolve(null, null, null, null))
                .isEqualTo(new LessonBackgrounds.Resolved(null, null));
    }

    @Test
    void noneDoesNotFallThroughAndBlankDoesNotCount() {
        LessonBackgrounds.Resolved resolved = LessonBackgrounds.resolve(
                "none", "  ", "chemistry", "https://subject.example/a.png");

        assertThat(resolved.backgroundKey()).isEqualTo("none");
        assertThat(resolved.backgroundImageUrl()).isEqualTo("https://subject.example/a.png");
    }

    @Test
    void imageWinsWhenBothFieldsResolve() {
        Lesson lesson = new Lesson();
        Category category = new Category();
        category.setBackgroundKey("physics");
        category.setBackgroundImageUrl("https://subject.example/a.png");
        lesson.setCategory(category);
        lesson.setBackgroundKey("math");
        lesson.setBackgroundImageUrl("https://lesson.example/a.png");

        LessonBackgrounds.Resolved resolved = LessonBackgrounds.resolve(lesson);

        assertThat(resolved.backgroundKey()).isEqualTo("math");
        assertThat(resolved.backgroundImageUrl()).isEqualTo("https://lesson.example/a.png");
        assertThat(resolved.showsImage()).isTrue();
    }

    @Test
    void presetIsUsedOnlyWhenNoImageResolves() {
        LessonBackgrounds.Resolved preset = LessonBackgrounds.resolve("biology", null, null, null);
        LessonBackgrounds.Resolved empty = LessonBackgrounds.resolve(null, null, null, null);

        assertThat(preset.showsImage()).isFalse();
        assertThat(preset.backgroundKey()).isEqualTo("biology");
        assertThat(empty.showsImage()).isFalse();
        assertThat(empty.backgroundKey()).isNull();
    }

    @Test
    void normalizeKeyAcceptsThePresetSetAndRejectsUnknownValues() {
        assertThat(LessonBackgrounds.normalizeKey(null)).isNull();
        assertThat(LessonBackgrounds.normalizeKey("  ")).isNull();
        assertThat(LessonBackgrounds.normalizeKey(" computer_science ")).isEqualTo("computer_science");
        assertThat(LessonBackgrounds.normalizeKey("none")).isEqualTo("none");

        assertThatThrownBy(() -> LessonBackgrounds.normalizeKey("MATH"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("backgroundKey must be one of: math, physics, chemistry, biology, computer_science, none");
        assertThatThrownBy(() -> LessonBackgrounds.normalizeKey("history"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
