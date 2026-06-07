package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CdpWarehouseDorisPrometheusMetricsParserTest {

    @Test
    void parsesPrometheusMetricsWithLabelsCommentsAndScientificNotation() {
        CdpWarehouseDorisPrometheusMetricsParser parser = new CdpWarehouseDorisPrometheusMetricsParser();

        CdpWarehouseDorisPrometheusMetricsParser.ParsedMetrics metrics = parser.parse(
                "http://fe:8030/metrics",
                "FE",
                """
                        # HELP doris_fe_qps Queries per second
                        # TYPE doris_fe_qps gauge
                        doris_fe_qps 42
                        doris_fe_query_err_rate 1.5e-2
                        doris_fe_query_latency_ms{quantile="0.99"} 388.25
                        doris_fe_tablet_max_compaction_score{backend="10.0.0.1:9556"} 8
                        malformed line ignored
                        """,
                LocalDateTime.of(2026, 6, 6, 1, 0));

        assertThat(metrics.endpoint()).isEqualTo("http://fe:8030/metrics");
        assertThat(metrics.role()).isEqualTo("FE");
        assertThat(metrics.measuredAt()).isEqualTo(LocalDateTime.of(2026, 6, 6, 1, 0));
        assertThat(metrics.max("doris_fe_qps")).isEqualTo(42.0);
        assertThat(metrics.max("doris_fe_query_err_rate")).isEqualTo(0.015);
        assertThat(metrics.max("doris_fe_query_latency_ms")).isEqualTo(388.25);
        assertThat(metrics.max("doris_fe_tablet_max_compaction_score")).isEqualTo(8.0);
        assertThat(metrics.hasMetric("malformed")).isFalse();
    }

    @Test
    void missingMetricReturnsNaNInsteadOfAccidentallyPassing() {
        CdpWarehouseDorisPrometheusMetricsParser parser = new CdpWarehouseDorisPrometheusMetricsParser();

        CdpWarehouseDorisPrometheusMetricsParser.ParsedMetrics metrics = parser.parse(
                "http://be:8040/metrics",
                "BE",
                "doris_be_tablet_base_max_compaction_score 5\n",
                LocalDateTime.of(2026, 6, 6, 1, 0));

        assertThat(metrics.hasMetric("doris_fe_qps")).isFalse();
        assertThat(Double.isNaN(metrics.max("doris_fe_qps"))).isTrue();
    }
}
