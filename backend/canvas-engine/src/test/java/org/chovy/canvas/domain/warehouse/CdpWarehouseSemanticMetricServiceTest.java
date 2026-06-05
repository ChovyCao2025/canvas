package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiFieldSpec;
import org.chovy.canvas.domain.bi.query.BiMetricSpec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CdpWarehouseSemanticMetricServiceTest {

    @Test
    void listsMetricsForSpecificDatasetWithAllowedDimensions() {
        CdpWarehouseSemanticMetricService service =
                new CdpWarehouseSemanticMetricService(resolver());

        List<CdpWarehouseSemanticMetricService.SemanticMetricView> rows =
                service.listMetrics(9L, "canvas_daily_stats");

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).tenantId()).isEqualTo(9L);
        assertThat(rows.get(0).datasetKey()).isEqualTo("canvas_daily_stats");
        assertThat(rows.get(0).metricKey()).isEqualTo("success_rate");
        assertThat(rows.get(0).allowedDimensions()).containsExactly("stat_date", "canvas_id");
        assertThat(rows.get(0).dimensionPolicy()).isEqualTo("ALLOW_LIST");
    }

    @Test
    void emptyAllowedDimensionsMeansAllDatasetDimensions() {
        CdpWarehouseSemanticMetricService service =
                new CdpWarehouseSemanticMetricService(new BiDatasetSpecResolver() {
                    @Override
                    public BiDatasetSpec dataset(String datasetKey, Long tenantId) {
                        return CdpWarehouseSemanticMetricServiceTest.this.dataset(
                                new BiMetricSpec("free_metric", "SUM(total_executions)", "NUMBER"));
                    }

                    @Override
                    public List<BiDatasetSpec> datasets(Long tenantId) {
                        return List.of(CdpWarehouseSemanticMetricServiceTest.this.dataset("canvas_daily_stats", List.of(
                                new BiMetricSpec("free_metric", "SUM(total_executions)", "NUMBER"))));
                    }
                });

        List<CdpWarehouseSemanticMetricService.SemanticMetricView> rows =
                service.listMetrics(9L, null);

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.allowedDimensions()).isEmpty();
            assertThat(row.dimensionPolicy()).isEqualTo("ALL_DATASET_DIMENSIONS");
        });
    }

    private BiDatasetSpecResolver resolver() {
        return new BiDatasetSpecResolver() {
            @Override
            public BiDatasetSpec dataset(String datasetKey, Long tenantId) {
                return CdpWarehouseSemanticMetricServiceTest.this.dataset(new BiMetricSpec(
                        "success_rate",
                        "CASE WHEN SUM(total_executions) > 0 THEN SUM(success_count) / SUM(total_executions) ELSE 0 END",
                        "PERCENT",
                        List.of("stat_date", "canvas_id")));
            }

            @Override
            public List<BiDatasetSpec> datasets(Long tenantId) {
                return List.of(CdpWarehouseSemanticMetricServiceTest.this.dataset("canvas_daily_stats", List.of(
                        new BiMetricSpec("success_rate", "SUM(success_count)", "NUMBER",
                                List.of("stat_date", "canvas_id")))));
            }
        };
    }

    private BiDatasetSpec dataset(BiMetricSpec metric) {
        return dataset("canvas_daily_stats", List.of(metric));
    }

    private BiDatasetSpec dataset(String datasetKey, List<BiMetricSpec> metrics) {
        return new BiDatasetSpec(
                datasetKey,
                "canvas_dws.canvas_daily_stats",
                "tenant_id",
                Map.of(
                        "stat_date", new BiFieldSpec("stat_date", "stat_date", BiFieldSpec.Role.DIMENSION, "DATE"),
                        "canvas_id", new BiFieldSpec("canvas_id", "canvas_id", BiFieldSpec.Role.DIMENSION, "NUMBER"),
                        "total_executions", new BiFieldSpec(
                                "total_executions", "total_executions", BiFieldSpec.Role.MEASURE, "NUMBER")),
                metrics.stream().collect(java.util.stream.Collectors.toMap(
                        BiMetricSpec::metricKey,
                        metric -> metric)));
    }
}
