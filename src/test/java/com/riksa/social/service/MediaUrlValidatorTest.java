package com.riksa.social.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaUrlValidatorTest {

    private final MediaUrlValidator validator = new MediaUrlValidator();

    @Test
    void normalize_null_returnsNull() {
        assertThat(validator.normalize(null)).isNull();
    }

    @Test
    void normalize_blank_returnsNull() {
        assertThat(validator.normalize("   ")).isNull();
    }

    @Test
    void normalize_httpsUrl_passes() {
        String url = "https://cdn.example.com/image.png";
        assertThat(validator.normalize(url)).isEqualTo(url);
    }

    @Test
    void normalize_cdnUrlWithoutExtension_passes() {
        // The shape already stored in posts.media_url today.
        String url = "https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=600";
        assertThat(validator.normalize(url)).isEqualTo(url);
    }

    @Test
    void normalize_trimsSurroundingWhitespace() {
        assertThat(validator.normalize("  https://cdn.example.com/a.jpg  "))
                .isEqualTo("https://cdn.example.com/a.jpg");
    }

    @Test
    void normalize_javascriptScheme_throwsException() {
        assertThatThrownBy(() -> validator.normalize("javascript:alert(1)"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("http or https");
    }

    @Test
    void normalize_dataUri_throwsException() {
        assertThatThrownBy(() -> validator.normalize("data:image/png;base64,iVBORw0KGgo="))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void normalize_relativeUrl_throwsException() {
        assertThatThrownBy(() -> validator.normalize("/uploads/a.png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("absolute");
    }

    @Test
    void normalize_urlWithCredentials_throwsException() {
        assertThatThrownBy(() -> validator.normalize("https://user:pass@cdn.example.com/a.png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("credentials");
    }

    @Test
    void normalize_overlyLongUrl_throwsException() {
        String url = "https://cdn.example.com/" + "a".repeat(2100);
        assertThatThrownBy(() -> validator.normalize(url))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2048");
    }

    @Test
    void normalize_malformedUrl_throwsException() {
        assertThatThrownBy(() -> validator.normalize("http://exa mple.com/a.png"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
