package com.riksa.admin.service;

import com.riksa.auth.repository.UserRepository;
import com.riksa.admin.dto.settings.AiChatSettings;
import com.riksa.chatAi.repository.ChatMessageRepository;
import com.riksa.chatAi.repository.ChatSessionRepository;
import com.riksa.common.audit.AuditLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatAiAdminServiceTest {

    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatSessionRepository chatSessionRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private SettingsService settingsService;
    @InjectMocks private ChatAiAdminService chatAiAdminService;

    @Test
    void usageAggregatesMessagesUsersAndTopUsers() {
        UUID mostActiveId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        OffsetDateTime latest = OffsetDateTime.now().minusMinutes(1);
        ChatMessageRepository.UserUsageRow mostActive = mock(ChatMessageRepository.UserUsageRow.class);
        when(mostActive.getUserId()).thenReturn(mostActiveId);
        when(mostActive.getUsername()).thenReturn("top");
        when(mostActive.getDisplayName()).thenReturn("Top Learner");
        when(mostActive.getMessageCount()).thenReturn(6L);
        when(mostActive.getSessionCount()).thenReturn(2L);
        when(mostActive.getLastMessageAt()).thenReturn(latest);

        ChatMessageRepository.UserUsageRow second = mock(ChatMessageRepository.UserUsageRow.class);
        when(second.getUserId()).thenReturn(secondId);
        when(second.getUsername()).thenReturn("second");
        when(second.getDisplayName()).thenReturn(null);
        when(second.getMessageCount()).thenReturn(3L);
        when(second.getSessionCount()).thenReturn(1L);
        when(second.getLastMessageAt()).thenReturn(latest.minusHours(1));

        when(chatMessageRepository.findUsageByCreatedAtAfter(any()))
                .thenReturn(List.of(second, mostActive));
        when(settingsService.getAiChat()).thenReturn(new AiChatSettings(true, 0, "system"));
        ReflectionTestUtils.setField(chatAiAdminService, "openRouterApiKey", "configured-key");

        var result = chatAiAdminService.usage(7);

        assertThat(result.days()).isEqualTo(7);
        assertThat(result.totalMessages()).isEqualTo(9);
        assertThat(result.activeUsers()).isEqualTo(2);
        assertThat(result.dailyLimit()).isZero();
        assertThat(result.enabled()).isTrue();
        assertThat(result.topUsers()).extracting("userId").containsExactly(mostActiveId, secondId);
        assertThat(result.topUsers().getFirst().messages()).isEqualTo(6);
    }

    @Test
    void usageUsesConfiguredEnabledStateAndDailyLimit() {
        when(chatMessageRepository.findUsageByCreatedAtAfter(any())).thenReturn(List.of());
        when(settingsService.getAiChat()).thenReturn(new AiChatSettings(false, 17, "system"));
        ReflectionTestUtils.setField(chatAiAdminService, "openRouterApiKey", "configured-key");

        var result = chatAiAdminService.usage(1);

        assertThat(result.dailyLimit()).isEqualTo(17);
        assertThat(result.enabled()).isFalse();
    }

    @Test
    void usageReportsDisabledWhenProviderKeyIsMissing() {
        when(chatMessageRepository.findUsageByCreatedAtAfter(any())).thenReturn(List.of());
        when(settingsService.getAiChat()).thenReturn(new AiChatSettings(true, 3, "system"));
        ReflectionTestUtils.setField(chatAiAdminService, "openRouterApiKey", "");

        var result = chatAiAdminService.usage(1);

        assertThat(result.enabled()).isFalse();
        assertThat(result.dailyLimit()).isEqualTo(3);
    }
}
