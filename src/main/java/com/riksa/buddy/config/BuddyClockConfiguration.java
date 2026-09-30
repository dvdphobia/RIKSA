package com.riksa.buddy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneOffset;

@Configuration
public class BuddyClockConfiguration {

    @Bean
    public Clock buddyCareClock() {
        return Clock.system(ZoneOffset.UTC);
    }
}
