package com.riksa.learning.service;

import java.util.UUID;

/** Raised when a student tries to bypass roadmap progression. */
public class LessonLockedException extends IllegalStateException {
    public LessonLockedException(UUID lessonId) {
        super("Lesson is locked until the previous lesson is completed: " + lessonId);
    }
}
