package org.chovy.canvas.domain.bi.query;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BiQueryHistoryReaderTest {

    @Test
    void attributesSlowQueriesByDatasetFailureCacheMissOverPolicyAndRows() {
        BiQueryHistoryReader reader = (tenantId, limit) -> List.of(
                item(1L, "canvas_daily_stats", 20_000L, "FAILED", 220_000),
                item(2L, "canvas_daily_stats", 17_000L, "CACHE_HIT", 12_000),
                item(3L, "canvas_daily_stats", 16_000L, "SUCCESS", 180_000),
                item(4L, "node_daily_stats", 6_000L, "SUCCESS", 8_000));
        BiQueryGovernancePolicy policy = new BiQueryGovernancePolicy(
                12_000L,
                500_000,
                Map.of("canvas_daily_stats", new BiQueryGovernancePolicy.DatasetPolicy(15_000L, 120_000)));

        BiQueryGovernanceSummary summary = reader.governanceSummary(7L, 100, policy);

        BiQueryGovernanceSummary.DatasetQueryStats canvas = summary.datasets().get(0);
        assertThat(canvas.datasetKey()).isEqualTo("canvas_daily_stats");
        assertThat(canvas.slowQueries()).isEqualTo(3);
        assertThat(canvas.slowFailures()).isEqualTo(1);
        assertThat(canvas.slowCacheMisses()).isEqualTo(2);
        assertThat(canvas.maxOverPolicyMs()).isEqualTo(5_000L);
        assertThat(canvas.maxRowCount()).isEqualTo(220_000);
    }

    private BiQueryHistoryItem item(Long id, String datasetKey, long durationMs, String status, int rowCount) {
        return new BiQueryHistoryItem(
                id,
                datasetKey,
                "alice",
                rowCount,
                durationMs,
                status,
                "hash-" + id,
                null,
                LocalDateTime.parse("2026-06-05T02:00:00").plusMinutes(id));
    }
}
