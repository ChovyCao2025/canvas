package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseFieldGovernanceFacade;
import org.chovy.canvas.cdp.application.CdpWarehouseFieldGovernanceApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpWarehouseFieldGovernanceControllerCompatibilityTest {

    @Test
    void exposesLegacyPoliciesAndEvaluationRoutesWithCompatibilityEnvelope() {
        WebTestClient client = webClient(new CdpWarehouseFieldGovernanceApplicationService());

        client.post()
                .uri("/warehouse/fields/policies")
                .header("X-Tenant-Id", "9")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "datasetKey", "canvas_daily_stats",
                        "fieldKey", "canvas_id",
                        "physicalName", "canvas_dws.canvas_daily_stats",
                        "columnName", "canvas_id",
                        "valueType", "number",
                        "accessPolicy", "mask",
                        "minRole", "tenant_admin",
                        "allowedUsages", "select,filter"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.tenantId").isEqualTo(9)
                .jsonPath("$.data.fieldKey").isEqualTo("canvas_id")
                .jsonPath("$.data.accessPolicy").isEqualTo("MASK")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        client.get()
                .uri("/warehouse/fields/policies?datasetKey=canvas_daily_stats&lifecycleStatus=active")
                .header("X-Tenant-Id", "9")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].fieldKey").isEqualTo("canvas_id");

        client.post()
                .uri("/warehouse/fields/evaluate-bi-query")
                .header("X-Tenant-Id", "9")
                .header("X-Actor", "alice")
                .header("X-Role", "OPERATOR")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "datasetKey", "canvas_daily_stats",
                        "dimensions", List.of("canvas_id")))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.allowed").isEqualTo(false)
                .jsonPath("$.data.decisions[0].decision").isEqualTo("DENY");
    }

    @Test
    void defaultsHeadersAndMapsBadRequestsToApi001() {
        RecordingFacade facade = new RecordingFacade();
        WebTestClient client = webClient(facade);

        client.get()
                .uri("/warehouse/fields/policies")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].tenantId").isEqualTo(0);

        assertThat(facade.lastTenantId).isEqualTo(0L);

        client.post()
                .uri("/warehouse/fields/evaluate-bi-query")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("datasetKey", "missing_dataset"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("dataset is not supported: missing_dataset")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CdpWarehouseFieldGovernanceFacade facade) {
        return WebTestClient.bindToController(new CdpWarehouseFieldGovernanceController(facade)).build();
    }

    private static final class RecordingFacade extends CdpWarehouseFieldGovernanceApplicationService {
        private Long lastTenantId;

        @Override
        public List<Map<String, Object>> listPolicies(Long tenantId, String datasetKey, String lifecycleStatus) {
            lastTenantId = tenantId;
            return List.of(Map.of("tenantId", tenantId));
        }
    }
}
