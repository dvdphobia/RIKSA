package com.riksa.auth.event;

import com.riksa.common.event.DomainEvent;

import java.util.List;
import java.util.UUID;

public class UserAccountDeletedEvent extends DomainEvent {

    private final UUID userId;
    private final List<MediaAssetReference> mediaAssets;

    public UserAccountDeletedEvent(Object source, UUID userId, List<MediaAssetReference> mediaAssets) {
        super(source);
        this.userId = userId;
        this.mediaAssets = List.copyOf(mediaAssets);
    }

    public UUID getUserId() {
        return userId;
    }

    public List<MediaAssetReference> getMediaAssets() {
        return mediaAssets;
    }

    public record MediaAssetReference(String id, String url) {}
}
