package com.riksa.blog.util;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class BadWordFilter {

    private static final Set<String> BAD_WORDS = Set.of(
            // English
            "spam", "scam", "fake", "hate",
            // Khmer — add real ones as needed
            "ជេរ", "បង្អាប់"
    );

    public void validate(String content) {
        String lower = content.toLowerCase();
        boolean hasBadWord = BAD_WORDS.stream().anyMatch(lower::contains);
        if (hasBadWord) {
            throw new IllegalArgumentException("Content contains prohibited words");
        }
    }
}