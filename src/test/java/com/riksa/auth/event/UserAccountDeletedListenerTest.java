package com.riksa.auth.event;

import com.riksa.leaderboard.service.RedisLeaderboardStore;
import com.riksa.media.repository.MediaReferenceRepository;
import com.riksa.media.service.MediaStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAccountDeletedListenerTest {

    @Mock private RedisLeaderboardStore leaderboardStore;
    @Mock private MediaReferenceRepository mediaReferenceRepository;
    @Mock private MediaStorageService mediaStorageService;

    @InjectMocks private UserAccountDeletedListener listener;

    @Test
    void removesLeaderboardEntryAndOnlyDeletesUnreferencedImages() {
        UUID userId = UUID.randomUUID();
        var unreferenced = new UserAccountDeletedEvent.MediaAssetReference("first.png", "https://api.test/first.png");
        var shared = new UserAccountDeletedEvent.MediaAssetReference("second.png", "https://api.test/second.png");
        when(mediaReferenceRepository.isReferenced(unreferenced.url())).thenReturn(false);
        when(mediaReferenceRepository.isReferenced(shared.url())).thenReturn(true);

        listener.cleanAfterCommit(new UserAccountDeletedEvent(this, userId, List.of(unreferenced, shared)));

        verify(leaderboardStore).removeUser(userId);
        verify(mediaStorageService).deleteIfExists(unreferenced.id());
        verify(mediaStorageService, never()).deleteIfExists(shared.id());
    }
}
