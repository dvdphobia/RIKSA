package com.riksa.learning.service;

import com.riksa.common.enums.DifficultyLevel;
import com.riksa.learning.dto.RoadmapLessonResponse;
import com.riksa.learning.entity.*;
import com.riksa.learning.repository.*;
import com.riksa.social.entity.Category;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoadmapServiceTest {

    @Mock RoadmapUnitRepository unitRepository;
    @Mock LessonRepository lessonRepository;
    @Mock ExerciseRepository exerciseRepository;
    @Mock LessonCompletionRepository completionRepository;
    @Mock ExerciseAttemptRepository attemptRepository;
    @Mock UserLessonProgressRepository progressRepository;
    @Mock RoadmapUnitSkipRepository skipRepository;

    @Test
    void buildsCurrentUnitFromCompletionAndExerciseProgressAndLocksTheNextUnit() {
        UUID userId = UUID.randomUUID();
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Mathematics");
        RoadmapUnit foundations = unit(category, 1, "Foundations");
        RoadmapUnit functions = unit(category, 2, "Functions & Graphs");
        Lesson completed = lesson(foundations, 1, "Numbers");
        Lesson current = lesson(foundations, 2, "Algebra");
        Lesson future = lesson(functions, 1, "Functions");
        Exercise first = exercise(current, 5);
        Exercise second = exercise(current, 5);
        LessonCompletion completion = new LessonCompletion();
        completion.setLessonId(completed.getId());
        UserLessonProgress progress = new UserLessonProgress();
        progress.setLessonId(current.getId());
        ExerciseAttemptRepository.LessonCorrectCount correct = mock(ExerciseAttemptRepository.LessonCorrectCount.class);
        when(correct.getLessonId()).thenReturn(current.getId());
        when(correct.getCorrectCount()).thenReturn(1L);

        when(unitRepository.findByCategoryIdOrderByPositionAsc(category.getId()))
                .thenReturn(List.of(foundations, functions));
        when(lessonRepository.findRoadmapLessons(List.of(foundations.getId(), functions.getId())))
                .thenReturn(List.of(completed, current, future));
        when(exerciseRepository.findByLessonIdIn(anyList())).thenReturn(List.of(first, second));
        when(completionRepository.findByUserIdAndLessonIdIn(eq(userId), anyList()))
                .thenReturn(List.of(completion));
        when(progressRepository.findByUserIdAndLessonIdIn(eq(userId), anyList()))
                .thenReturn(List.of(progress));
        when(attemptRepository.countCorrectByUserIdAndLessonIds(eq(userId), anyList()))
                .thenReturn(List.of(correct));
        when(skipRepository.findByUserIdAndRoadmapUnitIdIn(eq(userId), anyList()))
                .thenReturn(List.of());

        var roadmap = service().getRoadmap(userId, category.getId());

        assertThat(roadmap.completedLessons()).isEqualTo(1);
        assertThat(roadmap.currentLessonId()).isEqualTo(current.getId());
        assertThat(roadmap.units().getFirst().status()).isEqualTo("CURRENT");
        assertThat(roadmap.units().getFirst().lessons().get(1).status()).isEqualTo("IN_PROGRESS");
        assertThat(roadmap.units().getFirst().lessons().get(1).progress()).isEqualTo(0.5);
        assertThat(roadmap.units().get(1).status()).isEqualTo("LOCKED");
    }

    @Test
    void roadmapLessonSummaryResolvesBackgroundFromTheLessonThenTheSubject() {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Chemistry");
        category.setBackgroundKey("chemistry");
        category.setBackgroundImageUrl("https://subject.example/chem.png");
        RoadmapUnit unit = unit(category, 1, "Atoms");
        Lesson inherited = lesson(unit, 1, "Elements");
        Lesson overridden = lesson(unit, 2, "Bonds");
        overridden.setBackgroundKey("none");
        overridden.setBackgroundImageUrl("https://lesson.example/bonds.png");

        when(unitRepository.findByCategoryIdOrderByPositionAsc(category.getId())).thenReturn(List.of(unit));
        when(lessonRepository.findRoadmapLessons(List.of(unit.getId()))).thenReturn(List.of(inherited, overridden));
        when(exerciseRepository.findByLessonIdIn(anyList())).thenReturn(List.of());

        var lessons = service().getRoadmap(null, category.getId()).units().getFirst().lessons();

        assertThat(lessons.get(0).backgroundKey()).isEqualTo("chemistry");
        assertThat(lessons.get(0).backgroundImageUrl()).isEqualTo("https://subject.example/chem.png");
        assertThat(lessons.get(1).backgroundKey()).isEqualTo("none");
        assertThat(lessons.get(1).backgroundImageUrl()).isEqualTo("https://lesson.example/bonds.png");
    }

    @Test
    void checksUnitOnlyWhenEveryLessonIsCompleted() {
        UUID userId = UUID.randomUUID();
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Mathematics");
        RoadmapUnit foundations = unit(category, 1, "Foundations");
        List<Lesson> fiveLessons = java.util.stream.IntStream.rangeClosed(1, 5)
                .mapToObj(position -> lesson(foundations, position, "Foundation " + position))
                .toList();
        List<LessonCompletion> completions = fiveLessons.stream().map(item -> {
            LessonCompletion completion = new LessonCompletion();
            completion.setLessonId(item.getId());
            return completion;
        }).toList();

        when(unitRepository.findByCategoryIdOrderByPositionAsc(category.getId()))
                .thenReturn(List.of(foundations));
        when(lessonRepository.findRoadmapLessons(List.of(foundations.getId()))).thenReturn(fiveLessons);
        when(exerciseRepository.findByLessonIdIn(anyList())).thenReturn(List.of());
        when(completionRepository.findByUserIdAndLessonIdIn(eq(userId), anyList())).thenReturn(completions);
        when(progressRepository.findByUserIdAndLessonIdIn(eq(userId), anyList())).thenReturn(List.of());
        when(attemptRepository.countCorrectByUserIdAndLessonIds(eq(userId), anyList())).thenReturn(List.of());
        when(skipRepository.findByUserIdAndRoadmapUnitIdIn(eq(userId), anyList())).thenReturn(List.of());

        var roadmap = service().getRoadmap(userId, category.getId());

        assertThat(roadmap.units().getFirst().status()).isEqualTo("COMPLETED");
        assertThat(roadmap.units().getFirst().completedLessons()).isEqualTo(5);
        assertThat(roadmap.progress()).isEqualTo(1.0);
    }

    @Test
    void lessonsWithoutAUnitContinueTheLastUnitInsteadOfDisappearing() {
        UUID userId = UUID.randomUUID();
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Biology");
        RoadmapUnit cells = unit(category, 1, "Cells");
        RoadmapUnit genetics = unit(category, 2, "Genetics");
        Lesson cell = lesson(cells, 1, "Cell structure");
        Lesson dna = lesson(genetics, 1, "DNA");
        Lesson uploaded = lesson(genetics, 104, "Protein synthesis");
        uploaded.setRoadmapUnit(null);

        when(unitRepository.findByCategoryIdOrderByPositionAsc(category.getId()))
                .thenReturn(List.of(cells, genetics));
        when(lessonRepository.findRoadmapLessons(List.of(cells.getId(), genetics.getId())))
                .thenReturn(List.of(cell, dna));
        when(lessonRepository.findByCategoryIdAndRoadmapUnitIsNullOrderByPositionAsc(category.getId()))
                .thenReturn(List.of(uploaded));
        when(exerciseRepository.findByLessonIdIn(anyList())).thenReturn(List.of());
        when(completionRepository.findByUserIdAndLessonIdIn(eq(userId), anyList())).thenReturn(List.of());
        when(progressRepository.findByUserIdAndLessonIdIn(eq(userId), anyList())).thenReturn(List.of());
        when(attemptRepository.countCorrectByUserIdAndLessonIds(eq(userId), anyList())).thenReturn(List.of());
        when(skipRepository.findByUserIdAndRoadmapUnitIdIn(eq(userId), anyList())).thenReturn(List.of());

        var roadmap = service().getRoadmap(userId, category.getId());

        assertThat(roadmap.totalLessons()).isEqualTo(3);
        assertThat(roadmap.units().get(1).lessons())
                .extracting(RoadmapLessonResponse::title)
                .containsExactly("DNA", "Protein synthesis");
        assertThat(roadmap.units().get(1).totalLessons()).isEqualTo(2);
    }

    private RoadmapService service() {
        return new RoadmapService(unitRepository, lessonRepository, exerciseRepository,
                completionRepository, attemptRepository, progressRepository, skipRepository,
                new LessonRewardPolicy());
    }

    private RoadmapUnit unit(Category category, int position, String title) {
        RoadmapUnit unit = new RoadmapUnit();
        unit.setId(UUID.randomUUID());
        unit.setCategory(category);
        unit.setPosition(position);
        unit.setTitle(title);
        unit.setDescription(title + " description");
        return unit;
    }

    private Lesson lesson(RoadmapUnit unit, int position, String title) {
        Lesson lesson = new Lesson();
        lesson.setId(UUID.randomUUID());
        lesson.setRoadmapUnit(unit);
        lesson.setCategory(unit.getCategory());
        lesson.setPosition(position);
        lesson.setTitle(title);
        lesson.setDescription(title + " description");
        lesson.setDifficulty(DifficultyLevel.EASY);
        lesson.setBaseXp(10);
        lesson.setEstimatedMinutes(8);
        return lesson;
    }

    private Exercise exercise(Lesson lesson, int points) {
        Exercise exercise = new Exercise();
        exercise.setId(UUID.randomUUID());
        exercise.setLesson(lesson);
        exercise.setPoints(points);
        return exercise;
    }
}
