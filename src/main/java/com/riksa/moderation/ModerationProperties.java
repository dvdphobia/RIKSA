package com.riksa.moderation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

/**
 * Rule thresholds and trusted link hosts. Defaults review at 2 points, block
 * at 5 points, consider accounts under 24 hours old new, and trust the
 * education/product hosts configured here (including their subdomains).
 */
@ConfigurationProperties("moderation")
public record ModerationProperties(
        @DefaultValue("5") int blockScore,
        @DefaultValue("2") int reviewScore,
        @DefaultValue({"youtube.com", "youtu.be", "wikipedia.org", "khanacademy.org", "github.com", "riksa.app",
                "moeys.gov.kh", "edu.kh", "coursera.org", "edx.org", "mit.edu", "stackoverflow.com",
                "geeksforgeeks.org", "w3schools.com", "desmos.com", "geogebra.org", "phet.colorado.edu"})
        List<String> allowedLinkHosts,
        @DefaultValue("24h") Duration newAccountAge) {

    public ModerationProperties {
        allowedLinkHosts = allowedLinkHosts == null ? List.of() : List.copyOf(allowedLinkHosts);
        newAccountAge = newAccountAge == null ? Duration.ofHours(24) : newAccountAge;
    }
}
