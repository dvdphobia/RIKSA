package com.riksa.media.controller;

import com.riksa.common.constants.MediaEndpoints;
import com.riksa.media.dto.MediaUploadResponse;
import com.riksa.media.service.MediaStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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

    /**
     * Base URL to build image links from. Left blank the URL is derived from the
     * request, which is what a phone on a LAN or emulator needs in development;
     * set it explicitly once the API sits behind a proxy or CDN.
     */
    @Value("${media.public-base-url:}")
    private String publicBaseUrl;

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
        return new MediaUploadResponse(id, publicUrl(id));
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

    private String publicUrl(String id) {
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            return publicBaseUrl.replaceAll("/+$", "") + MediaEndpoints.BASE + "/" + id;
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(MediaEndpoints.BASE)
                .path("/")
                .path(id)
                .toUriString();
    }
}
