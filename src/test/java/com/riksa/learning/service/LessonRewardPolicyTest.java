package com.riksa.learning.service;

import com.riksa.common.enums.DifficultyLevel;
import com.riksa.learning.entity.Exercise;
import com.riksa.learning.entity.Lesson;
import com.riksa.learning.entity.RoadmapUnit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LessonRewardPolicyTest {

    @Test
    void roadmapRewardIncludesLengthDifficultyAndExercisePoints() {
        Lesson lesson = new Lesson();
        lesson.setRoadmapUnit(new RoadmapUnit());
        lesson.setDifficulty(DifficultyLevel.MEDIUM);
        lesson.setEstimatedMinutes(10);
        lesson.setBaseXp(20);
        Exercise first = new Exercise();
        first.setPoints(5);
        Exercise second = new Exercise();
        second.setPoints(10);

        assertThat(new LessonRewardPolicy().potentialXp(lesson, List.of(first, second)))
                .isEqualTo(55);
    }
}
