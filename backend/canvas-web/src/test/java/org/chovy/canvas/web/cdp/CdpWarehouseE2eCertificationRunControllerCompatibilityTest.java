package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseE2eCertificationFacade;
import org.chovy.canvas.cdp.application.CdpWarehouseE2eCertificationApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpWarehouseE2eCertificationRunControllerCompatibilityTest {

    @Test
    void exposesLegacyRunAliasWithSameRunRecentAndDetailBehavior() {
        WebTestClient client = webClient(new CdpWarehouseE2eCertificationApplicationService());

        client.post()
                .uri("/warehouse/e2e-certification-runs?mode=batch&contractKey=orders&contractKey=audience"
                        + "&requirePhysical=false&requireRealtime=true&requireDataPathProof=false"
                        + "&requestedBy=qa")
                .header("X-Tenant-Id", "12")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.tenantId").isEqualTo(12)
                .jsonPath("$.data.mode").isEqualTo("BATCH")
                .jsonPath("$.data.requirePhysical").isEqualTo(false)
                .jsonPath("$.data.requireRealtime").isEqualTo(true)
                .jsonPath("$.data.requireDataPathProof").isEqualTo(false)
                .jsonPath("$.data.requestedBy").isEqualTo("qa")
                .jsonPath("$.data.contractKeysJson").isEqualTo("[\"orders\",\"audience\"]")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        client.get()
                .uri("/warehouse/e2e-certification-runs?limit=1")
                .header("X-Tenant-Id", "12")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].tenantId").isEqualTo(12)
                .jsonPath("$.data[0].requestedBy").isEqualTo("qa");

        client.get()
                .uri("/warehouse/e2e-certification-runs/101")
                .header("X-Tenant-Id", "12")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.id").isEqualTo(101)
                .jsonPath("$.data.status").isEqualTo("PASS");
    }

    @Test
    void forwardsLegacyDefaultsAndMapsNotFoundToApi001() {
        RecordingFacade facade = new RecordingFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/warehouse/e2e-certification-runs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(0)
                .jsonPath("$.data.mode").isEqualTo("HYBRID")
                .jsonPath("$.data.requirePhysical").isEqualTo(true)
                .jsonPath("$.data.requireRealtime").isEqualTo(true)
                .jsonPath("$.data.requireDataPathProof").isEqualTo(true)
                .jsonPath("$.data.requestedBy").isEqualTo("system");

        assertThat(facade.lastTenantId).isEqualTo(0L);
        assertThat(facade.lastContractKeys).isEmpty();
        assertThat(facade.lastRequestedBy).isEqualTo("system");

        client.get()
                .uri("/warehouse/e2e-certification-runs/999")
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
        return WebTestClient.bindToController(new CdpWarehouseE2eCertificationRunController(facade)).build();
    }

    private static final class RecordingFacade extends CdpWarehouseE2eCertificationApplicationService {
        private Long lastTenantId;
        private List<String> lastContractKeys;
        private String lastRequestedBy;

        @Override
        public Map<String, Object> run(Long tenantId, String from, String to, String mode,
                List<String> contractKeys, boolean requirePhysical, boolean requireRealtime,
                boolean requireDataPathProof, String requestedBy) {
            lastTenantId = tenantId;
            lastContractKeys = contractKeys;
            lastRequestedBy = requestedBy;
            return super.run(tenantId, from, to, mode, contractKeys, requirePhysical, requireRealtime,
                    requireDataPathProof, requestedBy);
        }
    }
}
