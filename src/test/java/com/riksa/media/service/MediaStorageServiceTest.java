package com.riksa.media.service;

import com.riksa.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaStorageServiceTest {

    @TempDir
    Path dir;

    private MediaStorageService service() {
        return new MediaStorageService(dir.toString(), 1024);
    }

    private static byte[] png() {
        byte[] header = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        byte[] bytes = new byte[64];
        System.arraycopy(header, 0, bytes, 0, header.length);
        return bytes;
    }

    private static byte[] webp() {
        byte[] bytes = new byte[32];
        System.arraycopy("RIFF".getBytes(StandardCharsets.US_ASCII), 0, bytes, 0, 4);
        System.arraycopy("WEBP".getBytes(StandardCharsets.US_ASCII), 0, bytes, 8, 4);
        return bytes;
    }

    @Test
    void storesAnImageUnderAGeneratedNameAndReadsItBack() {
        MediaStorageService service = service();

        String id = service.store(png());

        assertThat(id).endsWith(".png");
        var loaded = service.load(id);
        assertThat(loaded.contentType()).isEqualTo("image/png");
        assertThat(loaded.bytes()).isEqualTo(png());
    }

    @Test
    void namesTheFileFromItsBytesNotItsClaimedType() {
        // A WebP is a RIFF container; only byte 8 distinguishes it.
        assertThat(service().store(webp())).endsWith(".webp");
    }

    @Test
    void rejectsContentThatIsNotAnImage() {
        assertThatThrownBy(() -> service().store("<?php echo 1; ?>".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JPEG, PNG, GIF or WebP");
    }

    @Test
    void rejectsAnEmptyUpload() {
        assertThatThrownBy(() -> service().store(new byte[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void rejectsAnOversizeUpload() {
        assertThatThrownBy(() -> new MediaStorageService(dir.toString(), 16).store(png()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most");
    }

    @Test
    void refusesToServeAnythingButAGeneratedName() throws Exception {
        MediaStorageService service = service();
        Files.writeString(dir.resolve("secret.txt"), "private");

        // Traversal, absolute paths and hand-written names all miss the pattern.
        for (String id : new String[]{
                "../../etc/passwd", "..%2Fsecret.txt", "secret.txt",
                "/etc/passwd", "not-a-uuid.png", null}) {
            assertThatThrownBy(() -> service.load(id))
                    .as("id %s", id)
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Test
    void reportsAMissingImageAsNotFound() {
        assertThatThrownBy(() ->
                service().load("00000000-0000-0000-0000-000000000000.png"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void leavesNoPartialFileBehind() {
        MediaStorageService service = service();
        String id = service.store(png());

        assertThat(dir.toFile().list()).containsExactly(id);
    }
}
