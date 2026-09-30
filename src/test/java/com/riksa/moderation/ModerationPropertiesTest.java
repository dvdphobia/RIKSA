package com.riksa.moderation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ModerationPropertiesTest {

    @Test
    void configurationPropertiesUseTheDocumentedDefaults() {
        new ApplicationContextRunner()
                .withUserConfiguration(ModerationConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ModerationProperties.class);
                    ModerationProperties properties = context.getBean(ModerationProperties.class);
                    assertThat(properties.blockScore()).isEqualTo(5);
                    assertThat(properties.reviewScore()).isEqualTo(2);
                    assertThat(properties.newAccountAge()).isEqualTo(Duration.ofHours(24));
                    assertThat(properties.allowedLinkHosts()).containsExactly(
                            "youtube.com", "youtu.be", "wikipedia.org", "khanacademy.org", "github.com", "riksa.app",
                            "moeys.gov.kh", "edu.kh", "coursera.org", "edx.org", "mit.edu", "stackoverflow.com",
                            "geeksforgeeks.org", "w3schools.com", "desmos.com", "geogebra.org", "phet.colorado.edu");
                });
    }

    @Test
    void configuredValuesOverrideDefaults() {
        new ApplicationContextRunner()
                .withUserConfiguration(ModerationConfiguration.class)
                .withPropertyValues(
                        "moderation.block-score=7",
                        "moderation.review-score=3",
                        "moderation.new-account-age=48h",
                        "moderation.allowed-link-hosts[0]=school.example")
                .run(context -> {
                    ModerationProperties properties = context.getBean(ModerationProperties.class);
                    assertThat(properties.blockScore()).isEqualTo(7);
                    assertThat(properties.reviewScore()).isEqualTo(3);
                    assertThat(properties.newAccountAge()).isEqualTo(Duration.ofHours(48));
                    assertThat(properties.allowedLinkHosts()).containsExactly("school.example");
                });
    }
}
