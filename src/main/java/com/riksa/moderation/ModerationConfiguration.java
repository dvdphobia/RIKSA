package com.riksa.moderation;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers the immutable moderation settings record for constructor binding. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ModerationProperties.class)
public class ModerationConfiguration {
}
