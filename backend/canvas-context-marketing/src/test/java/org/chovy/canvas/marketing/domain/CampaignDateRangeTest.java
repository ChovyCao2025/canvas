package org.chovy.canvas.marketing.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证CampaignDateRange的关键兼容行为。
 */
class CampaignDateRangeTest {

    /**
     * 验证 allows open ended ranges 场景的兼容行为。
     */
    @Test
    void allowsOpenEndedRanges() {
        CampaignDateRange open = CampaignDateRange.of(null, null);
        CampaignDateRange startOnly = CampaignDateRange.of(LocalDateTime.parse("2026-06-01T00:00:00"), null);
        CampaignDateRange endOnly = CampaignDateRange.of(null, LocalDateTime.parse("2026-06-30T23:59:00"));

        assertThat(open.startAt()).isNull();
        assertThat(startOnly.startAt()).isEqualTo(LocalDateTime.parse("2026-06-01T00:00:00"));
        assertThat(endOnly.endAt()).isEqualTo(LocalDateTime.parse("2026-06-30T23:59:00"));
    }

    /**
     * 验证 rejects end before start 场景的兼容行为。
     */
    @Test
    void rejectsEndBeforeStart() {
        assertThatThrownBy(() -> CampaignDateRange.of(
                LocalDateTime.parse("2026-06-30T00:00:00"),
                LocalDateTime.parse("2026-06-01T00:00:00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endAt must be after startAt");
    }
}
