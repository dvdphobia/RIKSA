package com.riksa.social.service;

import com.riksa.media.service.MediaPublicUrlFactory;
import com.riksa.media.service.MediaStorageService;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/** Ensures post media points to an image this API has actually stored. */
@Service
public class UploadedMediaUrlValidator {

    private static final int MAX_LENGTH = 2048;
    private static final Pattern STORED_ID = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(?:jpg|png|gif|webp)$");

    private final MediaPublicUrlFactory publicUrlFactory;
    private final MediaStorageService storage;

    public UploadedMediaUrlValidator(MediaPublicUrlFactory publicUrlFactory, MediaStorageService storage) {
        this.publicUrlFactory = publicUrlFactory;
        this.storage = storage;
    }

    public String normalize(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }

        String url = rawUrl.trim();
        if (url.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Media URL must be at most " + MAX_LENGTH + " characters");
        }

        String prefix = publicUrlFactory.prefix();
        URI prefixUri;
        URI urlUri;
        try {
            prefixUri = new URI(prefix);
            urlUri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Media URL is not a valid uploaded image URL");
        }

        if (!prefixUri.isAbsolute() || prefixUri.getHost() == null
                || !urlUri.isAbsolute() || urlUri.getHost() == null
                || urlUri.getUserInfo() != null || urlUri.getQuery() != null || urlUri.getFragment() != null
                || !sameOrigin(prefixUri, urlUri) || !url.startsWith(prefix)) {
            throw new IllegalArgumentException("Media URL must use this API's media upload URL");
        }

        String id = url.substring(prefix.length());
        if (!STORED_ID.matcher(id).matches() || !storage.exists(id)) {
            throw new IllegalArgumentException("Media URL must be an image uploaded through this API");
        }
        return url;
    }

    private boolean sameOrigin(URI expected, URI actual) {
        return expected.getScheme().equalsIgnoreCase(actual.getScheme())
                && expected.getHost().equalsIgnoreCase(actual.getHost())
                && expected.getPort() == actual.getPort();
    }
}
