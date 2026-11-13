package com.riksa.auth.event;

import com.riksa.leaderboard.service.RedisLeaderboardStore;
import com.riksa.media.repository.MediaReferenceRepository;
import com.riksa.media.service.MediaStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserAccountDeletedListener {

    private final RedisLeaderboardStore leaderboardStore;
    private final MediaReferenceRepository mediaReferenceRepository;
    private final MediaStorageService mediaStorageService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void cleanAfterCommit(UserAccountDeletedEvent event) {
        try {
            leaderboardStore.removeUser(event.getUserId());
        } catch (RuntimeException exception) {
            log.warn("Account deletion leaderboard cleanup failed ({})",
                    exception.getClass().getSimpleName());
        }

        for (var asset : event.getMediaAssets()) {
            try {
                if (!mediaReferenceRepository.isReferenced(asset.url())) {
                    mediaStorageService.deleteIfExists(asset.id());
                }
            } catch (RuntimeException exception) {
                log.warn("Account deletion media cleanup failed ({})",
                        exception.getClass().getSimpleName());
            }
        }
    }
}
