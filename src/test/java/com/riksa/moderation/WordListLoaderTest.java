package com.riksa.moderation;

import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WordListLoaderTest {

    @Test
    void ignoresCommentsAndBlankLinesAndReadsTermWeights() {
        var parsed = WordListLoader.parse(new StringReader("# header\n\nterm\t2\n"), "fixture.txt");

        assertThat(parsed).containsEntry("term", 2);
    }

    @Test
    void collapsesRepeatedRunsInTermsUsingTheSameRuleAsIncomingText() {
        var parsed = WordListLoader.parse(new StringReader("fuuuuck\t3\n"), "fixture.txt");

        assertThat(parsed).containsEntry("fuuck", 3);
        assertThat(new ContentModerationService(new WordListLoader(
                new org.springframework.core.io.ByteArrayResource("fuuuuck\t3\n".getBytes()),
                new org.springframework.core.io.ByteArrayResource(new byte[0]),
                new org.springframework.core.io.ByteArrayResource(new byte[0])),
                new ModerationProperties(5, 2, java.util.List.of(), java.time.Duration.ofHours(24)))
                .assess(new ModerationInput(null, null, ModerationSurface.POST, "", "fuuuuck"), ignored -> false)
                .verdict()).isEqualTo(ModerationVerdict.BLOCK);
    }

    @Test
    void malformedLineFailsFastWithFileAndLine() {
        assertThatThrownBy(() -> new WordListLoader(
                new org.springframework.core.io.ByteArrayResource("not-a-tab-line".getBytes()),
                new org.springframework.core.io.ByteArrayResource(new byte[0]),
                new org.springframework.core.io.ByteArrayResource(new byte[0])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Malformed moderation word list")
                .hasMessageContaining("line 1");
    }
}
