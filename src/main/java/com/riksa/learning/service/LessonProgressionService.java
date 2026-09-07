package com.riksa.learning.service;

import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.learning.dto.LessonProgressState;
import com.riksa.learning.entity.Lesson;
import com.riksa.learning.repository.LessonCompletionRepository;
import com.riksa.learning.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Computes category-local lesson availability from ordered lessons and completions. */
@Service
@RequiredArgsConstructor
public class LessonProgressionService {

    private final LessonRepository lessonRepository;
    private final LessonCompletionRepository lessonCompletionRepository;

    @Transactional(readOnly = true)
    public LessonProgressState getState(UUID userId, Lesson lesson) {
        if (userId == null) {
            return LessonProgressState.AVAILABLE;
        }

        // Standalone or demo lessons (e.g. legacy/showcase lessons with position > 50) are not locked by curriculum track
        if (lesson.getPosition() > 50) {
            boolean isCompleted = lessonCompletionRepository.existsByUserIdAndLessonId(userId, lesson.getId());
            return isCompleted ? LessonProgressState.COMPLETED : LessonProgressState.AVAILABLE;
        }

        List<Lesson> ordered = lesson.getCategory() == null
                ? lessonRepository.findByCategoryIsNullOrderByPositionAsc()
                : lessonRepository.findByCategoryIdOrderByPositionAsc(lesson.getCategory().getId());

        List<Lesson> track = ordered.stream()
                .filter(l -> l.getPosition() <= 50)
                .toList();

        Set<UUID> completed = new HashSet<>(lessonCompletionRepository
                .findByUserIdAndLessonIdIn(userId, track.stream().map(Lesson::getId).toList())
                .stream().map(c -> c.getLessonId()).toList());

        if (completed.contains(lesson.getId())) return LessonProgressState.COMPLETED;

        for (Lesson candidate : track) {
            if (completed.contains(candidate.getId())) {
                continue;
            }
            return candidate.getId().equals(lesson.getId())
                    ? LessonProgressState.AVAILABLE
                    : LessonProgressState.LOCKED;
        }
        return LessonProgressState.LOCKED;
    }

    @Transactional(readOnly = true)
    public Lesson requireAccessible(UUID userId, UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found: " + lessonId));
        if (getState(userId, lesson) == LessonProgressState.LOCKED) {
            throw new LessonLockedException(lessonId);
        }
        return lesson;
    }
}
