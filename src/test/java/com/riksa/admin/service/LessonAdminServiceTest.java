package com.riksa.admin.service;

import com.riksa.admin.dto.*;
import com.riksa.common.enums.DifficultyLevel;
import com.riksa.learning.entity.Exercise;
import com.riksa.learning.entity.ExerciseType;
import com.riksa.learning.entity.Lesson;
import com.riksa.learning.repository.ExerciseRepository;
import com.riksa.learning.repository.LessonRepository;
import com.riksa.social.entity.Category;
import com.riksa.social.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LessonAdminServiceTest {

    @Mock private LessonRepository lessonRepository;
    @Mock private ExerciseRepository exerciseRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private LessonMarkdownParser markdownParser;

    @InjectMocks private LessonAdminService lessonAdminService;

    @Test
    void create_validRequest_createsLessonWithExercises() {
        UUID categoryId = UUID.randomUUID();
        Category category = new Category();
        category.setId(categoryId);
        category.setName("Physics");

        ExerciseRequest exerciseReq = new ExerciseRequest();
        exerciseReq.setQuestion("What is Newton's first law?");
        exerciseReq.setAnswerKey("Inertia");
        exerciseReq.setDifficulty(DifficultyLevel.EASY);
        exerciseReq.setPoints(5);

        LessonCreateRequest req = new LessonCreateRequest();
        req.setTitle("Physics Basics");
        req.setDescription("Intro to physics");
        req.setDifficulty(DifficultyLevel.EASY);
        req.setBaseXp(20);
        req.setCategoryId(categoryId);
        req.setExercises(List.of(exerciseReq));

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> {
            Lesson l = inv.getArgument(0);
            l.setId(UUID.randomUUID());
            return l;
        });
        when(exerciseRepository.saveAll(any())).thenAnswer(inv -> {
            List<Exercise> exercises = inv.getArgument(0);
            exercises.forEach(e -> e.setId(UUID.randomUUID()));
            return exercises;
        });

        LessonResponse result = lessonAdminService.create(req);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Physics Basics");
        assertThat(result.getDifficulty()).isEqualTo("EASY");
        assertThat(result.getBaseXp()).isEqualTo(20);
        assertThat(result.getExercises()).hasSize(1);
        verify(exerciseRepository).saveAll(any());
    }

    @Test
    void create_multipleChoiceAndTrueFalseExercises_persistsRichExerciseFields() {
        ExerciseRequest multipleChoice = new ExerciseRequest();
        multipleChoice.setType(ExerciseType.MULTIPLE_CHOICE);
        multipleChoice.setQuestion("Which number is even?");
        multipleChoice.setAnswerKey("2");
        multipleChoice.setOptions(List.of("1", "2", "3"));
        multipleChoice.setDifficulty(DifficultyLevel.EASY);
        multipleChoice.setPoints(5);
        multipleChoice.setHint("Look for a number divisible by two.");
        multipleChoice.setExplanation("Two divides evenly by two.");

        ExerciseRequest trueFalse = new ExerciseRequest();
        trueFalse.setType(ExerciseType.TRUE_FALSE);
        trueFalse.setQuestion("The sky is blue on a clear day.");
        trueFalse.setAnswerKey("True");
        trueFalse.setOptions(List.of("True", "False"));
        trueFalse.setDifficulty(DifficultyLevel.EASY);
        trueFalse.setPoints(5);

        LessonCreateRequest req = new LessonCreateRequest();
        req.setTitle("Rich exercises");
        req.setDifficulty(DifficultyLevel.EASY);
        req.setBaseXp(10);
        req.setExercises(List.of(multipleChoice, trueFalse));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> {
            Lesson lesson = inv.getArgument(0); lesson.setId(UUID.randomUUID()); return lesson;
        });
        when(exerciseRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        LessonResponse result = lessonAdminService.create(req);

        assertThat(result.getExercises()).extracting("type")
                .containsExactly(ExerciseType.MULTIPLE_CHOICE, ExerciseType.TRUE_FALSE);
        assertThat(result.getExercises().getFirst().getOptions()).containsExactly("1", "2", "3");
        assertThat(result.getExercises().getFirst().getHint()).isEqualTo(multipleChoice.getHint());
    }

    @Test
    void create_categoryNotFound_throwsException() {
        UUID categoryId = UUID.randomUUID();
        LessonCreateRequest req = new LessonCreateRequest();
        req.setTitle("Test");
        req.setDifficulty(DifficultyLevel.EASY);
        req.setBaseXp(10);
        req.setCategoryId(categoryId);
        req.setExercises(List.of());

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonAdminService.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    void create_nullCategoryId_createsWithoutCategory() {
        ExerciseRequest exerciseReq = new ExerciseRequest();
        exerciseReq.setQuestion("What is 2+2?");
        exerciseReq.setAnswerKey("4");
        exerciseReq.setDifficulty(DifficultyLevel.EASY);
        exerciseReq.setPoints(5);

        LessonCreateRequest req = new LessonCreateRequest();
        req.setTitle("Math Basics");
        req.setDifficulty(DifficultyLevel.EASY);
        req.setBaseXp(10);
        req.setExercises(List.of(exerciseReq));

        when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> {
            Lesson l = inv.getArgument(0);
            l.setId(UUID.randomUUID());
            return l;
        });
        when(exerciseRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        LessonResponse result = lessonAdminService.create(req);

        assertThat(result.getCategoryId()).isNull();
    }

    @Test
    void update_existingLesson_updatesFields() {
        UUID lessonId = UUID.randomUUID();
        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setTitle("Old Title");
        lesson.setDifficulty(DifficultyLevel.EASY);
        lesson.setBaseXp(10);

        LessonUpdateRequest req = new LessonUpdateRequest();
        req.setTitle("New Title");
        req.setBaseXp(30);

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> inv.getArgument(0));
        when(exerciseRepository.findByLessonId(lessonId)).thenReturn(List.of());

        LessonResponse result = lessonAdminService.update(lessonId, req);

        assertThat(result.getTitle()).isEqualTo("New Title");
        assertThat(result.getBaseXp()).isEqualTo(30);
    }

    @Test
    void update_nonExistentLesson_throwsException() {
        UUID lessonId = UUID.randomUUID();
        LessonUpdateRequest req = new LessonUpdateRequest();
        req.setTitle("New");

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonAdminService.update(lessonId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Lesson not found");
    }

    @Test
    void update_replaceExercises_deletesOldAndCreatesNew() {
        UUID lessonId = UUID.randomUUID();
        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setTitle("Test");
        lesson.setDifficulty(DifficultyLevel.MEDIUM);
        lesson.setBaseXp(15);

        ExerciseRequest newEx = new ExerciseRequest();
        newEx.setQuestion("New question?");
        newEx.setAnswerKey("New answer");
        newEx.setDifficulty(DifficultyLevel.HARD);
        newEx.setPoints(10);

        LessonUpdateRequest req = new LessonUpdateRequest();
        req.setExercises(List.of(newEx));

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> inv.getArgument(0));
        when(exerciseRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        LessonResponse result = lessonAdminService.update(lessonId, req);

        verify(exerciseRepository).deleteByLessonId(lessonId);
        verify(exerciseRepository).flush();
        verify(exerciseRepository).saveAll(any());
        assertThat(result.getExercises()).hasSize(1);
    }

    @Test
    void delete_existingLesson_deletesIt() {
        UUID lessonId = UUID.randomUUID();
        Lesson lesson = new Lesson();
        lesson.setId(lessonId);

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));

        lessonAdminService.delete(lessonId);

        verify(lessonRepository).delete(lesson);
    }

    @Test
    void delete_nonExistentLesson_throwsException() {
        UUID lessonId = UUID.randomUUID();
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonAdminService.delete(lessonId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Lesson not found");
    }

    @Test
    void bulkCreate_mixedSuccessAndFailure_returnsResults() {
        UUID categoryId = UUID.randomUUID();
        Category category = new Category();
        category.setId(categoryId);

        ExerciseRequest exerciseReq = new ExerciseRequest();
        exerciseReq.setQuestion("Q?");
        exerciseReq.setAnswerKey("A");
        exerciseReq.setDifficulty(DifficultyLevel.EASY);
        exerciseReq.setPoints(5);

        LessonCreateRequest goodReq = new LessonCreateRequest();
        goodReq.setTitle("Good Lesson");
        goodReq.setDifficulty(DifficultyLevel.EASY);
        goodReq.setBaseXp(10);
        goodReq.setExercises(List.of(exerciseReq));

        LessonCreateRequest badReq = new LessonCreateRequest();
        badReq.setTitle("Bad Lesson");
        badReq.setDifficulty(DifficultyLevel.EASY);
        badReq.setBaseXp(10);
        badReq.setCategoryId(UUID.randomUUID()); // non-existent
        badReq.setExercises(List.of(exerciseReq));

        when(categoryRepository.findById(any())).thenReturn(Optional.of(category));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> {
            Lesson l = inv.getArgument(0);
            l.setId(UUID.randomUUID());
            return l;
        });
        when(exerciseRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        // For badReq, category not found
        when(categoryRepository.findById(badReq.getCategoryId())).thenReturn(Optional.empty());

        BulkLessonResult result = lessonAdminService.bulkCreate(List.of(goodReq, badReq));

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.getErrors()).hasSize(1);
    }

    @Test
    void uploadMarkdown_validFile_createsLesson() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "lesson.md", "text/markdown", "---\ntitle: T\n---\nbody".getBytes(StandardCharsets.UTF_8));

        ExerciseRequest exerciseReq = new ExerciseRequest();
        exerciseReq.setQuestion("Q?");
        exerciseReq.setAnswerKey("A");
        exerciseReq.setDifficulty(DifficultyLevel.EASY);
        exerciseReq.setPoints(5);

        LessonCreateRequest parsed = new LessonCreateRequest();
        parsed.setTitle("T");
        parsed.setDescription("body");
        parsed.setDifficulty(DifficultyLevel.EASY);
        parsed.setBaseXp(10);
        parsed.setExercises(List.of(exerciseReq));

        when(markdownParser.parse(any(String.class))).thenReturn(parsed);
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> {
            Lesson l = inv.getArgument(0);
            l.setId(UUID.randomUUID());
            return l;
        });
        when(exerciseRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        LessonResponse result = lessonAdminService.uploadMarkdown(file);

        assertThat(result.getTitle()).isEqualTo("T");
        assertThat(result.getDescription()).isEqualTo("body");
        assertThat(result.getExercises()).hasSize(1);
    }

    @Test
    void uploadMarkdown_emptyFile_throws() {
        MockMultipartFile file = new MockMultipartFile("file", "lesson.md", "text/markdown", new byte[0]);

        assertThatThrownBy(() -> lessonAdminService.uploadMarkdown(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void uploadMarkdown_nonMarkdownExtension_throws() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "lesson.txt", "text/plain", "content".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> lessonAdminService.uploadMarkdown(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(".md");
    }

    @Test
    void uploadMarkdownBulk_mixedResults_returnsBulkResult() {
        MockMultipartFile good = new MockMultipartFile(
                "files", "good.md", "text/markdown", "good".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile bad = new MockMultipartFile(
                "files", "bad.md", "text/markdown", "bad".getBytes(StandardCharsets.UTF_8));

        ExerciseRequest exerciseReq = new ExerciseRequest();
        exerciseReq.setQuestion("Q?");
        exerciseReq.setAnswerKey("A");
        exerciseReq.setDifficulty(DifficultyLevel.EASY);
        exerciseReq.setPoints(5);

        LessonCreateRequest parsed = new LessonCreateRequest();
        parsed.setTitle("T");
        parsed.setDifficulty(DifficultyLevel.EASY);
        parsed.setBaseXp(10);
        parsed.setExercises(List.of(exerciseReq));

        when(markdownParser.parse("good")).thenReturn(parsed);
        when(markdownParser.parse("bad")).thenThrow(new IllegalArgumentException("parse error"));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> {
            Lesson l = inv.getArgument(0);
            l.setId(UUID.randomUUID());
            return l;
        });
        when(exerciseRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        BulkLessonResult result = lessonAdminService.uploadMarkdownBulk(List.of(good, bad));

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.getErrors()).containsExactly("bad.md: parse error");
    }
}
