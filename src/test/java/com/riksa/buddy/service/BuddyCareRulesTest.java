package com.riksa.buddy.service;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BuddyCareRulesTest {

    private final BuddyCareRules rules = new BuddyCareRules();
    private final OffsetDateTime now = OffsetDateTime.of(2026, 9, 24, 10, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void settleUsesCompleteSeventyTwoMinuteIntervals() {
        assertThat(rules.settle(100, now, now)).isEqualTo(100);
        assertThat(rules.settle(100, now.minusMinutes(71), now)).isEqualTo(100);
        assertThat(rules.settle(100, now.minusMinutes(72), now)).isEqualTo(99);
        assertThat(rules.settle(100, now.minusMinutes(7200), now)).isZero();
        assertThat(rules.settle(1, now.minusMinutes(7200), now)).isZero();
    }

    @Test
    void mapsGainsAndDailyCaps() {
        assertThat(rules.gainFor("LESSON")).isEqualTo(new BuddyCareRules.Gain(35, 10));
        assertThat(rules.gainFor("BLOG_READ")).isEqualTo(new BuddyCareRules.Gain(10, 3));
        assertThat(rules.gainFor("POST")).isEqualTo(new BuddyCareRules.Gain(10, 3));
        assertThat(rules.gainFor("BLOG_PUBLISHED")).isEqualTo(new BuddyCareRules.Gain(20, 8));
        assertThat(rules.dailyCap("LESSON")).isNull();
        assertThat(rules.dailyCap("BLOG_READ")).isEqualTo(2);
        assertThat(rules.dailyCap("POST")).isEqualTo(1);
        assertThat(rules.dailyCap("BLOG_PUBLISHED")).isEqualTo(1);
        assertThatThrownBy(() -> rules.gainFor("UNKNOWN")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bondLevelsUseTheSquareRootThresholds() {
        assertThat(rules.bondLevel(0)).isEqualTo(1);
        assertThat(rules.bondLevel(14)).isEqualTo(1);
        assertThat(rules.bondLevel(15)).isEqualTo(2);
        assertThat(rules.bondLevel(60)).isEqualTo(3);
        assertThat(rules.bondLevel(135)).isEqualTo(4);
        assertThat(rules.bondLevel(240)).isEqualTo(5);
    }
}
