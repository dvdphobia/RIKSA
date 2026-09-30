package com.riksa.quest.setup;

import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.gamification.entity.XpTransaction;
import com.riksa.gamification.repository.XpTransactionRepository;
import com.riksa.gamification.service.XpLedgerService;
import com.riksa.social.repository.PostRepository;
import com.riksa.social.repository.UserCategoryMembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SetupQuestServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserCategoryMembershipRepository membershipRepository;
    @Mock private PostRepository postRepository;
    @Mock private XpTransactionRepository xpTransactionRepository;
    @Mock private XpLedgerService xpLedgerService;
    @InjectMocks private SetupQuestService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(membershipRepository.findByUserId(user.getId())).thenReturn(List.of());
        when(xpTransactionRepository.findByUserIdAndSourceTypeAndSourceId(any(), any(), any()))
                .thenReturn(Optional.empty());
    }

    private Map<String, SetupTaskResponse> byKey() {
        return service.list(user.getId()).stream()
                .collect(Collectors.toMap(SetupTaskResponse::key, task -> task));
    }

    @Test
    void aNewAccountHasEverythingLeftToDo() {
        assertThat(byKey().values()).allMatch(task -> !task.done() && !task.claimed());
    }

    @Test
    void telegramSignInWithoutANumberDoesNotCountAsVerified() {
        user.setPhoneVerified(true);
        assertThat(byKey().get("VERIFY_PHONE").done()).isFalse();

        user.setPhoneNumber("+85512345678");
        assertThat(byKey().get("VERIFY_PHONE").done()).isTrue();
    }

    @Test
    void claimPaysOnceTheStepIsDone() {
        user.setBio("I love chemistry");

        service.claim(user.getId(), "write_bio");

        verify(xpLedgerService).awardXp(user.getId(), 20, SetupTask.XP_SOURCE,
                SetupTask.WRITE_BIO.sourceId(user.getId()));
    }

    @Test
    void claimRefusesAnUnfinishedStep() {
        assertThatThrownBy(() -> service.claim(user.getId(), "WRITE_BIO"))
                .isInstanceOf(IllegalStateException.class);
        verify(xpLedgerService, never()).awardXp(any(), anyInt(), any(), any());
    }

    @Test
    void aClaimedStepStaysClaimed() {
        when(xpTransactionRepository.findByUserIdAndSourceTypeAndSourceId(
                user.getId(), SetupTask.XP_SOURCE, SetupTask.JOIN_COMMUNITY.sourceId(user.getId())))
                .thenReturn(Optional.of(new XpTransaction()));

        SetupTaskResponse task = byKey().get("JOIN_COMMUNITY");
        assertThat(task.claimed()).isTrue();
        assertThat(task.done()).isTrue();
    }

    @Test
    void sourceIdsAreStablePerUser() {
        UUID other = UUID.randomUUID();
        assertThat(SetupTask.WRITE_BIO.sourceId(user.getId())).isEqualTo(SetupTask.WRITE_BIO.sourceId(user.getId()));
        assertThat(SetupTask.WRITE_BIO.sourceId(user.getId())).isNotEqualTo(SetupTask.WRITE_BIO.sourceId(other));
    }
}
