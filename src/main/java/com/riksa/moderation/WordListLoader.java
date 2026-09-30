package com.riksa.moderation;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Loads the English, Khmer, and Khmer-Latin rule lists once when Spring starts.
 * Each non-comment line must contain one non-empty term, one tab, and a weight
 * from 1 to 3. Invalid lines stop application startup so a typo cannot silently
 * weaken moderation. Duplicate normalized terms are rejected.
 */
@Component
public class WordListLoader {

    private final Map<String, Integer> englishTerms;
    private final Map<String, Integer> khmerTerms;
    private final Map<String, Integer> khmerLatinTerms;

    public WordListLoader() {
        this(new ClassPathResource("moderation/en.txt"),
                new ClassPathResource("moderation/km.txt"),
                new ClassPathResource("moderation/km-latin.txt"));
    }

    WordListLoader(Resource english, Resource khmer, Resource khmerLatin) {
        englishTerms = load(english);
        khmerTerms = load(khmer);
        khmerLatinTerms = load(khmerLatin);
    }

    /** Returns the immutable English term-to-weight map. */
    Map<String, Integer> englishTerms() {
        return englishTerms;
    }

    /** Returns the immutable Khmer-script substring term-to-weight map. */
    Map<String, Integer> khmerTerms() {
        return khmerTerms;
    }

    /** Returns the immutable Latin-token Khmer term-to-weight map. */
    Map<String, Integer> khmerLatinTerms() {
        return khmerLatinTerms;
    }

    static Map<String, Integer> parse(Reader source, String sourceName) {
        Map<String, Integer> terms = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(source)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                String[] fields = line.split("\\t", -1);
                if (fields.length != 2) {
                    throw malformed(sourceName, lineNumber, "expected term<TAB>weight");
                }

                String term = normalizeTerm(fields[0]);
                if (term.isEmpty()) {
                    throw malformed(sourceName, lineNumber, "term must not be empty");
                }

                final int weight;
                try {
                    weight = Integer.parseInt(fields[1].trim());
                } catch (NumberFormatException ex) {
                    throw malformed(sourceName, lineNumber, "weight must be 1, 2, or 3");
                }
                if (weight < 1 || weight > 3) {
                    throw malformed(sourceName, lineNumber, "weight must be 1, 2, or 3");
                }
                if (terms.putIfAbsent(term, weight) != null) {
                    throw malformed(sourceName, lineNumber, "duplicate normalized term");
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read moderation word list " + sourceName, ex);
        }
        return Map.copyOf(terms);
    }

    private Map<String, Integer> load(Resource resource) {
        try {
            return parse(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8),
                    resource.getFilename());
        } catch (IOException ex) {
            throw new IllegalStateException("Could not load moderation word list " + resource.getFilename(), ex);
        }
    }

    private static IllegalStateException malformed(String sourceName, int lineNumber, String reason) {
        return new IllegalStateException("Malformed moderation word list " + sourceName
                + " at line " + lineNumber + ": " + reason);
    }

    private static String normalizeTerm(String term) {
        return collapseRunsToTwo(Normalizer.normalize(term.trim(), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT));
    }

    private static String collapseRunsToTwo(String text) {
        StringBuilder result = new StringBuilder(text.length());
        int previous = -1;
        int runLength = 0;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            offset += Character.charCount(codePoint);
            runLength = codePoint == previous ? runLength + 1 : 1;
            if (runLength <= 2) {
                result.appendCodePoint(codePoint);
            }
            previous = codePoint;
        }
        return result.toString();
    }
}
