package com.riksa.moderation;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies a deterministic, explainable moderation policy with no model or
 * network dependency. Text is NFKC-normalized, lowercased, stripped of common
 * invisible formatting characters (Khmer coeng U+17D2 stays), then runs of
 * three or more identical code points are reduced to two. Latin copies map
 * common leetspeak, fold a small set of cross-script lookalikes only in mixed
 * or lookalike-only tokens, and join single letters separated by non-letter
 * and non-digit characters.
 * English and Khmer-Latin terms match whole Unicode-letter tokens in those
 * copies; Khmer-script terms also match after separators between Khmer letters
 * are removed. A weight-3 term blocks by itself; weight-1 and weight-2 terms
 * add their weights once each. Spam signals add: 2 for more than 3 untrusted links; 1 for each
 * untrusted link, capped at 3; 3 for a caller-confirmed duplicate body; 1 when
 * more than 70% of at least 20 letters are uppercase; 1 for a letter, digit,
 * or emoji grapheme repeated at least 8 times; and 2 when untrusted URLs
 * occupy over 60% of the body with fewer than 40 characters remaining after
 * their text is removed. An account younger than the configured age adds 1
 * only when another signal has fired. Scores at the configured review and
 * block thresholds produce REVIEW and BLOCK; otherwise content is ALLOW. Empty
 * input is always ALLOW. Duplicate lookup is delegated to the caller and
 * receives {@link #normalizeForDuplicate(String)} of the body.
 */
@Service
public class ContentModerationService {

    private static final Pattern NON_LETTER = Pattern.compile("[^\\p{L}]+");
    private static final Pattern LETTER_TOKEN = Pattern.compile("\\p{L}+");
    private static final Pattern SQUASHED_LETTERS = Pattern.compile(
            "(?<![\\p{L}\\p{N}])(?:[\\p{L}][^\\p{L}\\p{N}]+)+[\\p{L}](?![\\p{L}\\p{N}])");
    private static final Pattern LINK = Pattern.compile(
            "(?i)(?:https?://|www\\.)[^\\s<>\\\"']+"
                    + "|(?<![\\p{L}\\p{N}@.-])(?:[a-z0-9-]+\\.)+[a-z]{2,}(?:/[^\\s<>\\\"']*)?");
    private static final Pattern REPEATED_GRAPHEME = Pattern.compile("(\\X)\\1{7,}");
    private static final Pattern KHMER_SEPARATORS = Pattern.compile(
            "(?<=[\\p{InKhmer}])[\\s\\p{Z}\\p{P}\\p{Cf}]+(?=[\\p{InKhmer}])");
    private static final String INVISIBLE_FORMATTING = "[\\u00AD\\u200B-\\u200F\\u202A-\\u202E"
            + "\\u2060\\u2066-\\u2069\\uFEFF]";
    private static final String LEET_SOURCE = "013457@$!";
    private static final String LEET_TARGET = "oieastasi";
    private static final Set<String> WEB_TLDS = Set.of("com", "net", "org", "edu", "gov", "io", "co", "kh",
            "info", "me", "app", "dev", "ly", "gl", "gg", "tv", "xyz", "site", "online", "link",
            "click", "top");
    private static final Set<String> CODE_EXTENSIONS = Set.of("py", "java", "js", "ts", "css", "html", "json",
            "md", "cpp", "c", "h", "cs", "kt", "dart", "go", "rs", "sql", "txt", "csv", "xml", "yml",
            "yaml", "sh", "rb", "php", "swift");

    private final WordListLoader wordLists;
    private final ModerationProperties properties;
    private final Map<String, Integer> latinTerms;

    public ContentModerationService(WordListLoader wordLists, ModerationProperties properties) {
        this.wordLists = wordLists;
        this.properties = properties;
        Map<String, Integer> allLatinTerms = new LinkedHashMap<>(wordLists.englishTerms());
        wordLists.khmerLatinTerms().forEach((term, weight) ->
                allLatinTerms.merge(term, weight, Math::max));
        this.latinTerms = Map.copyOf(allLatinTerms);
    }

    /**
     * Assesses title and body together. The duplicate predicate is called once
     * with the canonical body key and may perform the repository lookup; a null
     * predicate is treated as no duplicate signal. This method has no writes or
     * service-side persistence.
     */
    public ModerationResult assess(ModerationInput input, Predicate<String> duplicateCheck) {
        String title = safe(input.title());
        String body = safe(input.body());
        String combined = title + "\n" + body;
        String normalized = normalizeText(combined);
        if (normalized.isBlank()) {
            return new ModerationResult(ModerationVerdict.ALLOW, 0, List.of());
        }

        Map<ModerationReasonCode, ModerationReason> reasons = new LinkedHashMap<>();
        Set<String> matchedLatin = new LinkedHashSet<>();
        List<String> latinCopies = latinCopies(normalized);
        for (Map.Entry<String, Integer> entry : latinTerms.entrySet()) {
            if (latinCopies.stream().anyMatch(copy -> hasWholeTokenTerm(copy, entry.getKey()))) {
                matchedLatin.add(entry.getKey());
            }
        }

        Set<String> matchedKhmer = new LinkedHashSet<>();
        String khmerMatchingText = collapseKhmerSeparators(normalized);
        for (String term : wordLists.khmerTerms().keySet()) {
            if (khmerMatchingText.contains(collapseKhmerSeparators(term))) {
                matchedKhmer.add(term);
            }
        }

        int score = 0;
        boolean severeTerm = false;
        boolean profanityFound = false;
        for (String term : matchedLatin) {
            int weight = latinTerms.get(term);
            profanityFound = true;
            severeTerm |= weight == 3;
            if (weight < 3) {
                score += weight;
            }
        }
        for (String term : matchedKhmer) {
            int weight = wordLists.khmerTerms().get(term);
            profanityFound = true;
            severeTerm |= weight == 3;
            if (weight < 3) {
                score += weight;
            }
        }
        if (profanityFound) {
            addReason(reasons, ModerationReasonCode.PROFANITY,
                    "One or more configured content terms matched.");
        }

        String signalText = stripInvisible(Normalizer.normalize(combined, Normalizer.Form.NFKC));
        List<FoundLink> links = findLinks(signalText);
        List<FoundLink> untrustedLinks = links.stream()
                .filter(link -> !isAllowedHost(link.host()))
                .toList();
        if (untrustedLinks.size() > 3) {
            score += 2;
            addReason(reasons, ModerationReasonCode.SPAM_LINKS, "More than three links were found.");
        }

        int unknownLinks = untrustedLinks.size();
        if (unknownLinks > 0) {
            score += Math.min(unknownLinks, 3);
            addReason(reasons, ModerationReasonCode.UNKNOWN_LINK,
                    "One or more link hosts are not on the allowed list.");
        }

        String duplicateKey = normalizeForDuplicate(body);
        if (duplicateCheck != null && !duplicateKey.isBlank() && duplicateCheck.test(duplicateKey)) {
            score += 3;
            addReason(reasons, ModerationReasonCode.DUPLICATE,
                    "The same normalized body was posted recently.");
        }

        if (isShouting(signalText)) {
            score += 1;
            addReason(reasons, ModerationReasonCode.SHOUTING,
                    "The text contains unusually high capitalization.");
        }

        if (hasRepeatedGrapheme(signalText)) {
            score += 1;
            addReason(reasons, ModerationReasonCode.REPEATED_CHARS,
                    "A character or emoji is repeated many times.");
        }

        String normalizedBody = stripInvisible(Normalizer.normalize(body, Normalizer.Form.NFKC));
        List<FoundLink> bodyLinks = findLinks(normalizedBody).stream()
                .filter(link -> !isAllowedHost(link.host()))
                .toList();
        int bodyLength = normalizedBody.codePointCount(0, normalizedBody.length());
        int linkLength = bodyLinks.stream().mapToInt(link -> link.value().codePointCount(0, link.value().length())).sum();
        int nonLinkCharacters = bodyLength - linkLength;
        if (bodyLength > 0 && linkLength > bodyLength * 0.60 && nonLinkCharacters < 40) {
            score += 2;
            addReason(reasons, ModerationReasonCode.LINK_ONLY, "The body is mostly links.");
        }

        boolean anotherSignalFired = severeTerm || score > 0 || !reasons.isEmpty();
        if (anotherSignalFired && isNewAccount(input.accountCreatedAt())) {
            score += 1;
            addReason(reasons, ModerationReasonCode.NEW_ACCOUNT,
                    "The account is newer than the configured age limit.");
        }

        ModerationVerdict verdict = severeTerm || score >= properties.blockScore()
                ? ModerationVerdict.BLOCK
                : score >= properties.reviewScore() ? ModerationVerdict.REVIEW : ModerationVerdict.ALLOW;
        return new ModerationResult(verdict, score, List.copyOf(reasons.values()));
    }

    /**
     * Produces a stable body key by applying NFKC, lowercase, the specified
     * zero-width cleanup, a two-character repeat cap, and whitespace collapse.
     * Store this value or normalize stored text the same way before duplicate
     * comparison; it is intentionally independent of word-list contents.
     */
    public String normalizeForDuplicate(String text) {
        return normalizeText(safe(text)).replaceAll("\\s+", " ").trim();
    }

    /** Throws a client-mappable exception only when the supplied result is BLOCK. */
    public void requireNotBlocked(ModerationResult result) {
        if (result.verdict() == ModerationVerdict.BLOCK) {
            throw new ContentBlockedException(result.reasons());
        }
    }

    private List<String> latinCopies(String normalized) {
        String twoCollapsed = collapseRunsToTwo(normalized);
        String latinTwoCollapsed = mapLeetspeak(twoCollapsed);
        String latinOneCollapsed = mapLeetspeak(collapseAllRepeats(normalized));
        String squashed = squashSingleLetters(latinTwoCollapsed);
        String confusableLatin = mapLeetspeak(collapseRunsToTwo(foldConfusables(normalized)));
        String confusableSquashed = squashSingleLetters(confusableLatin);
        return List.of(twoCollapsed, latinTwoCollapsed, latinOneCollapsed, squashed,
                confusableLatin, confusableSquashed);
    }

    private String foldConfusables(String text) {
        Matcher matcher = LETTER_TOKEN.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String token = matcher.group();
            boolean hasLatin = false;
            boolean hasConfusableScript = false;
            boolean lookalikeOnly = true;
            for (int offset = 0; offset < token.length();) {
                int codePoint = token.codePointAt(offset);
                offset += Character.charCount(codePoint);
                Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
                hasLatin |= script == Character.UnicodeScript.LATIN;
                if (script == Character.UnicodeScript.CYRILLIC || script == Character.UnicodeScript.GREEK) {
                    if (confusableLatin(codePoint) == codePoint) {
                        lookalikeOnly = false;
                    } else {
                        hasConfusableScript = true;
                    }
                } else if (script != Character.UnicodeScript.LATIN) {
                    lookalikeOnly = false;
                }
            }

            boolean mixedScript = hasLatin && hasConfusableScript;
            boolean cyrillicOrGreekLookalikesOnly = !hasLatin && hasConfusableScript && lookalikeOnly;
            if (mixedScript || cyrillicOrGreekLookalikesOnly) {
                StringBuilder folded = new StringBuilder(token.length());
                token.codePoints().map(this::confusableLatin).forEach(folded::appendCodePoint);
                matcher.appendReplacement(result, Matcher.quoteReplacement(folded.toString()));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private int confusableLatin(int codePoint) {
        return switch (codePoint) {
            case '\u0430' -> 'a'; // Cyrillic а
            case '\u0435' -> 'e'; // Cyrillic е
            case '\u043E' -> 'o'; // Cyrillic о
            case '\u0440' -> 'p'; // Cyrillic р
            case '\u0441' -> 'c'; // Cyrillic с
            case '\u0443' -> 'y'; // Cyrillic у
            case '\u0445' -> 'x'; // Cyrillic х
            case '\u0456' -> 'i'; // Cyrillic і
            case '\u0458' -> 'j'; // Cyrillic ј
            case '\u043A' -> 'k'; // Cyrillic к
            case '\u043C' -> 'm'; // Cyrillic м
            case '\u0442' -> 't'; // Cyrillic т
            case '\u043D' -> 'h'; // Cyrillic н
            case '\u0432' -> 'b'; // Cyrillic в
            case '\u0455' -> 's'; // Cyrillic ѕ
            case '\u03BF' -> 'o'; // Greek ο
            case '\u03B1' -> 'a'; // Greek α
            case '\u03B5' -> 'e'; // Greek ε
            case '\u03B9' -> 'i'; // Greek ι
            case '\u03BA' -> 'k'; // Greek κ
            case '\u03BD' -> 'v'; // Greek ν
            case '\u03C1' -> 'p'; // Greek ρ
            case '\u03C4' -> 't'; // Greek τ
            case '\u03C5' -> 'u'; // Greek υ
            case '\u03C7' -> 'x'; // Greek χ
            default -> codePoint;
        };
    }

    private boolean hasWholeTokenTerm(String text, String term) {
        List<String> tokens = tokens(text);
        List<String> termTokens = tokens(term);
        if (termTokens.isEmpty() || termTokens.size() > tokens.size()) {
            return false;
        }
        for (int start = 0; start <= tokens.size() - termTokens.size(); start++) {
            if (tokens.subList(start, start + termTokens.size()).equals(termTokens)) {
                return true;
            }
        }
        return false;
    }

    private List<String> tokens(String text) {
        return Arrays.stream(NON_LETTER.split(text))
                .filter(token -> !token.isEmpty())
                .toList();
    }

    private String normalizeText(String text) {
        String normalized = stripInvisible(Normalizer.normalize(text, Normalizer.Form.NFKC))
                .toLowerCase(Locale.ROOT);
        return collapseRunsToTwo(normalized);
    }

    private String mapLeetspeak(String text) {
        StringBuilder mapped = new StringBuilder(text.length());
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            int replacement = LEET_SOURCE.indexOf(current);
            mapped.append(replacement >= 0 ? LEET_TARGET.charAt(replacement) : current);
        }
        return mapped.toString();
    }

    private String collapseRunsToTwo(String text) {
        StringBuilder result = new StringBuilder(text.length());
        int previous = -1;
        int runLength = 0;
        for (int offset = 0; offset < text.length();) {
            int current = text.codePointAt(offset);
            offset += Character.charCount(current);
            runLength = current == previous ? runLength + 1 : 1;
            if (runLength <= 2) {
                result.appendCodePoint(current);
            }
            previous = current;
        }
        return result.toString();
    }

    private String collapseAllRepeats(String text) {
        StringBuilder result = new StringBuilder(text.length());
        int previous = -1;
        for (int offset = 0; offset < text.length();) {
            int current = text.codePointAt(offset);
            offset += Character.charCount(current);
            if (current != previous) {
                result.appendCodePoint(current);
            }
            previous = current;
        }
        return result.toString();
    }

    private String squashSingleLetters(String text) {
        Matcher matcher = SQUASHED_LETTERS.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement(
                    matcher.group().replaceAll("[^\\p{L}\\p{N}]+", "")));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private List<FoundLink> findLinks(String text) {
        List<FoundLink> links = new ArrayList<>();
        Matcher matcher = LINK.matcher(text);
        while (matcher.find()) {
            String value = trimLinkPunctuation(matcher.group());
            if (value.isBlank() || !isRecognizedLink(value)) {
                continue;
            }
            String host = extractHost(value);
            links.add(new FoundLink(value, host));
        }
        return links;
    }

    private boolean isRecognizedLink(String value) {
        if (value.regionMatches(true, 0, "http://", 0, 7)
                || value.regionMatches(true, 0, "https://", 0, 8)
                || value.regionMatches(true, 0, "www.", 0, 4)) {
            return true;
        }
        String host = extractHost(value);
        int lastDot = host.lastIndexOf('.');
        if (lastDot < 0 || lastDot == host.length() - 1) {
            return false;
        }
        String suffix = host.substring(lastDot + 1);
        return !CODE_EXTENSIONS.contains(suffix) && WEB_TLDS.contains(suffix);
    }

    private String trimLinkPunctuation(String value) {
        int end = value.length();
        while (end > 0 && ".,!?:;)]}".indexOf(value.charAt(end - 1)) >= 0) {
            end--;
        }
        return value.substring(0, end);
    }

    private String extractHost(String value) {
        String candidate = value;
        if (!candidate.regionMatches(true, 0, "http://", 0, 7)
                && !candidate.regionMatches(true, 0, "https://", 0, 8)) {
            candidate = "https://" + candidate;
        }
        try {
            String host = new URI(candidate).getHost();
            return host == null ? "" : host.toLowerCase(Locale.ROOT);
        } catch (URISyntaxException ex) {
            return "";
        }
    }

    private boolean isAllowedHost(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        return properties.allowedLinkHosts().stream()
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .anyMatch(allowed -> host.equals(allowed) || host.endsWith("." + allowed));
    }

    private boolean isShouting(String text) {
        int letters = 0;
        int uppercase = 0;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (Character.isLetter(codePoint)) {
                letters++;
                if (Character.isUpperCase(codePoint)) {
                    uppercase++;
                }
            }
        }
        return letters >= 20 && uppercase / (double) letters > 0.70;
    }

    private boolean hasRepeatedGrapheme(String text) {
        Matcher matcher = REPEATED_GRAPHEME.matcher(text);
        while (matcher.find()) {
            if (isLetterDigitOrEmojiGrapheme(matcher.group(1))) {
                return true;
            }
        }
        return false;
    }

    private boolean isLetterDigitOrEmojiGrapheme(String grapheme) {
        return grapheme.codePoints().anyMatch(codePoint -> Character.isLetterOrDigit(codePoint)
                || isEmojiCodePoint(codePoint));
    }

    private boolean isEmojiCodePoint(int codePoint) {
        if (codePoint >= 0x1F1E6 && codePoint <= 0x1F1FF) {
            return true;
        }
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        return block == Character.UnicodeBlock.EMOTICONS
                || block == Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS
                || block == Character.UnicodeBlock.DINGBATS
                || block == Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS
                || block == Character.UnicodeBlock.TRANSPORT_AND_MAP_SYMBOLS
                || block == Character.UnicodeBlock.SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS
                || block == Character.UnicodeBlock.SYMBOLS_AND_PICTOGRAPHS_EXTENDED_A;
    }

    private String collapseKhmerSeparators(String text) {
        return KHMER_SEPARATORS.matcher(text).replaceAll("");
    }

    private boolean isNewAccount(OffsetDateTime createdAt) {
        if (createdAt == null) {
            return false;
        }
        Duration age = Duration.between(createdAt, OffsetDateTime.now());
        return age.compareTo(properties.newAccountAge()) < 0;
    }

    private void addReason(Map<ModerationReasonCode, ModerationReason> reasons,
                           ModerationReasonCode code, String detail) {
        reasons.putIfAbsent(code, new ModerationReason(code, detail));
    }

    private String stripInvisible(String text) {
        return text.replaceAll(INVISIBLE_FORMATTING, "");
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }

    private record FoundLink(String value, String host) {
    }
}
