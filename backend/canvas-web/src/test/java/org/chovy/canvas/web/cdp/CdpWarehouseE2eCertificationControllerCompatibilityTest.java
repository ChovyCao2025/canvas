package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseE2eCertificationFacade;
import org.chovy.canvas.cdp.application.CdpWarehouseE2eCertificationApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpWarehouseE2eCertificationControllerCompatibilityTest {

    @Test
    void exposesLegacyWarehouseE2eCertificationRouteFamily() {
        WebTestClient client = webClient(new CdpWarehouseE2eCertificationApplicationService());

        client.get()
                .uri("/warehouse/e2e-certification?contractKey=orders&contractKey=audience")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.tenantId").isEqualTo(0)
                .jsonPath("$.data.mode").isEqualTo("HYBRID")
                .jsonPath("$.data.requirePhysical").isEqualTo(true)
                .jsonPath("$.data.requireRealtime").isEqualTo(true)
                .jsonPath("$.data.requireDataPathProof").isEqualTo(true)
                .jsonPath("$.data.evidence[0].key").isEqualTo("production_readiness")
                .jsonPath("$.data.productionReadiness.status").isEqualTo("PASS")
                .jsonPath("$.data.liveTableInspection.status").isEqualTo("PASS")
                .jsonPath("$.data.realtimePipelineStatus.status").isEqualTo("PASS")
                .jsonPath("$.data.realtimeJobStatus.status").isEqualTo("PASS")
                .jsonPath("$.data.dataPathProof.sourceMode").isEqualTo("MYSQL_CDC");

        client.post()
                .uri("/warehouse/e2e-certification/runs?contractKey=orders&contractKey=audience")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.id").isEqualTo(101)
                .jsonPath("$.data.tenantId").isEqualTo(0)
                .jsonPath("$.data.requestedBy").isEqualTo("system")
                .jsonPath("$.data.contractKeysJson").isEqualTo("[\"orders\",\"audience\"]")
                .jsonPath("$.data.evidenceJson").exists()
                .jsonPath("$.data.productionReadinessJson").exists()
                .jsonPath("$.data.liveTableInspectionJson").exists()
                .jsonPath("$.data.realtimePipelineStatusJson").exists()
                .jsonPath("$.data.realtimeJobStatusJson").exists()
                .jsonPath("$.data.dataPathProofJson").exists();

        client.get()
                .uri("/warehouse/e2e-certification/gate?contractKey=orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.status").isEqualTo("PASS")
                .jsonPath("$.data.matchedRunStatus").isEqualTo("PASS")
                .jsonPath("$.data.maxAgeMinutes").isEqualTo(60);

        client.get()
                .uri("/warehouse/e2e-certification/runs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].id").isEqualTo(101);

        client.get()
                .uri("/warehouse/e2e-certification/runs/101")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.status").isEqualTo("PASS");
    }

    @Test
    void forwardsTenantHeaderAndExplicitFlagsToFacade() {
        RecordingFacade facade = new RecordingFacade();

        webClient(facade).post()
                .uri("/warehouse/e2e-certification/runs?mode=batch&contractKey=orders&contractKey=orders"
                        + "&requirePhysical=false&requireRealtime=false&requireDataPathProof=false"
                        + "&requestedBy=alice")
                .header("X-Tenant-Id", "12")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(12)
                .jsonPath("$.data.mode").isEqualTo("BATCH")
                .jsonPath("$.data.requestedBy").isEqualTo("alice")
                .jsonPath("$.data.requirePhysical").isEqualTo(false)
                .jsonPath("$.data.requireRealtime").isEqualTo(false)
                .jsonPath("$.data.requireDataPathProof").isEqualTo(false)
                .jsonPath("$.data.contractKeysJson").isEqualTo("[\"orders\"]");

        assertThat(facade.lastTenantId).isEqualTo(12L);
        assertThat(facade.lastContractKeys).containsExactly("orders", "orders");
        assertThat(facade.lastRequirePhysical).isFalse();
        assertThat(facade.lastRequireRealtime).isFalse();
        assertThat(facade.lastRequireDataPathProof).isFalse();
        assertThat(facade.lastRequestedBy).isEqualTo("alice");
    }

    @Test
    void recentLimitDefaultsToTwentyAndNotFoundUsesApi001Envelope() {
        RecordingFacade facade = new RecordingFacade();
        WebTestClient client = webClient(facade);

        client.get()
                .uri("/warehouse/e2e-certification/runs")
                .header("X-Tenant-Id", "44")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].limit").isEqualTo(20);

        assertThat(facade.lastTenantId).isEqualTo(44L);
        assertThat(facade.lastLimit).isEqualTo(20);

        client.get()
                .uri("/warehouse/e2e-certification/runs/999")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("certification run not found: 999")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CdpWarehouseE2eCertificationFacade facade) {
        return WebTestClient.bindToController(new CdpWarehouseE2eCertificationController(facade)).build();
    }

    private static final class RecordingFacade extends CdpWarehouseE2eCertificationApplicationService {
        private Long lastTenantId;
        private List<String> lastContractKeys;
        private boolean lastRequirePhysical;
        private boolean lastRequireRealtime;
        private boolean lastRequireDataPathProof;
        private String lastRequestedBy;
        private Integer lastLimit;

        @Override
        public Map<String, Object> run(Long tenantId, String from, String to, String mode,
                List<String> contractKeys, boolean requirePhysical, boolean requireRealtime,
                boolean requireDataPathProof, String requestedBy) {
            lastTenantId = tenantId;
            lastContractKeys = contractKeys;
            lastRequirePhysical = requirePhysical;
            lastRequireRealtime = requireRealtime;
            lastRequireDataPathProof = requireDataPathProof;
            lastRequestedBy = requestedBy;
            return super.run(tenantId, from, to, mode, contractKeys, requirePhysical, requireRealtime,
                    requireDataPathProof, requestedBy);
        }

        @Override
        public List<Map<String, Object>> recent(Long tenantId, Integer limit) {
            lastTenantId = tenantId;
            lastLimit = limit;
            return List.of(Map.of("tenantId", tenantId, "limit", limit));
        }
    }
}
