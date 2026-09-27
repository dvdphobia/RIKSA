package com.riksa.social.service;

import com.riksa.media.service.MediaPublicUrlFactory;
import com.riksa.media.service.MediaStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadedMediaUrlValidatorTest {

    @TempDir
    Path dir;

    private MediaStorageService storage() {
        return new MediaStorageService(dir.toString(), 1024);
    }

    @Test
    void acceptsAnExistingUploadOnTheApiHostAndPrefix() {
        MediaStorageService storage = storage();
        String id = storage.store(pngHeader());
        UploadedMediaUrlValidator validator = new UploadedMediaUrlValidator(
                new MediaPublicUrlFactory("https://api.example.test"), storage);

        assertThat(validator.normalize("https://api.example.test/api/v1/media/" + id))
                .isEqualTo("https://api.example.test/api/v1/media/" + id);
    }

    @Test
    void rejectsForeignHostEvenWhenItHasAnImagePath() {
        UploadedMediaUrlValidator validator = new UploadedMediaUrlValidator(
                new MediaPublicUrlFactory("https://api.example.test"), storage());

        assertThatThrownBy(() -> validator.normalize("https://foreign.example/api/v1/media/00000000-0000-0000-0000-000000000000.png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("this API");
    }

    @Test
    void rejectsCorrectHostWithWrongPrefix() {
        UploadedMediaUrlValidator validator = new UploadedMediaUrlValidator(
                new MediaPublicUrlFactory("https://api.example.test"), storage());

        assertThatThrownBy(() -> validator.normalize("https://api.example.test/uploads/00000000-0000-0000-0000-000000000000.png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("this API");
    }

    @Test
    void rejectsAWellShapedButUnissuedId() {
        UploadedMediaUrlValidator validator = new UploadedMediaUrlValidator(
                new MediaPublicUrlFactory("https://api.example.test"), storage());

        assertThatThrownBy(() -> validator.normalize(
                "https://api.example.test/api/v1/media/00000000-0000-0000-0000-000000000000.png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("uploaded through this API");
    }

    private static byte[] pngHeader() {
        byte[] header = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        byte[] bytes = new byte[64];
        System.arraycopy(header, 0, bytes, 0, header.length);
        return bytes;
    }
}
