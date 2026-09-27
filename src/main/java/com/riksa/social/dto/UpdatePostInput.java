package com.riksa.social.dto;

import java.util.UUID;
import java.util.List;

/**
 * A null media list leaves the existing images alone; an empty list clears them.
 * The deprecated mediaUrl field is accepted when media is null.
 */
public record UpdatePostInput(
        UUID id,
        String title,
        String content,
        String mediaUrl,
        List<PostMediaInput> media
) {}
