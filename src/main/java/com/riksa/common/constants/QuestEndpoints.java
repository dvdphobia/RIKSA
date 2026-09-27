package com.riksa.common.constants;
public final class QuestEndpoints {
    private QuestEndpoints() {}
    public static final String BASE = ApiConstants.API_V1 + "/quests";
    public static final String TODAY = "/today";
    public static final String WEEK = "/week";
    public static final String CLAIM = "/{id}/claim";
    public static final String CHEST_OPEN = "/chest/open";
}
