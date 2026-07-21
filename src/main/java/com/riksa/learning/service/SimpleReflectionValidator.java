package com.riksa.learning.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Heuristic validator that scores a reflection based on word count.
 * <ul>
 *   <li>Below {@code minWords} → rejected (IllegalArgumentException)</li>
 *   <li>Up to {@code goodWords} → 1 point</li>
 *   <li>Up to {@code greatWords} → 2 points</li>
 *   <li>Above {@code greatWords} → 3 points</li>
 * </ul>
 * Defaults: 10 / 30 / 60 words. Overridable via application properties
 * ({@code rksa.learning.reflection.min-words} etc.).
 */
@Component
public class SimpleReflectionValidator implements ReflectionValidator {

    private final int minWords;
    private final int goodWords;
    private final int greatWords;

    public SimpleReflectionValidator(
            @Value("${rksa.learning.reflection.min-words:10}") int minWords,
            @Value("${rksa.learning.reflection.good-words:30}") int goodWords,
            @Value("${rksa.learning.reflection.great-words:60}") int greatWords
    ) {
        this.minWords = minWords;
        this.goodWords = goodWords;
        this.greatWords = greatWords;
    }

    @Override
    public int validate(String reflectionText) {
        if (reflectionText == null || reflectionText.isBlank()) {
            throw new IllegalArgumentException("Reflection text must not be empty");
        }

        int words = countWords(reflectionText);
        if (words < minWords) {
            throw new IllegalArgumentException(
                    "Reflection is too short: at least " + minWords + " words required, got " + words);
        }

        if (words >= greatWords) return 3;
        if (words >= goodWords) return 2;
        return 1;
    }

    private int countWords(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return 0;
        return trimmed.split("\\s+").length;
    }
}