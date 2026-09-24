package com.riksa.social.service;

import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.auth.security.RateLimitDecision;
import com.riksa.auth.security.RedisRateLimiter;
import com.riksa.common.exception.RateLimitExceededException;
import com.riksa.common.exception.ServiceUnavailableException;
import com.riksa.social.entity.Category;
import com.riksa.social.entity.UserCategoryMembership;
import com.riksa.social.repository.CategoryRepository;
import com.riksa.social.repository.UserCategoryMembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommunityCreationServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private UserCategoryMembershipRepository membershipRepository;
    @Mock private UserRepository userRepository;
    @Mock private RedisRateLimiter rateLimiter;

    private CommunityCreationService service;
    private final UUID userId = UUID.randomUUID();
    private User user;

    @BeforeEach
    void setUp() {
        service = new CommunityCreationService(
                categoryRepository, membershipRepository, userRepository,
                new MediaUrlValidator(), rateLimiter);
        user = new User();
        user.setId(userId);
        user.setEmailVerified(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(rateLimiter.acquire(anyString(), anyString(), anyLong(), any(Duration.class)))
                .thenReturn(new RateLimitDecision(true, 3, 2, 0));
        when(categoryRepository.saveAndFlush(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
    }

    @Test
    void create_validRequest_savesUserCommunityAndJoinsFounder() {
        Category saved = service.create(userId, "  Robotics   Club ", " Build robots\n\n\n\ntogether ", null);

        assertThat(saved.getName()).isEqualTo("Robotics Club");
        assertThat(saved.getSlug()).isEqualTo("robotics-club");
        assertThat(saved.getDescription()).isEqualTo("Build robots\n\ntogether");
        assertThat(saved.isOfficial()).isFalse();
        assertThat(saved.getCreatedBy()).isEqualTo(userId);

        ArgumentCaptor<UserCategoryMembership> membership =
                ArgumentCaptor.forClass(UserCategoryMembership.class);
        verify(membershipRepository).save(membership.capture());
        assertThat(membership.getValue().getUserId()).isEqualTo(userId);
        assertThat(membership.getValue().getCategoryId()).isEqualTo(saved.getId());
        verify(rateLimiter).acquire(eq("community-create"), eq(userId.toString()), eq(3L), any());
    }

    @Test
    void create_khmerName_isAcceptedWithGeneratedSlug() {
        Category saved = service.create(userId, "អក្សរសិល្ប៍ខ្មែរ", null, null);

        assertThat(saved.getName()).isEqualTo("អក្សរសិល្ប៍ខ្មែរ");
        assertThat(saved.getSlug()).matches("community-[0-9a-f]{6}");
    }

    @Test
    void create_takenSlug_getsRandomSuffix() {
        when(categoryRepository.existsBySlug("robotics")).thenReturn(true);

        Category saved = service.create(userId, "Robotics", null, null);

        assertThat(saved.getSlug()).matches("robotics-[0-9a-f]{6}");
    }

    @Test
    void create_signedOut_isDenied() {
        assertThatThrownBy(() -> service.create(null, "Robotics", null, null))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void create_unverifiedAccount_isDenied() {
        user.setEmailVerified(false);

        assertThatThrownBy(() -> service.create(userId, "Robotics", null, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Verify");
        verify(categoryRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_telegramAccount_countsAsVerified() {
        user.setEmailVerified(false);
        user.setTelegramId("12345");

        assertThat(service.create(userId, "Robotics", null, null).getName()).isEqualTo("Robotics");
    }

    @Test
    void create_suspendedAccount_isDenied() {
        user.setSuspendedUntil(OffsetDateTime.now().plusDays(1));

        assertThatThrownBy(() -> service.create(userId, "Robotics", null, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Suspended");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ab",                                    // too short
            "123456",                                // no letters
            "Robotics <script>",                     // markup
            "Robo‮tics",                        // bidi override
            "Robo​tics",                        // zero-width space
            "Admin",                                 // reserved
            "M o d s",                               // reserved once spaces are gone
            "Official RIKSA Team",                   // impersonates the platform
            "Riksa-Help",
            "This name is far too long to be a community name ok",
    })
    void create_badName_isRejectedBeforeAnyQuotaIsUsed(String name) {
        assertThatThrownBy(() -> service.create(userId, name, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(rateLimiter);
        verify(categoryRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_descriptionTooLong_isRejected() {
        assertThatThrownBy(() -> service.create(userId, "Robotics", "x".repeat(301), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("300");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "javascript:alert(1)",
            "http://images.example.com/a.png",
            "https://user:pass@images.example.com/a.png",
    })
    void create_unsafeIconUrl_isRejected(String iconUrl) {
        assertThatThrownBy(() -> service.create(userId, "Robotics", null, iconUrl))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_duplicateNameIgnoringCase_isRejected() {
        when(categoryRepository.existsByNameIgnoreCase("biology")).thenReturn(true);

        assertThatThrownBy(() -> service.create(userId, "biology", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
        verifyNoInteractions(rateLimiter);
    }

    @Test
    void create_nameTakenByConcurrentRequest_isReportedAsDuplicate() {
        when(categoryRepository.saveAndFlush(any(Category.class)))
                .thenThrow(new DataIntegrityViolationException("ux_categories_name_lower"));

        assertThatThrownBy(() -> service.create(userId, "Robotics", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void create_ownerAtLimit_isRejected() {
        when(categoryRepository.countByCreatedBy(userId)).thenReturn(5L);

        assertThatThrownBy(() -> service.create(userId, "Robotics", null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at most 5");
    }

    @Test
    void create_dailyQuotaUsed_isRateLimited() {
        when(rateLimiter.acquire(anyString(), anyString(), anyLong(), any(Duration.class)))
                .thenReturn(new RateLimitDecision(false, 3, 0, 3600));

        assertThatThrownBy(() -> service.create(userId, "Robotics", null, null))
                .isInstanceOf(RateLimitExceededException.class);
        verify(categoryRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_rateLimiterDown_failsClosed() {
        when(rateLimiter.acquire(anyString(), anyString(), anyLong(), any(Duration.class)))
                .thenThrow(new IllegalStateException("redis down"));

        assertThatThrownBy(() -> service.create(userId, "Robotics", null, null))
                .isInstanceOf(ServiceUnavailableException.class);
        verify(categoryRepository, never()).saveAndFlush(any());
    }
}
