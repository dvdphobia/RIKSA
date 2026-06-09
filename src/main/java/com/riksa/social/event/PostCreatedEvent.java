package com.riksa.social.event;

import java.util.UUID;

public record PostCreatedEvent(UUID postId, UUID authorId, UUID categoryId) {}