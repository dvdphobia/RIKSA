package com.riksa.quest.entity;

public enum WeeklyQuestType {
    WEEKLY_LESSONS("Finish 7 lessons", "បញ្ចប់មេរៀន 7", 7, 100),
    WEEKLY_CORRECT_ANSWERS("Answer 30 correctly", "ឆ្លើយត្រូវ 30", 30, 60),
    WEEKLY_ACTIVE_DAYS("Learn on 5 days this week", "រៀន 5 ថ្ងៃក្នុងសប្តាហ៍", 5, 80),
    WEEKLY_SOCIAL("Join the community 5 times", "ចូលរួមសហគមន៍ 5 ដង", 5, 40);

    private final String title;
    private final String titleKm;
    private final int target;
    private final int xpReward;

    WeeklyQuestType(String title, String titleKm, int target, int xpReward) {
        this.title = title;
        this.titleKm = titleKm;
        this.target = target;
        this.xpReward = xpReward;
    }

    public String title() { return title; }
    public String titleKm() { return titleKm; }
    public int target() { return target; }
    public int xpReward() { return xpReward; }
}
