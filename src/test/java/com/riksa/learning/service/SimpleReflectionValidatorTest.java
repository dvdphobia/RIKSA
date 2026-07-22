package com.riksa.learning.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimpleReflectionValidatorTest {

    private final SimpleReflectionValidator validator = new SimpleReflectionValidator(3, 5, 8);

    @Test
    void rejectsBlankAndTooShortReflections() {
        assertThatThrownBy(() -> validator.validate(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate("only two")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void scoresConfiguredWordThresholds() {
        assertThat(validator.validate("one two three")).isEqualTo(1);
        assertThat(validator.validate("one two three four five")).isEqualTo(2);
        assertThat(validator.validate("one two three four five six seven eight")).isEqualTo(3);
    }
}
