package org.chovy.canvas.domain.bi.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarketingBiDatasetRegistryTest {

    @Test
    void exposesCanvasDailyStatsDatasetWithCoreFieldsAndMetrics() {
        BiDatasetSpec dataset = MarketingBiDatasetRegistry.dataset("canvas_daily_stats");

        assertThat(dataset.datasetKey()).isEqualTo("canvas_daily_stats");
        assertThat(dataset.tableExpression()).isEqualTo("canvas_dws.canvas_daily_stats");
        assertThat(dataset.tenantColumn()).isEqualTo("tenant_id");
        assertThat(dataset.fields().keySet()).contains(
                "stat_date",
                "canvas_id",
                "canvas_name",
                "trigger_type",
                "total_executions",
                "success_count",
                "fail_count",
                "unique_users",
                "avg_duration_ms"
        );
        assertThat(dataset.metrics().keySet()).contains(
                "total_executions",
                "success_count",
                "fail_count",
                "unique_users",
                "avg_duration_ms",
                "success_rate"
        );
    }
}
