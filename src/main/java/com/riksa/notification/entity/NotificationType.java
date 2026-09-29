package com.riksa.notification.entity;

/** What happened. The app picks the wording and icon from this. */
public enum NotificationType {
    /** Someone reacted to your community post. */
    POST_REACTION,
    /** Someone reacted to your comment. */
    COMMENT_REACTION,
    /** Someone commented on your community post. */
    POST_COMMENT,
    /** Someone replied to your comment. */
    COMMENT_REPLY,
    /** Someone liked your blog article. */
    BLOG_LIKE,
    /** A moderator published your held blog article. */
    BLOG_APPROVED,
    /** A moderator turned down your blog article. */
    BLOG_REJECTED;

    /** Types where one person repeating the action should refresh, not add, a notification. */
    public boolean collapsesRepeats() {
        return this == POST_REACTION || this == COMMENT_REACTION || this == BLOG_LIKE;
    }
}
