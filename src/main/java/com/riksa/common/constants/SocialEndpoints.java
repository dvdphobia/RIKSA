package com.riksa.common.constants;

/**
 * Social module GraphQL operation names.
 * Single endpoint: POST /graphql
 */
public final class SocialEndpoints {

    private SocialEndpoints() {}

    // --- Queries ---
    public static final String QUERY_POST = "post";
    public static final String QUERY_CATEGORIES = "categories";
    public static final String QUERY_FEED = "feed";
    public static final String QUERY_COMMENTS = "comments";

    // --- Post mutations ---
    public static final String MUTATION_CREATE_POST = "createPost";
    public static final String MUTATION_UPDATE_POST = "updatePost";
    public static final String MUTATION_DELETE_POST = "deletePost";
    public static final String MUTATION_SHARE_POST = "sharePost";

    // --- Category mutations ---
    public static final String MUTATION_JOIN_CATEGORY = "joinCategory";
    public static final String MUTATION_LEAVE_CATEGORY = "leaveCategory";

    // --- Reaction mutations ---
    public static final String MUTATION_REACT = "react";
    public static final String MUTATION_REMOVE_REACTION = "removeReaction";

    // --- Comment mutations ---
    public static final String MUTATION_CREATE_COMMENT = "createComment";
    public static final String MUTATION_UPDATE_COMMENT = "updateComment";
    public static final String MUTATION_DELETE_COMMENT = "deleteComment";
}