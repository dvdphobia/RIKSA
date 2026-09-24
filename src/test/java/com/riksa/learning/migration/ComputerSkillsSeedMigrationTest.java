package com.riksa.learning.migration;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComputerSkillsSeedMigrationTest {

    private static final String CATEGORY_ID = "c3800000-0000-0000-0000-000000000001";
    private static final String UNIT_ID = "38000000-0000-0000-0000-000000000001";
    private static final String LESSON_LINE_PREFIX = "    '38100000-";
    private static final String EXERCISE_LINE_PREFIX = "('38200000-";

    @Test
    void seedsOneBilingualCategoryAndUnitWithFiveLessonsAndFifteenQcmExercises() throws IOException {
        String sql = new ClassPathResource(
                "db/migration/V38__seed_computer_skills_starter_unit.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        assertTrue(sql.contains(CATEGORY_ID));
        assertTrue(sql.contains(UNIT_ID));
        assertTrue(sql.contains("Computer Skills / ជំនាញកុំព្យូទ័រ"));
        assertTrue(sql.contains("Digital basics / មូលដ្ឋានឌីជីថល"));
        assertEquals(5, occurrences(sql, LESSON_LINE_PREFIX));
        assertEquals(15, occurrences(sql, EXERCISE_LINE_PREFIX));
        assertEquals(15, occurrences(sql, "'MULTIPLE_CHOICE'"));
        assertEquals(5, occurrences(sql, "'EASY', 15,"));
        assertEquals(5, occurrences(sql, "## 1."));
        assertEquals(5, occurrences(sql, "## 2."));
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        int fromIndex = 0;
        while ((fromIndex = text.indexOf(needle, fromIndex)) >= 0) {
            count++;
            fromIndex += needle.length();
        }
        return count;
    }
}
