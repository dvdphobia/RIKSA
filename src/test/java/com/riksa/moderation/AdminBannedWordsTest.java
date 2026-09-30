package com.riksa.moderation;

import com.riksa.admin.dto.settings.BannedWordsSettings;
import com.riksa.admin.service.SettingsService;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminBannedWordsTest {

    @Test
    void adminConfiguredEnglishWordBlocksMatchingWholeToken() {
        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.getBannedWords()).thenReturn(new BannedWordsSettings(List.of("customword")));
        ContentModerationService service = new ContentModerationService(new WordListLoader(), properties(),
                settingsService);

        ModerationResult result = service.assess(input("This has CUSTOMWORD in it"), null);

        assertThat(result.verdict()).isEqualTo(ModerationVerdict.BLOCK);
    }

    @Test
    void adminConfiguredEnglishWordUsesWholeTokenMatching() {
        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.getBannedWords()).thenReturn(new BannedWordsSettings(List.of("customword")));
        ContentModerationService service = new ContentModerationService(new WordListLoader(), properties(),
                settingsService);

        ModerationResult result = service.assess(input("customwording"), null);

        assertThat(result.verdict()).isEqualTo(ModerationVerdict.ALLOW);
    }

    private ModerationInput input(String body) {
        return new ModerationInput(UUID.randomUUID(), OffsetDateTime.now().minusDays(5),
                ModerationSurface.POST, "", body);
    }

    private ModerationProperties properties() {
        return new ModerationProperties(5, 2, List.of("riksa.app"), java.time.Duration.ofHours(24));
    }
}
