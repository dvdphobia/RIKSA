package com.riksa.learning.service;

import com.riksa.common.enums.DifficultyLevel;
import com.riksa.learning.entity.Exercise;
import com.riksa.learning.entity.Lesson;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LessonRewardPolicy {

    public int potentialXp(Lesson lesson, List<Exercise> exercises) {
        int durationReward = lesson.getRoadmapUnit() == null
                ? 0
                : Math.max(0, lesson.getEstimatedMinutes()) * difficultyMultiplier(lesson.getDifficulty());
        int exerciseReward = exercises.stream().mapToInt(Exercise::getPoints).sum();
        return Math.max(0, lesson.getBaseXp()) + durationReward + exerciseReward;
    }

    private int difficultyMultiplier(DifficultyLevel difficulty) {
        if (difficulty == null) return 1;
        return switch (difficulty) {
            case EASY -> 1;
            case MEDIUM -> 2;
            case HARD -> 3;
            case DIFFICULT -> 4;
        };
    }
}
