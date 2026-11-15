package com.riksa.learning.service;

import com.riksa.common.enums.DifficultyLevel;
import com.riksa.learning.entity.Lesson;
import com.riksa.learning.entity.RoadmapUnit;
import com.riksa.learning.repository.LessonRepository;
import com.riksa.learning.repository.RoadmapUnitRepository;
import com.riksa.social.entity.Category;
import com.riksa.social.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the real Flyway schema with {@code ddl-auto: validate}. A SMALLINT/INTEGER
 * style mismatch fails context startup before these assertions run.
 */
@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "RIKSA_RUN_INTEGRATION_TESTS", matches = "true")
class LessonBackgroundMappingIntegrationTest {

    @Autowired CategoryRepository categoryRepository;
    @Autowired LessonRepository lessonRepository;
    @Autowired RoadmapUnitRepository roadmapUnitRepository;
    @Autowired LessonService lessonService;
    @Autowired RoadmapService roadmapService;
    @Autowired JdbcTemplate jdbc;

    @Test
    void columnsMatchStringMappingAndStudentPayloadsResolveStoredValues() {
        List<Map<String, Object>> columns = jdbc.queryForList("""
                SELECT table_name, column_name, data_type, character_maximum_length
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND column_name IN ('background_key', 'background_image_url')
                ORDER BY table_name, column_name
                """);

        assertThat(columns).hasSize(4);
        assertThat(columns).allSatisfy(column -> {
            if ("background_key".equals(column.get("column_name"))) {
                assertThat(column.get("data_type")).isEqualTo("character varying");
                assertThat(((Number) column.get("character_maximum_length")).intValue()).isEqualTo(32);
            } else {
                assertThat(column.get("column_name")).isEqualTo("background_image_url");
                assertThat(column.get("data_type")).isEqualTo("text");
                assertThat(column.get("character_maximum_length")).isNull();
            }
        });

        String token = UUID.randomUUID().toString().substring(0, 8);
        Category category = new Category();
        category.setName("Background " + token);
        category.setSlug("background-" + token);
        category.setOfficial(true);
        category.setBackgroundKey("physics");
        category.setBackgroundImageUrl("https://subject.example/physics.png");
        category = categoryRepository.saveAndFlush(category);

        RoadmapUnit unit = new RoadmapUnit();
        unit.setCategory(category);
        unit.setTitle("Motion");
        unit.setPosition(1);
        unit = roadmapUnitRepository.saveAndFlush(unit);

        Lesson inherited = lesson(category, unit, 1, "Speed");
        Lesson overridden = lesson(category, unit, 2, "Forces");
        overridden.setBackgroundKey("none");
        overridden.setBackgroundImageUrl("https://lesson.example/forces.png");
        inherited = lessonRepository.saveAndFlush(inherited);
        overridden = lessonRepository.save(overridden);
        lessonRepository.flush();

        var catalog = lessonService.listLessons(null, category.getId(), null, 0, 20).getContent();
        assertThat(catalog).extracting(item -> item.getTitle())
                .containsExactly("Speed", "Forces");
        assertThat(catalog.get(0).getBackgroundKey()).isEqualTo("physics");
        assertThat(catalog.get(0).getBackgroundImageUrl()).isEqualTo("https://subject.example/physics.png");
        assertThat(catalog.get(1).getBackgroundKey()).isEqualTo("none");
        assertThat(catalog.get(1).getBackgroundImageUrl()).isEqualTo("https://lesson.example/forces.png");

        var detail = lessonService.getLesson(null, inherited.getId());
        assertThat(detail.getBackgroundKey()).isEqualTo("physics");
        assertThat(detail.getBackgroundImageUrl()).isEqualTo("https://subject.example/physics.png");

        var roadmapLessons = roadmapService.getRoadmap(null, category.getId()).units().getFirst().lessons();
        assertThat(roadmapLessons.get(0).backgroundKey()).isEqualTo("physics");
        assertThat(roadmapLessons.get(1).backgroundKey()).isEqualTo("none");
        assertThat(roadmapLessons.get(1).backgroundImageUrl()).isEqualTo("https://lesson.example/forces.png");

        String storedKey = jdbc.queryForObject(
                "SELECT background_key FROM lessons WHERE id = ?", String.class, overridden.getId());
        assertThat(storedKey).isEqualTo("none");
    }

    private Lesson lesson(Category category, RoadmapUnit unit, int position, String title) {
        Lesson lesson = new Lesson();
        lesson.setTitle(title);
        lesson.setCategory(category);
        lesson.setRoadmapUnit(unit);
        lesson.setPosition(position);
        lesson.setDifficulty(DifficultyLevel.EASY);
        lesson.setBaseXp(10);
        lesson.setEstimatedMinutes(8);
        return lesson;
    }
}
