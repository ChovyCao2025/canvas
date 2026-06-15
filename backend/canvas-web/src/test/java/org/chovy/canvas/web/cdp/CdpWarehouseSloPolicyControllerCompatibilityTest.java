package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseSloPolicyFacade;
import org.chovy.canvas.cdp.application.CdpWarehouseSloPolicyApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpWarehouseSloPolicyControllerCompatibilityTest {

    @Test
    void exposesLegacyListEffectiveAndUpsertRoutesWithCompatibilityEnvelope() {
        WebTestClient client = webClient(new CdpWarehouseSloPolicyApplicationService());

        client.post()
                .uri("/warehouse/slo-policies")
                .header("X-Tenant-Id", "9")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "policyKey", "warehouse_readiness_default",
                        "displayName", "Tenant readiness",
                        "offlineWarnRunGapMinutes", 30,
                        "offlineFailRunGapMinutes", 90,
                        "offlineWarnWatermarkLagMinutes", 10,
                        "offlineFailWatermarkLagMinutes", 40,
                        "audienceWarnRunGapMinutes", 60,
                        "audienceFailRunGapMinutes", 180,
                        "status", "active"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.tenantId").isEqualTo(9)
                .jsonPath("$.data.policyKey").isEqualTo("WAREHOUSE_READINESS_DEFAULT")
                .jsonPath("$.data.status").isEqualTo("ACTIVE")
                .jsonPath("$.data.offlineWarnRunGapMinutes").isEqualTo(30)
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        client.get()
                .uri("/warehouse/slo-policies/effective?policyKey=warehouse_readiness_default")
                .header("X-Tenant-Id", "9")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.displayName").isEqualTo("Tenant readiness");

        client.get()
                .uri("/warehouse/slo-policies?status=active")
                .header("X-Tenant-Id", "9")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].policyKey").isEqualTo("WAREHOUSE_READINESS_DEFAULT");
    }

    @Test
    void defaultsTenantPolicyKeyAndNullBodyWithoutInventingOperator() {
        RecordingFacade facade = new RecordingFacade();
        WebTestClient client = webClient(facade);

        client.get()
                .uri("/warehouse/slo-policies/effective")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(0)
                .jsonPath("$.data.policyKey").isEqualTo("WAREHOUSE_READINESS_DEFAULT");

        client.post()
                .uri("/warehouse/slo-policies")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.policyKey").isEqualTo("WAREHOUSE_READINESS_DEFAULT");

        assertThat(facade.lastTenantId).isEqualTo(0L);
        assertThat(facade.lastPolicyKey).isEqualTo("WAREHOUSE_READINESS_DEFAULT");
        assertThat(facade.lastPayload).doesNotContainKey("operator");
    }

    @Test
    void mapsInvalidThresholdsToApi001() {
        WebTestClient client = webClient(new CdpWarehouseSloPolicyApplicationService());

        client.post()
                .uri("/warehouse/slo-policies")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "policyKey", "bad",
                        "offlineWarnRunGapMinutes", 60,
                        "offlineFailRunGapMinutes", 30))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("offline run gap warn threshold must be <= fail threshold")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CdpWarehouseSloPolicyFacade facade) {
        return WebTestClient.bindToController(new CdpWarehouseSloPolicyController(facade)).build();
    }

    private static final class RecordingFacade implements CdpWarehouseSloPolicyFacade {
        private Long lastTenantId;
        private String lastPolicyKey;
        private Map<String, Object> lastPayload = Map.of();

        @Override
        public List<Map<String, Object>> listPolicies(Long tenantId, String status) {
            return List.of(policy(tenantId, "WAREHOUSE_READINESS_DEFAULT"));
        }

        @Override
        public Map<String, Object> effectivePolicy(Long tenantId, String policyKey) {
            lastTenantId = tenantId;
            lastPolicyKey = policyKey;
            return policy(tenantId, policyKey);
        }

        @Override
        public Map<String, Object> upsertPolicy(Long tenantId, Map<String, Object> payload) {
            lastTenantId = tenantId;
            lastPayload = payload;
            return policy(tenantId, "WAREHOUSE_READINESS_DEFAULT");
        }

        private static Map<String, Object> policy(Long tenantId, String policyKey) {
            return Map.of(
                    "tenantId", tenantId,
                    "policyKey", policyKey,
                    "status", "ACTIVE");
        }
    }
}
