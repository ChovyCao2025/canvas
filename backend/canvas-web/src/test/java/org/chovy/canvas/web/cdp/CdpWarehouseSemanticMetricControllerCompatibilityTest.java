package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.chovy.canvas.cdp.api.CdpWarehouseSemanticMetricFacade;
import org.chovy.canvas.cdp.api.CdpWarehouseSemanticMetricFacade.SemanticMetricView;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpWarehouseSemanticMetricControllerCompatibilityTest {

    @Test
    void exposesLegacyWarehouseSemanticMetricsRouteWithCompatibilityEnvelope() {
        RecordingFacade facade = new RecordingFacade();
        WebTestClient client = webClient(facade);

        client.get()
                .uri("/warehouse/semantic-metrics?datasetKey=canvas_daily_stats")
                .header("X-Tenant-Id", "9")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data[0].tenantId").isEqualTo(9)
                .jsonPath("$.data[0].datasetKey").isEqualTo("canvas_daily_stats")
                .jsonPath("$.data[0].metricKey").isEqualTo("success_rate")
                .jsonPath("$.data[0].expression").isEqualTo("SUM(success_count)")
                .jsonPath("$.data[0].valueType").isEqualTo("PERCENT")
                .jsonPath("$.data[0].allowedDimensions[0]").isEqualTo("stat_date")
                .jsonPath("$.data[0].allowedDimensions[1]").isEqualTo("canvas_id")
                .jsonPath("$.data[0].dimensionPolicy").isEqualTo("ALLOW_LIST")
                .jsonPath("$.data[0].source").isEqualTo("BI_DATASET_SPEC");

        assertThat(facade.operations).containsExactly("listMetrics");
        assertThat(facade.lastTenantId).isEqualTo(9L);
        assertThat(facade.lastDatasetKey).isEqualTo("canvas_daily_stats");
    }

    @Test
    void defaultsTenantAndPassesOptionalDatasetKeyAsNullWhenAbsent() {
        RecordingFacade facade = new RecordingFacade();

        webClient(facade).get()
                .uri("/warehouse/semantic-metrics")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].tenantId").isEqualTo(0)
                .jsonPath("$.data[0].datasetKey").isEqualTo("canvas_daily_stats");

        assertThat(facade.lastTenantId).isEqualTo(0L);
        assertThat(facade.lastDatasetKey).isNull();
    }

    private static WebTestClient webClient(CdpWarehouseSemanticMetricFacade facade) {
        return WebTestClient.bindToController(new CdpWarehouseSemanticMetricController(facade)).build();
    }

    private static final class RecordingFacade implements CdpWarehouseSemanticMetricFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private String lastDatasetKey;

        @Override
        public List<SemanticMetricView> listMetrics(Long tenantId, String datasetKey) {
            operations.add("listMetrics");
            lastTenantId = tenantId;
            lastDatasetKey = datasetKey;
            return List.of(new SemanticMetricView(
                    tenantId,
                    "canvas_daily_stats",
                    "success_rate",
                    "SUM(success_count)",
                    "PERCENT",
                    List.of("stat_date", "canvas_id"),
                    "ALLOW_LIST",
                    "BI_DATASET_SPEC"));
        }
    }
}
