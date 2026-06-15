package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseQualityFacade;
import org.chovy.canvas.cdp.application.CdpWarehouseQualityApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpWarehouseQualityControllerCompatibilityTest {

    @Test
    void exposesLegacyQualityRoutesWithCompatibilityEnvelope() {
        WebTestClient client = webClient(new CdpWarehouseQualityApplicationService());

        client.post()
                .uri("/warehouse/quality/reconcile-ods")
                .header("X-Tenant-Id", "9")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "from", "2026-06-05T10:00:00",
                        "to", "2026-06-05T11:00:00",
                        "tolerance", 1,
                        "operator", "alice"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.tenantId").isEqualTo(9)
                .jsonPath("$.data.checkType").isEqualTo("ODS_COUNT")
                .jsonPath("$.data.status").isEqualTo("WARN")
                .jsonPath("$.data.createdBy").isEqualTo("alice")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        client.post()
                .uri("/warehouse/quality/aggregate-lag")
                .header("X-Tenant-Id", "9")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "now", "2026-06-05T12:00:00",
                        "maxLagMinutes", 30,
                        "operator", "bob"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.checkType").isEqualTo("AGGREGATE_LAG")
                .jsonPath("$.data.createdBy").isEqualTo("bob");

        client.get()
                .uri("/warehouse/quality/checks?limit=2")
                .header("X-Tenant-Id", "9")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].tenantId").isEqualTo(9)
                .jsonPath("$.data[0].checkType").isEqualTo("AGGREGATE_LAG")
                .jsonPath("$.data[1].checkType").isEqualTo("ODS_COUNT");
    }

    @Test
    void defaultsTenantLimitThresholdAndNullOperatorBody() {
        RecordingFacade facade = new RecordingFacade();
        WebTestClient client = webClient(facade);

        client.get()
                .uri("/warehouse/quality/checks")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].tenantId").isEqualTo(0);

        client.post()
                .uri("/warehouse/quality/aggregate-lag")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.createdBy").isEqualTo("operator");

        assertThat(facade.lastTenantId).isEqualTo(0L);
        assertThat(facade.lastLimit).isEqualTo(20);
        assertThat(facade.lastMaxLagMinutes).isEqualTo(30L);
        assertThat(facade.lastOperator).isEqualTo("operator");
    }

    @Test
    void mapsInvalidQualityWindowToApi001() {
        WebTestClient client = webClient(new CdpWarehouseQualityApplicationService());

        client.post()
                .uri("/warehouse/quality/reconcile-ods")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "from", "2026-06-05T11:00:00",
                        "to", "2026-06-05T10:00:00"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("from must be before to")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CdpWarehouseQualityFacade facade) {
        return WebTestClient.bindToController(new CdpWarehouseQualityController(facade)).build();
    }

    private static final class RecordingFacade implements CdpWarehouseQualityFacade {
        private Long lastTenantId;
        private int lastLimit;
        private Long lastMaxLagMinutes;
        private String lastOperator;

        @Override
        public List<Map<String, Object>> recentChecks(Long tenantId, int limit) {
            lastTenantId = tenantId;
            lastLimit = limit;
            return List.of(Map.of("tenantId", tenantId));
        }

        @Override
        public Map<String, Object> reconcileOds(Long tenantId, LocalDateTime from, LocalDateTime to, Long tolerance,
                                                String operator) {
            return Map.of("tenantId", tenantId, "createdBy", operator);
        }

        @Override
        public Map<String, Object> checkAggregateLag(Long tenantId, LocalDateTime now, Long maxLagMinutes,
                                                     String operator) {
            lastTenantId = tenantId;
            lastMaxLagMinutes = maxLagMinutes;
            lastOperator = operator;
            return Map.of("tenantId", tenantId, "createdBy", operator);
        }
    }
}
