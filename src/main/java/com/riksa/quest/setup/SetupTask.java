package com.riksa.quest.setup;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * One-time "get started" quests for a new account. Each pays once: the XP
 * ledger refuses a second grant for the same user, source type and source id.
 */
public enum SetupTask {
    VERIFY_PHONE("Verify your phone", "ផ្ទៀងផ្ទាត់លេខទូរស័ព្ទ", 50),
    WRITE_BIO("Write a short bio", "សរសេរអំពីខ្លួនអ្នកខ្លីៗ", 20),
    CHOOSE_PROVINCE("Choose your province", "ជ្រើសរើសខេត្តរបស់អ្នក", 10),
    JOIN_COMMUNITY("Join a community", "ចូលរួមសហគមន៍មួយ", 10),
    FIRST_POST("Share your first post", "បង្ហោះលើកដំបូង", 20);

    public static final String XP_SOURCE = "SETUP_TASK";

    private final String title;
    private final String titleKm;
    private final int xpReward;

    SetupTask(String title, String titleKm, int xpReward) {
        this.title = title;
        this.titleKm = titleKm;
        this.xpReward = xpReward;
    }

    public String title() { return title; }
    public String titleKm() { return titleKm; }
    public int xpReward() { return xpReward; }

    /** The same id every time for one user and task, so the reward can't repeat. */
    public UUID sourceId(UUID userId) {
        return UUID.nameUUIDFromBytes(("setup:" + name() + ":" + userId).getBytes(StandardCharsets.UTF_8));
    }

    public static SetupTask fromKey(String key) {
        try {
            return valueOf(key.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Unknown setup task: " + key);
        }
    }
}
