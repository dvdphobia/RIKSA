package com.riksa.media.service;

import java.util.Arrays;
import java.util.Locale;

/**
 * The image formats the API accepts, each identified by its magic bytes.
 *
 * <p>Detection deliberately ignores both the declared {@code Content-Type} and
 * the client's filename: either can claim {@code image/png} for arbitrary
 * content. Sniffing the leading bytes is what actually decides the stored
 * extension, so a renamed executable cannot be served back as an image.
 */
public enum ImageType {

    JPEG("image/jpeg", "jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
    PNG("image/png", "png", new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}),
    GIF("image/gif", "gif", new byte[]{'G', 'I', 'F', '8'}),
    WEBP("image/webp", "webp", new byte[]{'R', 'I', 'F', 'F'});

    private final String contentType;
    private final String extension;
    private final byte[] signature;

    ImageType(String contentType, String extension, byte[] signature) {
        this.contentType = contentType;
        this.extension = extension;
        this.signature = signature;
    }

    public String contentType() {
        return contentType;
    }

    public String extension() {
        return extension;
    }

    /**
     * @return the format whose signature the bytes carry, or {@code null} when
     *         the content is not one of the accepted image formats
     */
    public static ImageType detect(byte[] bytes) {
        for (ImageType type : values()) {
            if (matches(bytes, type.signature)) {
                // "RIFF" alone is any RIFF container; WebP names itself at byte 8.
                if (type == WEBP && !isWebp(bytes)) {
                    continue;
                }
                return type;
            }
        }
        return null;
    }

    /** @return the format for a stored file's extension, or {@code null}. */
    public static ImageType fromExtension(String extension) {
        String lower = extension.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(type -> type.extension.equals(lower))
                .findFirst()
                .orElse(null);
    }

    private static boolean matches(byte[] bytes, byte[] signature) {
        if (bytes.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (bytes[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean isWebp(byte[] bytes) {
        return bytes.length >= 12
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }
}
