package com.riksa.quest.entity;
public enum QuestType {
    COMPLETE_LESSON("Finish 1 lesson", "បញ្ចប់មេរៀន 1", 1, 20),
    CORRECT_ANSWERS("Answer 3 correctly", "ឆ្លើយត្រូវ 3", 3, 15),
    SOCIAL_ACTION("React or comment once", "បញ្ចេញប្រតិកម្ម ឬមតិ 1 ដង", 1, 10);
    private final String title;
    private final String titleKm;
    private final int target;
    private final int xpReward;
    QuestType(String title, String titleKm, int target, int xpReward) {
        this.title = title; this.titleKm = titleKm; this.target = target; this.xpReward = xpReward;
    }
    public String title() { return title; }
    public String titleKm() { return titleKm; }
    public int target() { return target; }
    public int xpReward() { return xpReward; }
}
