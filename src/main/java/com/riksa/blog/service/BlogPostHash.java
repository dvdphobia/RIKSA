package com.riksa.blog.service;

import com.riksa.moderation.ContentModerationService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Creates the persisted canonical duplicate key shared by writes and backfills. */
final class BlogPostHash {

    private BlogPostHash() {
    }

    static String hash(ContentModerationService moderation, String text) {
        String normalized = moderation.normalizeForDuplicate(text);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
