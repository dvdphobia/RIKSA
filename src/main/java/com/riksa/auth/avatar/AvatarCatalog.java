package com.riksa.auth.avatar;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Preset avatar paths served from {@code src/main/resources/static/avatars}.
 */
public final class AvatarCatalog {

    private static final List<String> AVAILABLE_AVATARS = IntStream.rangeClosed(1, 30)
            .mapToObj(number -> "/avatars/avatar-" + number + ".png")
            .toList();

    private static final Set<String> AVAILABLE_AVATAR_SET = Set.copyOf(AVAILABLE_AVATARS);

    private AvatarCatalog() {
    }

    public static List<String> availableAvatars() {
        return AVAILABLE_AVATARS;
    }

    public static boolean isAvailable(String avatarUrl) {
        return AVAILABLE_AVATAR_SET.contains(avatarUrl);
    }
}
