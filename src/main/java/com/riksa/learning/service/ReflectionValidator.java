package com.riksa.learning.service;

/**
 * Validates a user's reflection text submitted upon lesson completion
 * and produces a score. Implementations may use simple heuristics or
 * call an external AI service.
 */
public interface ReflectionValidator {

    /**
     * Validate the reflection text and return a non-negative score.
     *
     * @param reflectionText the user's reflection (never null, may be blank)
     * @return a score from 0 upwards; higher means a better reflection
     * @throws IllegalArgumentException if the reflection is rejected entirely
     *         (e.g. empty, too short)
     */
    int validate(String reflectionText);
}