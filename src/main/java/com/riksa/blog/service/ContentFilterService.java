package com.riksa.blog.service;

import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class ContentFilterService {

    private static final Set<String> STEM_KEYWORDS = Set.of(
            // English
            "math", "physics", "chemistry", "biology", "code", "coding",
            "programming", "python", "java", "algorithm", "engineering",
            "science", "technology", "history", "computer", "data",
            "software", "hardware", "network", "database", "ai",
            "machine learning", "robot", "electric", "energy", "formula",
            // Khmer
            "គណិត", "រូបវិទ្យា", "គីមី", "ជីវវិទ្យា", "កូដ",
            "វិទ្យាសាស្ត្រ", "បច្ចេកវិទ្យា", "ប្រវត្តិ", "កុំព្យូទ័រ"
    );

    public void validate(String content) {
        String lower = content.toLowerCase();
        boolean hasKeyword = STEM_KEYWORDS.stream().anyMatch(lower::contains);
        if (!hasKeyword) {
            throw new IllegalArgumentException("Content must be related to STEM, Tech, or History");
        }
    }
}