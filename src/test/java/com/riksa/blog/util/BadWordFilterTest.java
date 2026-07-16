package com.riksa.blog.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class BadWordFilterTest {

    @Test
    void validate_cleanContent_passes() {
        assertThatCode(() -> new BadWordFilter().validate("This is a great science article"))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_containsSpam_throwsException() {
        assertThatThrownBy(() -> new BadWordFilter().validate("This is spam content"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Content contains prohibited words");
    }

    @Test
    void validate_containsScam_throwsException() {
        assertThatThrownBy(() -> new BadWordFilter().validate("Beware of scam links"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validate_caseInsensitive_throwsException() {
        assertThatThrownBy(() -> new BadWordFilter().validate("This is SPAM"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validate_khmerBadWord_throwsException() {
        assertThatThrownBy(() -> new BadWordFilter().validate("នេីក ជេរ គេ"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validate_emptyContent_passes() {
        assertThatCode(() -> new BadWordFilter().validate(""))
                .doesNotThrowAnyException();
    }
}