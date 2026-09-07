package com.riksa.social.dto;

import java.util.UUID;

/**
 * @param mediaUrl null leaves the existing image alone; blank clears it
 */
public record UpdatePostInput(
        UUID id,
        String title,
        String content,
        String mediaUrl
) {}
