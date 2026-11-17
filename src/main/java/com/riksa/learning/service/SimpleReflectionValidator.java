package com.riksa.learning.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Requires a substantial reflection, then scores it based on word count.
 * Defaults require 12 non-whitespace characters and 3 words. Text with at
 * least half its compact characters in the Khmer block needs no word minimum.
 * <ul>
 *   <li>Below {@code goodWords} → 1 point</li>
 *   <li>At least {@code goodWords} → 2 points</li>
 *   <li>At least {@code greatWords} → 3 points</li>
 * </ul>
 * Scoring defaults are 15 / 30 words. All thresholds are configurable under
 * {@code rksa.learning.reflection}, including {@code min-characters} and {@code min-words}.
 */
@Component
public class SimpleReflectionValidator implements ReflectionValidator {

    private final int minCharacters;
    private final int minWords;
    private final int goodWords;
    private final int greatWords;

    public SimpleReflectionValidator(
            @Value("${rksa.learning.reflection.min-characters:12}") int minCharacters,
            @Value("${rksa.learning.reflection.min-words:3}") int minWords,
            @Value("${rksa.learning.reflection.good-words:15}") int goodWords,
            @Value("${rksa.learning.reflection.great-words:30}") int greatWords
    ) {
        this.minCharacters = minCharacters;
        this.minWords = minWords;
        this.goodWords = goodWords;
        this.greatWords = greatWords;
    }

    @Override
    public int validate(String reflectionText) {
        String text = reflectionText == null ? "" : reflectionText.replaceAll("(?U)\\s+", " ").trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Reflection text must not be empty");
        }

        String compact = text.replace(" ", "");
        if (compact.length() < minCharacters) {
            throw new IllegalArgumentException(
                    "Reflection is too short: at least " + minCharacters + " non-whitespace characters required");
        }

        long khmerCharacters = compact.codePoints().filter(c -> c >= 0x1780 && c <= 0x17FF).count();
        int words = text.split(" ").length;
        if (khmerCharacters * 2 < compact.length() && words < minWords) {
            throw new IllegalArgumentException(
                    "Reflection is too short: at least " + minWords + " words required, got " + words);
        }

        if (words >= greatWords) return 3;
        if (words >= goodWords) return 2;
        return 1;
    }
}
