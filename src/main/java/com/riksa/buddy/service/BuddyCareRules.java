package com.riksa.buddy.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Locale;

/** Pure energy and bond rules shared with the Flutter BuddyCare model. */
public final class BuddyCareRules {

    public static final String LESSON = "LESSON";
    public static final String BLOG_READ = "BLOG_READ";
    public static final String POST = "POST";
    public static final String BLOG_PUBLISHED = "BLOG_PUBLISHED";
    public static final int MAX_ENERGY = 100;
    public static final int MAX_DAILY_LESSON_BOND_AWARDS = 5;
    public static final ZoneId CARE_ZONE = ZoneId.of("Asia/Phnom_Penh");

    public record Gain(int energy, int bond) {}

    public int settle(int energy, OffsetDateTime updatedAt, OffsetDateTime now) {
        long elapsedMinutes = Math.max(0, Duration.between(updatedAt, now).toMinutes());
        long decay = elapsedMinutes / 72;
        return (int) Math.max(0, Math.min(MAX_ENERGY, energy - decay));
    }

    public Gain gainFor(String sourceKey) {
        return switch (normalize(sourceKey)) {
            case LESSON -> new Gain(35, 10);
            case BLOG_READ, POST -> new Gain(10, 3);
            case BLOG_PUBLISHED -> new Gain(20, 8);
            default -> throw new IllegalArgumentException("Unknown buddy care source: " + sourceKey);
        };
    }

    /** Null means the source has no event-count cap. Lesson bond has its own cap. */
    public Integer dailyCap(String sourceKey) {
        return switch (normalize(sourceKey)) {
            case LESSON -> null;
            case BLOG_READ -> 2;
            case POST, BLOG_PUBLISHED -> 1;
            default -> throw new IllegalArgumentException("Unknown buddy care source: " + sourceKey);
        };
    }

    public int bondLevel(int bond) {
        return 1 + (int) Math.floor(Math.sqrt(Math.max(0, bond) / 15.0));
    }

    public static LocalDate careDay(OffsetDateTime now) {
        return now.atZoneSameInstant(CARE_ZONE).toLocalDate();
    }

    public static String normalize(String sourceKey) {
        return sourceKey == null ? "" : sourceKey.trim().toUpperCase(Locale.ROOT);
    }
}
