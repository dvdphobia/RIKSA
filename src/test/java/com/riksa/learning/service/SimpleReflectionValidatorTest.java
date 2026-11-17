package com.riksa.learning.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimpleReflectionValidatorTest {

    private final SimpleReflectionValidator validator = new SimpleReflectionValidator(12, 3, 5, 8);

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n", "\u00A0\u2003", "ok", "abcdefghijklmnop", "I am ok", "one two three",
            "only two", "កខគឃងចabcdefg", "កខគ reflection"})
    void rejectsInsubstantialReflections(String text) {
        assertThatThrownBy(() -> validator.validate(text)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"we learn today", "  we\tlearn\ntoday  ", "we\u00A0learn\u2003today",
            "កខគឃងចឆជឈញដឋ", "កខគឃងចabcdef", "កខគ english reflection"})
    void acceptsSubstantialEnglishKhmerAndMixedText(String text) {
        assertThat(validator.validate(text)).isEqualTo(1);
    }

    @Test
    void khmerStillNeedsTheCharacterMinimum() {
        assertThatThrownBy(() -> validator.validate("កខគឃងចឆជឈញដ"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void characterAndWordMinimumsAreConfigurable() {
        SimpleReflectionValidator stricter = new SimpleReflectionValidator(20, 4, 5, 8);

        assertThatThrownBy(() -> stricter.validate("we learn today"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> stricter.validate("substantial English reflection"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(stricter.validate("we learned something useful today")).isEqualTo(2);
    }

    @Test
    void blankIsRejectedEvenWhenMinimumsAreDisabled() {
        SimpleReflectionValidator permissive = new SimpleReflectionValidator(0, 0, 5, 8);

        assertThatThrownBy(() -> permissive.validate(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void scoresConfiguredWordThresholds() {
        assertThat(validator.validate("we learn today")).isEqualTo(1);
        assertThat(validator.validate("one two three four five")).isEqualTo(2);
        assertThat(validator.validate("one two three four five six seven eight")).isEqualTo(3);
    }
}
