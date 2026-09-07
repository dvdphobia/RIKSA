package com.riksa.social.service;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/**
 * Validates client-supplied media URLs.
 *
 * <p>The backend never fetches these URLs — the client uploads the image to its
 * own storage (S3/Cloudinary/etc.) and sends back the resulting link, exactly as
 * {@code posts.media_url} already works. Validation therefore guards against
 * malformed and hostile <em>values</em> (script schemes, embedded credentials,
 * unbounded strings) rather than against remote fetches.
 *
 * <p>Note that no file-extension check is applied: CDN links in use today, such
 * as {@code https://images.unsplash.com/photo-1635070041078?w=600}, carry no
 * extension at all.
 */
@Service
public class MediaUrlValidator {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final int MAX_LENGTH = 2048;

    /**
     * @param mediaUrl raw client input; may be {@code null} or blank
     * @return the trimmed URL, or {@code null} when no media was supplied
     * @throws IllegalArgumentException when a non-blank value is not a usable URL
     */
    public String normalize(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isBlank()) {
            return null;
        }

        String trimmed = mediaUrl.trim();

        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Media URL must be at most " + MAX_LENGTH + " characters");
        }

        URI uri;
        try {
            uri = new URI(trimmed);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Media URL is not a valid URL");
        }

        if (!uri.isAbsolute() || uri.getScheme() == null) {
            throw new IllegalArgumentException("Media URL must be an absolute URL");
        }

        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (!ALLOWED_SCHEMES.contains(scheme)) {
            throw new IllegalArgumentException("Media URL must use http or https");
        }

        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("Media URL must include a host");
        }

        // https://attacker@trusted-cdn.example renders as a trusted host in some
        // clients while resolving elsewhere, and leaks whatever is in userinfo.
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("Media URL must not contain credentials");
        }

        return trimmed;
    }
}
