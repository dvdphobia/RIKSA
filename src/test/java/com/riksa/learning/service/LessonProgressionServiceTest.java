package com.riksa.learning.service;

import com.riksa.learning.dto.LessonProgressState;
import com.riksa.learning.entity.Lesson;
import com.riksa.learning.entity.LessonCompletion;
import com.riksa.learning.repository.LessonCompletionRepository;
import com.riksa.learning.repository.LessonRepository;
import com.riksa.social.entity.Category;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonProgressionServiceTest {

    @Mock LessonRepository lessonRepository;
    @Mock LessonCompletionRepository completionRepository;

    @Test
    void state_firstIncompleteAvailable_andLaterLessonsLocked() {
        UUID userId = UUID.randomUUID();
        Lesson first = lesson(1), second = lesson(2), third = lesson(3);
        when(lessonRepository.findByCategoryIdOrderByPositionAsc(first.getCategory().getId()))
                .thenReturn(List.of(first, second, third));
        when(completionRepository.findByUserIdAndLessonIdIn(userId,
                List.of(first.getId(), second.getId(), third.getId()))).thenReturn(List.of());

        LessonProgressionService service = new LessonProgressionService(lessonRepository, completionRepository);

        assertThat(service.getState(userId, first)).isEqualTo(LessonProgressState.AVAILABLE);
        assertThat(service.getState(userId, second)).isEqualTo(LessonProgressState.LOCKED);
        assertThat(service.getState(userId, third)).isEqualTo(LessonProgressState.LOCKED);
    }

    @Test
    void state_completionUnlocksNext_andKeepsCompletedState() {
        UUID userId = UUID.randomUUID();
        Lesson first = lesson(1), second = lesson(2), third = lesson(3);
        LessonCompletion completion = new LessonCompletion();
        completion.setLessonId(first.getId());
        when(lessonRepository.findByCategoryIdOrderByPositionAsc(first.getCategory().getId()))
                .thenReturn(List.of(first, second, third));
        when(completionRepository.findByUserIdAndLessonIdIn(userId,
                List.of(first.getId(), second.getId(), third.getId()))).thenReturn(List.of(completion));

        LessonProgressionService service = new LessonProgressionService(lessonRepository, completionRepository);

        assertThat(service.getState(userId, first)).isEqualTo(LessonProgressState.COMPLETED);
        assertThat(service.getState(userId, second)).isEqualTo(LessonProgressState.AVAILABLE);
        assertThat(service.getState(userId, third)).isEqualTo(LessonProgressState.LOCKED);
    }

    @Test
    void requireAccessible_lockedLessonThrowsConflictException() {
        UUID userId = UUID.randomUUID();
        Lesson first = lesson(1), second = lesson(2);
        when(lessonRepository.findById(second.getId())).thenReturn(java.util.Optional.of(second));
        when(lessonRepository.findByCategoryIdOrderByPositionAsc(second.getCategory().getId()))
                .thenReturn(List.of(first, second));
        when(completionRepository.findByUserIdAndLessonIdIn(userId,
                List.of(first.getId(), second.getId()))).thenReturn(List.of());

        LessonProgressionService service = new LessonProgressionService(lessonRepository, completionRepository);

        assertThatThrownBy(() -> service.requireAccessible(userId, second.getId()))
                .isInstanceOf(LessonLockedException.class);
    }

    private Lesson lesson(int position) {
        Category category = new Category();
        category.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        Lesson lesson = new Lesson();
        lesson.setId(UUID.randomUUID());
        lesson.setCategory(category);
        lesson.setPosition(position);
        return lesson;
    }
}
