package com.riksa.media.service;

import com.riksa.common.constants.MediaEndpoints;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Builds the same media URL prefix for upload responses and post validation. */
@Component
public class MediaPublicUrlFactory {

    private final String publicBaseUrl;

    public MediaPublicUrlFactory(@Value("${media.public-base-url:}") String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String prefix() {
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            return publicBaseUrl.replaceAll("/+$", "") + MediaEndpoints.BASE + "/";
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(MediaEndpoints.BASE)
                .path("/")
                .toUriString();
    }

    public String urlFor(String id) {
        return prefix() + id;
    }
}
