package org.chovy.canvas.domain.analytics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalyticsQueryGuardTest {

    private final AnalyticsQueryGuard guard = new AnalyticsQueryGuard();

    @Test
    void validatesDateRangeAndNormalizesDateStrings() {
        AnalyticsQueryGuard.DateRange range = guard.validateDateRange("2026-01-01", "2026-01-31");

        assertThat(range.startDate()).isEqualTo("2026-01-01");
        assertThat(range.endDate()).isEqualTo("2026-01-31");
    }

    @Test
    void rejectsTooWideDateRange() {
        assertThatThrownBy(() -> guard.validateDateRange("2025-01-01", "2026-02-01"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed");
    }

    @Test
    void rejectsUnsafeAttributePaths() {
        assertThatThrownBy(() -> guard.requireAttributePath("profile['vip']"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dot-separated identifier path");
    }

    @Test
    void clampsPageSizeToQueryLimit() {
        AnalyticsQueryGuard.PageRequest pageRequest = guard.normalizePageRequest(3, 999);

        assertThat(pageRequest.size()).isEqualTo(AnalyticsQueryGuard.MAX_PAGE_SIZE);
        assertThat(pageRequest.offset()).isEqualTo(400);
    }

    @Test
    void rejectsNegativeTenantId() {
        assertThatThrownBy(() -> guard.requireTenantId(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId");
    }
}
