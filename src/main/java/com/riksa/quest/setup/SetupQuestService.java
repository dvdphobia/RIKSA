package com.riksa.quest.setup;

import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.gamification.repository.XpTransactionRepository;
import com.riksa.gamification.service.XpLedgerService;
import com.riksa.social.repository.PostRepository;
import com.riksa.social.repository.UserCategoryMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Works out which get-started tasks a user has done and pays each once. */
@Service
@RequiredArgsConstructor
public class SetupQuestService {

    private final UserRepository userRepository;
    private final UserCategoryMembershipRepository membershipRepository;
    private final PostRepository postRepository;
    private final XpTransactionRepository xpTransactionRepository;
    private final XpLedgerService xpLedgerService;

    @Transactional(readOnly = true)
    public List<SetupTaskResponse> list(UUID userId) {
        User user = requireUser(userId);
        return Arrays.stream(SetupTask.values())
                .map(task -> toResponse(task, user))
                .toList();
    }

    @Transactional
    public SetupTaskResponse claim(UUID userId, String key) {
        SetupTask task = SetupTask.fromKey(key);
        User user = requireUser(userId);
        if (!isDone(task, user)) {
            throw new IllegalStateException("Finish this step first");
        }
        // The ledger ignores a repeat for the same source, so a double tap is harmless.
        xpLedgerService.awardXp(userId, task.xpReward(), SetupTask.XP_SOURCE, task.sourceId(userId));
        return toResponse(task, user);
    }

    boolean isDone(SetupTask task, User user) {
        return switch (task) {
            case VERIFY_PHONE -> Boolean.TRUE.equals(user.getPhoneVerified())
                    && user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank();
            case WRITE_BIO -> user.getBio() != null && !user.getBio().isBlank();
            case CHOOSE_PROVINCE -> user.getProvince() != null;
            case JOIN_COMMUNITY -> !membershipRepository.findByUserId(user.getId()).isEmpty();
            case FIRST_POST -> postRepository.countByAuthorId(user.getId()) > 0;
        };
    }

    private SetupTaskResponse toResponse(SetupTask task, User user) {
        boolean claimed = xpTransactionRepository
                .findByUserIdAndSourceTypeAndSourceId(user.getId(), SetupTask.XP_SOURCE, task.sourceId(user.getId()))
                .isPresent();
        return new SetupTaskResponse(task.name(), task.title(), task.titleKm(), task.xpReward(),
                claimed || isDone(task, user), claimed);
    }

    private User requireUser(UUID userId) {
        if (userId == null) throw new AccessDeniedException("Sign in first");
        return userRepository.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("Sign in first"));
    }
}
