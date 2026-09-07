package com.riksa.blog.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContentFilterServiceTest {

    private final ContentFilterService service = new ContentFilterService();

    @Test
    void validate_containsMath_passes() {
        assertThatCode(() -> service.validate("Learning math is fun"))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_containsPhysics_passes() {
        assertThatCode(() -> service.validate("Physics is fascinating"))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_containsProgramming_passes() {
        assertThatCode(() -> service.validate("I love programming in java"))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_containsKhmerKeyword_passes() {
        assertThatCode(() -> service.validate("គណិត is the Khmer word for math"))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_noStemKeyword_throwsException() {
        assertThatThrownBy(() -> service.validate("Hello world, how are you?"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Content must be related to STEM, Tech, or History");
    }

    @Test
    void validate_caseInsensitive_passes() {
        assertThatCode(() -> service.validate("SCIENCE is amazing"))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_emptyContent_throwsException() {
        assertThatThrownBy(() -> service.validate(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validate_machineLearning_passes() {
        assertThatCode(() -> service.validate("machine learning is the future"))
                .doesNotThrowAnyException();
    }
}