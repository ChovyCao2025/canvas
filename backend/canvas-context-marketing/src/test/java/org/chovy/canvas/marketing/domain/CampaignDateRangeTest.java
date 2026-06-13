package org.chovy.canvas.marketing.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CampaignDateRangeTest {

    @Test
    void allowsOpenEndedRanges() {
        CampaignDateRange open = CampaignDateRange.of(null, null);
        CampaignDateRange startOnly = CampaignDateRange.of(LocalDateTime.parse("2026-06-01T00:00:00"), null);
        CampaignDateRange endOnly = CampaignDateRange.of(null, LocalDateTime.parse("2026-06-30T23:59:00"));

        assertThat(open.startAt()).isNull();
        assertThat(startOnly.startAt()).isEqualTo(LocalDateTime.parse("2026-06-01T00:00:00"));
        assertThat(endOnly.endAt()).isEqualTo(LocalDateTime.parse("2026-06-30T23:59:00"));
    }

    @Test
    void rejectsEndBeforeStart() {
        assertThatThrownBy(() -> CampaignDateRange.of(
                LocalDateTime.parse("2026-06-30T00:00:00"),
                LocalDateTime.parse("2026-06-01T00:00:00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endAt must be after startAt");
    }
}
