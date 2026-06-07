package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClientTest {

    @Test
    void querySloFailsClosedWhenSqlIsBlank() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClient client = client(jdbcTemplate, " ");

        assertThatThrownBy(client::querySlo)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Doris query SLO SQL is not configured");
    }

    @Test
    void querySloMapsConfiguredSqlRows() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        String sql = "select * from enterprise_olap_query_slo";
        LocalDateTime measuredAt = LocalDateTime.of(2026, 6, 6, 2, 30);
        when(jdbcTemplate.queryForList(sql)).thenReturn(List.of(Map.of(
                "profile_key", "bi_dashboard",
                "workload_group", "bi",
                "sample_count", 30L,
                "error_count", 0L,
                "p95_latency_ms", 900.5,
                "p99_latency_ms", 1_500.0,
                "max_queue_wait_ms", 200.0,
                "max_peak_memory_bytes", 536_870_912L,
                "measured_at", measuredAt)));
        HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClient client = client(jdbcTemplate, sql);

        List<CdpWarehouseEnterpriseOlapDorisEvidenceClient.QuerySloEvidence> rows = client.querySlo();

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.profileKey()).isEqualTo("bi_dashboard");
            assertThat(row.workloadGroup()).isEqualTo("bi");
            assertThat(row.sampleCount()).isEqualTo(30);
            assertThat(row.errorCount()).isZero();
            assertThat(row.p95LatencyMs()).isEqualTo(900.5);
            assertThat(row.p99LatencyMs()).isEqualTo(1_500.0);
            assertThat(row.maxQueueWaitMs()).isEqualTo(200.0);
            assertThat(row.maxPeakMemoryBytes()).isEqualTo(536_870_912L);
            assertThat(row.measuredAt()).isEqualTo(measuredAt);
        });
        verify(jdbcTemplate).queryForList(sql);
    }

    private HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClient client(
            JdbcTemplate jdbcTemplate,
            String querySloSql) {
        return new HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClient(
                provider(jdbcTemplate),
                new CdpWarehouseDorisPrometheusMetricsParser(),
                (url, timeout) -> "",
                List.of(),
                List.of(),
                Duration.ofSeconds(1),
                querySloSql);
    }

    @SuppressWarnings("unchecked")
    private <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
}
