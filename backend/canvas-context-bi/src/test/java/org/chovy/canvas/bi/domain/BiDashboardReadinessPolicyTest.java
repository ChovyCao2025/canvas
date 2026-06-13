package org.chovy.canvas.bi.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BiDashboardReadinessPolicyTest {

    private final BiDashboardReadinessPolicy policy = new BiDashboardReadinessPolicy();

    @Test
    void dashboardIsBlockedWhenReferencedChartIsMissingOrDatasetIsNotPublished() {
        BiDashboard dashboard = dashboard(List.of("orders-trend", "gmv-card"));
        BiDataset draftDataset = dataset(20L, "orders-daily", "DRAFT");
        BiChart chart = chart(30L, "orders-trend", draftDataset.id(), draftDataset.datasetKey().value(), "PUBLISHED");

        BiDashboardReadinessReport report = policy.evaluate(dashboard, List.of(chart), List.of(draftDataset));

        assertThat(report.productionReady()).isFalse();
        assertThat(report.blockers())
                .extracting(BiDashboardReadinessIssue::code)
                .containsExactly("DATASET_NOT_PUBLISHED", "MISSING_CHART");
        assertThat(report.publishedChartCount()).isEqualTo(1);
        assertThat(report.draftDatasetCount()).isEqualTo(1);
    }

    @Test
    void dashboardIsProductionReadyWhenAllChartsAndDatasetsArePublished() {
        BiDashboard dashboard = dashboard(List.of("orders-trend"));
        BiDataset dataset = dataset(20L, "orders-daily", "PUBLISHED");
        BiChart chart = chart(30L, "orders-trend", dataset.id(), dataset.datasetKey().value(), "PUBLISHED");

        BiDashboardReadinessReport report = policy.evaluate(dashboard, List.of(chart), List.of(dataset));

        assertThat(report.productionReady()).isTrue();
        assertThat(report.status()).isEqualTo("READY");
        assertThat(report.blockers()).isEmpty();
    }

    private static BiDashboard dashboard(List<String> chartKeys) {
        return new BiDashboard(
                10L,
                7L,
                5L,
                BiResourceKey.of("marketing-overview", "dashboardKey"),
                "Marketing overview",
                "Executive daily view",
                Map.of("theme", "light"),
                Map.of("region", "CN"),
                chartKeys,
                BiResourceStatus.DRAFT,
                1,
                "admin",
                LocalDateTime.parse("2026-06-01T00:00:00"),
                LocalDateTime.parse("2026-06-01T00:00:00"));
    }

    private static BiDataset dataset(Long id, String key, String status) {
        return new BiDataset(
                id,
                7L,
                5L,
                BiResourceKey.of(key, "datasetKey"),
                "Orders",
                "SQL",
                99L,
                "fact_order",
                "tenant_id",
                Map.of(),
                List.of(),
                List.of(),
                BiResourceStatus.from(status),
                "admin",
                LocalDateTime.parse("2026-06-01T00:00:00"),
                LocalDateTime.parse("2026-06-01T00:00:00"));
    }

    private static BiChart chart(Long id, String key, Long datasetId, String datasetKey, String status) {
        return new BiChart(
                id,
                7L,
                5L,
                BiResourceKey.of(key, "chartKey"),
                "Orders trend",
                "LINE",
                datasetId,
                BiResourceKey.of(datasetKey, "datasetKey"),
                Map.of("dimensions", List.of("date")),
                Map.of("palette", "ops"),
                Map.of(),
                BiResourceStatus.from(status),
                "admin",
                LocalDateTime.parse("2026-06-01T00:00:00"),
                LocalDateTime.parse("2026-06-01T00:00:00"));
    }
}
