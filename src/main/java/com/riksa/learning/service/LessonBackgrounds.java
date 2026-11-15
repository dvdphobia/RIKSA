package com.riksa.learning.service;

import com.riksa.learning.entity.Lesson;
import com.riksa.social.entity.Category;

import java.util.List;
import java.util.Set;

/**
 * Preset keys shipped in the student app, plus the lesson-over-subject resolution
 * the lesson page reads. Admin APIs store the raw columns and do not call {@link #resolve}.
 */
public final class LessonBackgrounds {

    public static final List<String> KEYS = List.of(
            "math", "physics", "chemistry", "biology", "computer_science", "none");

    private static final Set<String> KEY_SET = Set.copyOf(KEYS);

    private LessonBackgrounds() {}

    /**
     * @return the canonical key, or null when the caller asked to clear / inherit
     * @throws IllegalArgumentException when the value is not one of {@link #KEYS}
     */
    public static String normalizeKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String key = raw.trim();
        if (!KEY_SET.contains(key)) {
            throw new IllegalArgumentException(
                    "backgroundKey must be one of: " + String.join(", ", KEYS));
        }
        return key;
    }

    /** Lesson value, otherwise the subject value, otherwise null. Blank does not count as a value. */
    public static Resolved resolve(Lesson lesson) {
        Category category = lesson.getCategory();
        return resolve(
                lesson.getBackgroundKey(),
                lesson.getBackgroundImageUrl(),
                category == null ? null : category.getBackgroundKey(),
                category == null ? null : category.getBackgroundImageUrl());
    }

    public static Resolved resolve(String lessonKey, String lessonImageUrl,
                                   String categoryKey, String categoryImageUrl) {
        return new Resolved(first(lessonKey, categoryKey), first(lessonImageUrl, categoryImageUrl));
    }

    private static String first(String primary, String fallback) {
        if (present(primary)) {
            return primary;
        }
        return present(fallback) ? fallback : null;
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Both fields are returned to the app. {@link #showsImage()} is the paint rule:
     * a resolved image is the background, and the key is used only when the image is absent.
     * A key of {@code none} with no image means show no preset. Both null means the app
     * uses its own subject illustration.
     */
    public record Resolved(String backgroundKey, String backgroundImageUrl) {
        public boolean showsImage() {
            return present(backgroundImageUrl);
        }
    }
}
