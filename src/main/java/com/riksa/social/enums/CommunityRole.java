package com.riksa.social.enums;

/** A member's standing inside one community. Declared from least to most power. */
public enum CommunityRole {
    MEMBER, MODERATOR, OWNER;

    public boolean canModerate() {
        return this != MEMBER;
    }
}
