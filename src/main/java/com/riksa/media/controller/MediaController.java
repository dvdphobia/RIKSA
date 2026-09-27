package com.riksa.media.controller;

import com.riksa.common.constants.MediaEndpoints;
import com.riksa.media.dto.MediaUploadResponse;
import com.riksa.media.service.MediaPublicUrlFactory;
import com.riksa.media.service.MediaStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;

/**
 * Uploads and serves post images.
 *
 * <p>Uploading requires a signed-in user; reading does not, because the URL is
 * embedded in posts that any reader can already see (see {@code SecurityConfig},
 * which permits {@code GET} on this path only).
 */
@RestController
@RequestMapping(MediaEndpoints.BASE)
@RequiredArgsConstructor
public class MediaController {

    private final MediaStorageService storage;
    private final MediaPublicUrlFactory publicUrlFactory;

    @PostMapping(value = MediaEndpoints.ROOT, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MediaUploadResponse upload(@RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read uploaded file", e);
        }

        String id = storage.store(bytes);
        var dimensions = storage.dimensions(bytes);
        return new MediaUploadResponse(
                id,
                publicUrlFactory.urlFor(id),
                dimensions == null ? null : dimensions.width(),
                dimensions == null ? null : dimensions.height());
    }

    @GetMapping(MediaEndpoints.ITEM)
    public ResponseEntity<byte[]> serve(@PathVariable String id) {
        var image = storage.load(id);
        return ResponseEntity.ok()
                // The id is a fresh UUID per upload, so a stored image never changes.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.bytes());
    }
}
