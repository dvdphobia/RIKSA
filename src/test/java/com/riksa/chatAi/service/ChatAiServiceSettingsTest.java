package com.riksa.chatAi.service;

import com.riksa.admin.dto.settings.AiChatSettings;
import com.riksa.admin.service.SettingsService;
import com.riksa.auth.repository.UserRepository;
import com.riksa.chatAi.dto.SendChatMessageRequest;
import com.riksa.chatAi.repository.ChatMessageRepository;
import com.riksa.chatAi.repository.ChatSessionRepository;
import com.riksa.common.exception.RateLimitExceededException;
import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.common.exception.ServiceUnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatAiServiceSettingsTest {

    @Mock private ChatSessionRepository sessions;
    @Mock private ChatMessageRepository messages;
    @Mock private UserRepository users;
    @Mock private SettingsService settingsService;
    @InjectMocks private ChatAiService chatAiService;

    private final UUID userId = UUID.randomUUID();
    private final SendChatMessageRequest request = new SendChatMessageRequest(null, "Explain fractions", null);

    @Test
    void sendRefusesWhenTutorIsDisabled() {
        ReflectionTestUtils.setField(chatAiService, "apiKey", "configured-key");
        when(settingsService.getAiChat()).thenReturn(new AiChatSettings(false, 0, "system"));

        assertThatThrownBy(() -> chatAiService.send(userId, request))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("AI tutor is currently disabled");
        verify(messages, never()).countUserMessagesByUserIdAndCreatedAtInRange(any(), any(), any());
    }

    @Test
    void zeroDailyLimitDoesNotCountOrRejectUserMessages() {
        ReflectionTestUtils.setField(chatAiService, "apiKey", "configured-key");
        when(settingsService.getAiChat()).thenReturn(new AiChatSettings(true, 0, "system"));

        assertThatThrownBy(() -> chatAiService.send(userId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
        verify(messages, never()).countUserMessagesByUserIdAndCreatedAtInRange(any(), any(), any());
    }

    @Test
    void sendReturns429WhenUserHasReachedDailyLimit() {
        ReflectionTestUtils.setField(chatAiService, "apiKey", "configured-key");
        when(settingsService.getAiChat()).thenReturn(new AiChatSettings(true, 2, "system"));
        when(messages.countUserMessagesByUserIdAndCreatedAtInRange(eq(userId), any(), any())).thenReturn(2L);

        assertThatThrownBy(() -> chatAiService.send(userId, request))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Daily AI tutor message limit reached");
        verify(users, never()).findById(userId);
        verify(sessions, never()).save(any());
    }
}
