package com.riksa.moderation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContentModerationServiceTest {

    private ContentModerationService service;

    @BeforeEach
    void setUp() {
        service = new ContentModerationService(new WordListLoader(), properties());
    }

    @Test
    void mapsLeetspeakAndRemovesZeroWidthCharacters() {
        assertBlocked("sh!t");
        assertBlocked("sh\u200Bit");
        assertBlocked("sh\u2060it");
        assertBlocked("sh\uFEFFit");
    }

    @Test
    void joinsSingleLettersSeparatedBySpacesOrDots() {
        assertBlocked("s.h.i.t");
        assertBlocked("s h i t");
    }

    @Test
    void joinsSingleLettersSeparatedByPunctuationSymbolsEmojiAndFormattingCharacters() {
        for (String body : List.of(
                "s-h-i-t", "f_u_c_k", "f*u*c*k", "f/u/c/k", "s,h,i,t", "s~h~i~t",
                "s\u00ADh\u00ADi\u00ADt", "s🔥h🔥i🔥t")) {
            assertBlocked(body);
        }
        for (char formatting : new char[]{'\u200E', '\u200F', '\u202A', '\u202B', '\u202C',
                '\u202D', '\u202E', '\u2066', '\u2067', '\u2068', '\u2069'}) {
            assertBlocked("sh" + formatting + "it");
        }
    }

    @Test
    void foldsConfusablesOnlyForMixedOrLookalikeOnlyTokens() {
        for (String body : List.of("sh\u0456t", "b\u0430stard", "fuc\u043A", "f\u03C5c\u043A",
                "\u0455\u043D\u0456\u0442")) {
            assertBlocked(body);
        }

        assertThat(assess("", "привет, это обычный русский текст").verdict())
                .isEqualTo(ModerationVerdict.ALLOW);
        assertThat(assess("", "καλημέρα είναι μια συνηθισμένη ελληνική λέξη").verdict())
                .isEqualTo(ModerationVerdict.ALLOW);
    }

    @Test
    void oneCharacterCollapseFindsTermsWithRepeatedLetters() {
        assertBlocked("shiiiiiiit");
    }

    @Test
    void duplicateKeyIsStableAndKeepsKhmerCoeng() {
        assertThat(service.normalizeForDuplicate("  HELLO\u200B   world!!! "))
                .isEqualTo("hello world!!");
        assertThat(service.normalizeForDuplicate("x".repeat(5))).isEqualTo("xx");
        String khmerWithCoeng = "ក\u17D2ក";
        assertThat(service.normalizeForDuplicate(khmerWithCoeng)).contains("\u17D2");
    }

    @Test
    void ordinaryStudentQuestionsPublish() {
        String[] questions = {
                "How do I stay motivated before the national exam?",
                "Is this a scam? A site says I won free exam answers.",
                "I hate chemistry but I need it. Any tips?",
                "Hello everyone, how do I assess my weak topics?",
                "Which catalyst can retard the reaction rate?",
                "Why do rivers need a dyke in the rainy season?",
                "Summa cum laude means graduating with the highest honours.",
                "How is semen produced in human reproduction? (Biology 12)",
                "Shell, hello, Dickens, cockpit, Scunthorpe and classic are fine.",
                "Solve for x: a b c are sides of a triangle, find the area.",
                "a b c are sides",
                "x - y = 3",
                "Q: A or B?",
        };
        for (String question : questions) {
            assertThat(assess(null, question).verdict())
                    .as(question)
                    .isEqualTo(ModerationVerdict.ALLOW);
        }
    }

    @Test
    void historyOfTheKhmerRougeIsHeldForReviewNotBlocked() {
        ModerationResult result = assess("Khmer Rouge history",
                "Survivors described torture and rape in the prisons.");

        assertThat(result.verdict()).isEqualTo(ModerationVerdict.REVIEW);
    }

    @Test
    void latinTermsUseWholeTokensAndAvoidShortWordFalsePositives() {
        ModerationResult result = assess("Scunthorpe class assess", "Beware of scam links");

        assertThat(result.verdict()).isEqualTo(ModerationVerdict.ALLOW);
        assertThat(result.score()).isZero();
        assertThat(result.reasons()).isEmpty();
    }

    @Test
    void severeWordBlocksByItselfAndDetailsNeverRevealTheMatchedWord() {
        ModerationResult result = assess("", "shit");

        assertThat(result.verdict()).isEqualTo(ModerationVerdict.BLOCK);
        assertThat(result.reasons()).extracting(ModerationReason::code)
                .containsExactly(ModerationReasonCode.PROFANITY);
        assertThat(result.reasons()).allSatisfy(reason ->
                assertThat(reason.detail().toLowerCase()).doesNotContain("shit"));
    }

    @Test
    void khmerTermsMatchAsSubstrings() {
        ContentModerationService khmerService = serviceWithLists("", "ខ្មែរ\t3\n", "");

        ModerationResult result = khmerService.assess(input("", "កូនខ្មែរល្អ"), ignored -> false);

        assertThat(result.verdict()).isEqualTo(ModerationVerdict.BLOCK);
        assertThat(result.reasons()).extracting(ModerationReason::code)
                .contains(ModerationReasonCode.PROFANITY);
    }

    @Test
    void khmerTermsMatchAcrossSpacesPunctuationAndZeroWidthCharacters() {
        String termWithCoeng = "\u1780\u17D2\u179A";
        ContentModerationService khmerService = serviceWithLists("", termWithCoeng + "\t3\n", "");

        ModerationResult result = khmerService.assess(
                input("", "\u1780\u17D2 \u200B.\u179A"), ignored -> false);

        assertThat(result.verdict()).isEqualTo(ModerationVerdict.BLOCK);
        assertThat(result.reasons()).extracting(ModerationReason::code)
                .contains(ModerationReasonCode.PROFANITY);
    }

    @Test
    void moreThanThreeLinksAddsSpamPoints() {
        ModerationResult result = assess("", "https://one.invalid/a https://two.invalid/b "
                + "www.three.invalid/x four.com/y");

        assertThat(result.reasons()).extracting(ModerationReason::code)
                .contains(ModerationReasonCode.SPAM_LINKS, ModerationReasonCode.UNKNOWN_LINK);
        assertThat(result.score()).isGreaterThanOrEqualTo(5);
        assertThat(result.verdict()).isEqualTo(ModerationVerdict.BLOCK);
    }

    @Test
    void unknownLinkAddsOnePointPerLinkUpToThreeAndAllowedSubdomainsAreTrusted() {
        String context = "This classroom question gives enough useful background for a student to review. ";
        ModerationResult unknown = assess("", context + "https://outside.example/path");
        ModerationResult allowed = assess("", context + "https://video.youtube.com/lesson");

        assertThat(unknown.score()).isEqualTo(1);
        assertThat(unknown.reasons()).extracting(ModerationReason::code)
                .containsExactly(ModerationReasonCode.UNKNOWN_LINK);
        assertThat(allowed.score()).isZero();
        assertThat(allowed.reasons()).isEmpty();
    }

    @Test
    void duplicateCheckReceivesCanonicalBodyAndAddsThreePoints() {
        AtomicReference<String> checkedKey = new AtomicReference<>();
        ModerationResult result = service.assess(input("a title", "  SAME\u200B   body  "),
                key -> {
                    checkedKey.set(key);
                    return true;
                });

        assertThat(checkedKey.get()).isEqualTo("same body");
        assertThat(result.score()).isEqualTo(3);
        assertThat(result.verdict()).isEqualTo(ModerationVerdict.REVIEW);
        assertThat(result.reasons()).extracting(ModerationReason::code)
                .containsExactly(ModerationReasonCode.DUPLICATE);
    }

    @Test
    void shoutingRequiresTwentyLettersAndMoreThanSeventyPercentCapitals() {
        ModerationResult shouting = assess("", "THIS SENTENCE HAS ENOUGH CAPITAL LETTERS");
        ModerationResult shortText = assess("", "ALL CAPS");
        ModerationResult mixedText = assess("", "This sentence has enough ordinary lower case letters");

        assertThat(shouting.reasons()).extracting(ModerationReason::code)
                .contains(ModerationReasonCode.SHOUTING);
        assertThat(shortText.reasons()).extracting(ModerationReason::code)
                .doesNotContain(ModerationReasonCode.SHOUTING);
        assertThat(mixedText.reasons()).extracting(ModerationReason::code)
                .doesNotContain(ModerationReasonCode.SHOUTING);
    }

    @Test
    void repeatedCharactersAndEmojiAreDetectedByGrapheme() {
        ModerationResult repeatedLetter = assess("", "z".repeat(8));
        ModerationResult repeatedEmoji = assess("", "🙂".repeat(8));

        assertThat(repeatedLetter.reasons()).extracting(ModerationReason::code)
                .contains(ModerationReasonCode.REPEATED_CHARS);
        assertThat(repeatedEmoji.reasons()).extracting(ModerationReason::code)
                .contains(ModerationReasonCode.REPEATED_CHARS);
    }

    @Test
    void linkDominatedBodyAddsLinkOnlySignal() {
        ModerationResult result = assess("", "https://outside.example/a/long/path/with/many/characters"
                + "?source=classroom&chapter=three");

        assertThat(result.reasons()).extracting(ModerationReason::code)
                .contains(ModerationReasonCode.LINK_ONLY);
    }

    @Test
    void filenamesWithCodeExtensionsAreNotLinks() {
        ModerationResult result = assess("", "I have an issue between main.py, utils.py, config.py, and test.py. "
                + "Why does the import fail?");

        assertThat(result.verdict()).isEqualTo(ModerationVerdict.ALLOW);
        assertThat(result.score()).isZero();
        assertThat(result.reasons()).isEmpty();
    }

    @Test
    void allowlistedYoutubeLinksDoNotTriggerLinkOnlyOrSpamSignals() {
        ModerationResult result = assess("", "Here are two good physics lessons: "
                + "https://www.youtube.com/watch?v=12345678901 "
                + "https://www.youtube.com/watch?v=98765432109");

        assertThat(result.verdict()).isEqualTo(ModerationVerdict.ALLOW);
        assertThat(result.score()).isZero();
        assertThat(result.reasons()).isEmpty();
    }

    @Test
    void moreThanThreeAllowlistedLearningLinksDoNotCountAsSpamLinks() {
        ModerationResult result = assess("", "Study these references: https://www.youtube.com/a "
                + "https://youtu.be/b https://coursera.org/c https://sub.school.edu.kh/d");

        assertThat(result.verdict()).isEqualTo(ModerationVerdict.ALLOW);
        assertThat(result.reasons()).isEmpty();
    }

    @Test
    void addedCambodianAndLearningHostsAreAllowedIncludingEduKhSubdomains() {
        for (String host : List.of("moeys.gov.kh", "school.edu.kh", "coursera.org", "edx.org", "mit.edu",
                "stackoverflow.com", "geeksforgeeks.org", "w3schools.com", "desmos.com", "geogebra.org",
                "phet.colorado.edu")) {
            ModerationResult result = assess("", "A classroom reference: https://" + host + "/lesson");

            assertThat(result.score()).as(host).isZero();
            assertThat(result.reasons()).as(host).isEmpty();
        }
    }

    @Test
    void markdownRulesAndFillInLinesDoNotCountAsRepeatedCharacterSignals() {
        for (String marker : List.of("-", "=", "_", ".", "*", "~")) {
            ModerationInput shortMarkerNotes = input("", "Summary of Chapter 2\n"
                    + marker.repeat(6) + "\nKey formulas:", OffsetDateTime.now().minusHours(1));
            assertThat(service.assess(shortMarkerNotes, ignored -> false).reasons())
                    .as(marker).extracting(ModerationReason::code)
                    .doesNotContain(ModerationReasonCode.REPEATED_CHARS);

            ModerationInput youngStudentNotes = input("", "Summary of Chapter 2\n"
                    + marker.repeat(8) + "\nKey formulas:", OffsetDateTime.now().minusHours(1));

            ModerationResult result = service.assess(youngStudentNotes, ignored -> false);

            assertThat(result.verdict()).as(marker).isEqualTo(ModerationVerdict.ALLOW);
            assertThat(result.reasons()).as(marker).extracting(ModerationReason::code)
                    .doesNotContain(ModerationReasonCode.REPEATED_CHARS, ModerationReasonCode.NEW_ACCOUNT);
        }
    }

    @Test
    void mildPhraseAtReviewThresholdAndCombinedSignalsAtBlockThreshold() {
        ModerationResult review = assess("", "Free money is a phrase to flag");
        ModerationResult block = assess("", "Read these classroom resources for your lesson and compare each topic carefully. "
                + "https://one.invalid https://two.invalid https://three.invalid https://four.invalid");

        assertThat(review.score()).isEqualTo(2);
        assertThat(review.verdict()).isEqualTo(ModerationVerdict.REVIEW);
        assertThat(block.score()).isEqualTo(5);
        assertThat(block.verdict()).isEqualTo(ModerationVerdict.BLOCK);
    }

    @Test
    void newAccountOnlyAddsPointsWhenAnotherSignalFired() {
        ModerationInput youngBlank = input("", "", OffsetDateTime.now().minusHours(1));
        ModerationInput youngClean = input("", "A clear educational question.",
                OffsetDateTime.now().minusHours(1));
        ModerationInput youngMild = input("", "free money", OffsetDateTime.now().minusHours(1));

        ModerationResult blankResult = service.assess(youngBlank, ignored -> false);
        ModerationResult cleanResult = service.assess(youngClean, ignored -> false);
        ModerationResult mildResult = service.assess(youngMild, ignored -> false);

        assertThat(blankResult.reasons()).isEmpty();
        assertThat(cleanResult.reasons()).extracting(ModerationReason::code)
                .doesNotContain(ModerationReasonCode.NEW_ACCOUNT);
        assertThat(mildResult.score()).isEqualTo(3);
        assertThat(mildResult.reasons()).extracting(ModerationReason::code)
                .contains(ModerationReasonCode.NEW_ACCOUNT);
    }

    @Test
    void emptyInputAlwaysAllowsWithoutCallingDuplicateCheck() {
        ModerationResult result = service.assess(input(" \u200B", "  "), key -> {
            throw new AssertionError("Blank content must not query duplicates");
        });

        assertThat(result).isEqualTo(new ModerationResult(ModerationVerdict.ALLOW, 0, List.of()));
    }

    @Test
    void requireNotBlockedThrowsOnlyForBlockedResults() {
        ModerationResult blocked = assess("", "shit");
        ModerationResult allowed = assess("", "A school question");

        assertThatThrownBy(() -> service.requireNotBlocked(blocked))
                .isInstanceOf(ContentBlockedException.class);
        service.requireNotBlocked(allowed);
    }

    private void assertBlocked(String body) {
        assertThat(assess("", body).verdict()).as(body).isEqualTo(ModerationVerdict.BLOCK);
    }

    private ModerationResult assess(String title, String body) {
        return service.assess(input(title, body), ignored -> false);
    }

    private ModerationInput input(String title, String body) {
        return input(title, body, OffsetDateTime.now().minusDays(2));
    }

    private ModerationInput input(String title, String body, OffsetDateTime createdAt) {
        return new ModerationInput(UUID.randomUUID(), createdAt,
                ModerationSurface.POST, title, body);
    }

    private ModerationProperties properties() {
        return new ModerationProperties(5, 2,
                List.of("youtube.com", "youtu.be", "wikipedia.org", "khanacademy.org", "github.com", "riksa.app",
                        "moeys.gov.kh", "edu.kh", "coursera.org", "edx.org", "mit.edu", "stackoverflow.com",
                        "geeksforgeeks.org", "w3schools.com", "desmos.com", "geogebra.org", "phet.colorado.edu"),
                java.time.Duration.ofHours(24));
    }

    private ContentModerationService serviceWithLists(String english, String khmer, String khmerLatin) {
        WordListLoader loader = new WordListLoader(resource(english), resource(khmer), resource(khmerLatin));
        return new ContentModerationService(loader, properties());
    }

    private ByteArrayResource resource(String text) {
        return new ByteArrayResource(text.getBytes(StandardCharsets.UTF_8));
    }
}
