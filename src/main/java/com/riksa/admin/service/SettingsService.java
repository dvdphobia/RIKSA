package com.riksa.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riksa.admin.dto.settings.AiChatSettings;
import com.riksa.admin.dto.settings.AppFlagsSettings;
import com.riksa.admin.dto.settings.BannedWordsSettings;
import com.riksa.admin.dto.settings.XpRulesSettings;
import com.riksa.admin.entity.AppSetting;
import com.riksa.admin.repository.AppSettingRepository;
import com.riksa.common.audit.AuditLogService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SettingsService {

    public static final String XP_RULES = "xp.rules";
    public static final String APP_FLAGS = "app.flags";
    public static final String AI_CHAT = "ai.chat";
    public static final String BANNED_WORDS = "moderation.bannedWords";

    public static final String DEFAULT_SYSTEM_PROMPT = """
            You are Riksa AI Tutor, a patient bilingual learning coach for Cambodian students.
            Explain concepts step by step, ask a short checking question, and match the learner's
            language (Khmer or English). Teach reasoning instead of giving unsupported final answers.
            Never claim to have performed actions outside this conversation.
            """;

    private final AppSettingRepository appSettingRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Transactional(readOnly = true)
    public XpRulesSettings getXpRules() {
        return get(XP_RULES, XpRulesSettings.class, new XpRulesSettings(0));
    }

    @Transactional(readOnly = true)
    public AppFlagsSettings getAppFlags() {
        return get(APP_FLAGS, AppFlagsSettings.class, new AppFlagsSettings(false, "", "1.0.0", "1.0.0"));
    }

    @Transactional(readOnly = true)
    public AiChatSettings getAiChat() {
        return get(AI_CHAT, AiChatSettings.class, new AiChatSettings(true, 0, DEFAULT_SYSTEM_PROMPT));
    }

    @Transactional(readOnly = true)
    public BannedWordsSettings getBannedWords() {
        return get(BANNED_WORDS, BannedWordsSettings.class, new BannedWordsSettings(List.of()));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAll() {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put(XP_RULES, getXpRules());
        settings.put(APP_FLAGS, getAppFlags());
        settings.put(AI_CHAT, getAiChat());
        settings.put(BANNED_WORDS, getBannedWords());
        return settings;
    }

    @Transactional
    public Object update(String key, JsonNode body, UUID actorId) {
        Object value = switch (key) {
            case XP_RULES -> readAndValidate(body, XpRulesSettings.class);
            case APP_FLAGS -> readAndValidate(body, AppFlagsSettings.class);
            case AI_CHAT -> readAndValidate(body, AiChatSettings.class);
            case BANNED_WORDS -> readAndValidate(body, BannedWordsSettings.class);
            default -> throw new IllegalArgumentException("Unknown setting key: " + key);
        };

        AppSetting setting = appSettingRepository.findById(key).orElseGet(AppSetting::new);
        setting.setSettingKey(key);
        setting.setValue(objectMapper.valueToTree(value));
        setting.setUpdatedAt(OffsetDateTime.now());
        setting.setUpdatedBy(actorId);
        appSettingRepository.save(setting);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("key", key);
        details.put("value", objectMapper.convertValue(value, Map.class));
        auditLogService.record(actorId, "APP_SETTING_UPDATE", "APP_SETTING", null, details);
        return value;
    }

    private <T> T get(String key, Class<T> type, T defaultValue) {
        return appSettingRepository.findById(key)
                .map(setting -> objectMapper.convertValue(setting.getValue(), type))
                .orElse(defaultValue);
    }

    private <T> T readAndValidate(JsonNode body, Class<T> type) {
        if (body == null || !body.isObject()) {
            throw new IllegalArgumentException("Setting value must be a JSON object");
        }
        final T value;
        try {
            value = objectMapper.treeToValue(body, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid setting value: " + e.getMessage(), e);
        }
        Set<ConstraintViolation<T>> violations = validator.validate(value);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .sorted()
                    .reduce((left, right) -> left + "; " + right)
                    .orElse("Invalid setting value");
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
