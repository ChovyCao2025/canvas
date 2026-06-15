package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.chovy.canvas.cdp.api.CdpWarehouseMetricLineageFacade;
import org.chovy.canvas.cdp.api.CdpWarehouseMetricLineageFacade.ChartImpactView;
import org.chovy.canvas.cdp.api.CdpWarehouseMetricLineageFacade.DashboardImpactView;
import org.chovy.canvas.cdp.api.CdpWarehouseMetricLineageFacade.FieldDependencyView;
import org.chovy.canvas.cdp.api.CdpWarehouseMetricLineageFacade.MetricImpactView;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpWarehouseMetricLineageControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 0L;
    private static final Long HEADER_TENANT_ID = 42L;

    @Test
    void exposesLegacyImpactRouteWithQueryMappingTenantHeaderAndCompatibilityEnvelope() {
        RecordingMetricLineageFacade facade = new RecordingMetricLineageFacade();

        webClient(facade)
                .get()
                .uri("/warehouse/metric-lineage/impact?datasetKey=canvas_daily_stats&metricKey=success_rate")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.datasetKey").isEqualTo("canvas_daily_stats")
                .jsonPath("$.data.metricKey").isEqualTo("success_rate")
                .jsonPath("$.data.expression").isEqualTo("SUM(success_count)")
                .jsonPath("$.data.fieldDependencies[0].fieldKey").isEqualTo("success_count");

        assertThat(facade.calls).containsExactly(
                new ImpactCall(HEADER_TENANT_ID, "canvas_daily_stats", "success_rate"));
    }

    @Test
    void defaultsTenantWhenLegacyImpactRouteHasNoTenantHeader() {
        RecordingMetricLineageFacade facade = new RecordingMetricLineageFacade();

        webClient(facade)
                .get()
                .uri("/warehouse/metric-lineage/impact?datasetKey=canvas_daily_stats&metricKey=success_rate")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue());

        assertThat(facade.calls).containsExactly(
                new ImpactCall(DEFAULT_TENANT_ID, "canvas_daily_stats", "success_rate"));
    }

    private static WebTestClient webClient(CdpWarehouseMetricLineageFacade facade) {
        return WebTestClient.bindToController(new CdpWarehouseMetricLineageController(facade)).build();
    }

    private record ImpactCall(Long tenantId, String datasetKey, String metricKey) {
    }

    private static final class RecordingMetricLineageFacade implements CdpWarehouseMetricLineageFacade {
        private final List<ImpactCall> calls = new ArrayList<>();

        @Override
        public MetricImpactView impact(Long tenantId, String datasetKey, String metricKey) {
            calls.add(new ImpactCall(tenantId, datasetKey, metricKey));
            return new MetricImpactView(
                    tenantId,
                    datasetKey,
                    metricKey,
                    "SUM(success_count)",
                    "PERCENT",
                    List.of("stat_date"),
                    List.of(new FieldDependencyView("success_count", "EXPRESSION_FIELD")),
                    List.of(),
                    List.of(),
                    null,
                    List.of(new ChartImpactView("chart-success", "Success", "LINE", "ACTIVE", List.of("stat_date"))),
                    List.of(new DashboardImpactView("dashboard-ops", "Ops", "ACTIVE", 1, List.of("widget-1"))),
                    List.of());
        }
    }
}
