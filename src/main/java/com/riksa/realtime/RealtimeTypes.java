package com.riksa.realtime;

/** Event names the app listens for. Keep in step with the Flutter client. */
public final class RealtimeTypes {

    /** A post's reactions or comments changed: {@code {postId}}. */
    public static final String POST_CHANGED = "post.changed";
    /** A new post was published: {@code {postId, authorId}}. */
    public static final String POST_CREATED = "post.created";
    /** A post was removed: {@code {postId}}. */
    public static final String POST_DELETED = "post.deleted";
    /** The recipient's daily or weekly quest progress moved. */
    public static final String QUESTS_CHANGED = "quests.changed";
    /** The recipient has a new notification: {@code {id, unreadCount}}. */
    public static final String NOTIFICATION_CREATED = "notification.created";

    private RealtimeTypes() {
    }
}
