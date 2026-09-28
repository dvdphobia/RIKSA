package com.riksa.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riksa.admin.dto.settings.AiChatSettings;
import com.riksa.admin.dto.settings.AppFlagsSettings;
import com.riksa.admin.dto.settings.BannedWordsSettings;
import com.riksa.admin.dto.settings.XpRulesSettings;
import com.riksa.admin.repository.AppSettingRepository;
import com.riksa.common.audit.AuditLogService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock private AppSettingRepository appSettingRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private SettingsService settingsService;

    @Test
    void getAll_returnsTypedDefaultsForEveryKnownKey() {
        when(appSettingRepository.findById(any())).thenReturn(Optional.empty());

        var all = settingsService.getAll();

        assertThat(all.get("xp.rules")).isEqualTo(new XpRulesSettings(0));
        assertThat(all.get("app.flags")).isEqualTo(new AppFlagsSettings(false, "", "1.0.0", "1.0.0"));
        assertThat(all.get("ai.chat")).isEqualTo(new AiChatSettings(true, 50,
                "You are Riksa AI Tutor, a patient bilingual learning coach for Cambodian students.\n"
                        + "Explain concepts step by step, ask a short checking question, and match the learner's\n"
                        + "language (Khmer or English). Teach reasoning instead of giving unsupported final answers.\n"
                        + "Never claim to have performed actions outside this conversation.\n"));
        assertThat(all.get("moderation.bannedWords")).isEqualTo(new BannedWordsSettings(List.of()));
    }

    @Test
    void update_rejectsInvalidValueUsingTypedBeanValidation() throws Exception {
        JsonNode body = new ObjectMapper().readTree("{\"dailyLessonXpCap\":-1}");
        SettingsService service = new SettingsService(appSettingRepository, auditLogService,
                new ObjectMapper(), Validation.buildDefaultValidatorFactory().getValidator());

        assertThatThrownBy(() -> service.update("xp.rules", body, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dailyLessonXpCap");

        verify(appSettingRepository, never()).save(any());
        verify(auditLogService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void update_rejectsUnknownKey() throws Exception {
        JsonNode body = new ObjectMapper().readTree("{}");
        SettingsService service = new SettingsService(appSettingRepository, auditLogService,
                new ObjectMapper(), Validation.buildDefaultValidatorFactory().getValidator());

        assertThatThrownBy(() -> service.update("unknown.key", body, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown setting key: unknown.key");
    }
}
