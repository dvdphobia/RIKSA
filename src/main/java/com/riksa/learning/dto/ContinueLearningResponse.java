package com.riksa.learning.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The learner's best next lesson. A null lesson with allCompleted=true is a
 * deliberate terminal state, not a missing-resource error.
 */
@Getter
@AllArgsConstructor
public class ContinueLearningResponse {
    private final boolean allCompleted;
    private final LearnerLessonProgressResponse lesson;
}
