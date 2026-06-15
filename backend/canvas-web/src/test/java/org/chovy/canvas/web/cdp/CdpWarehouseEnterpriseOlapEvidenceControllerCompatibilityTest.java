package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseEnterpriseOlapEvidenceFacade;
import org.chovy.canvas.cdp.application.CdpWarehouseEnterpriseOlapEvidenceApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpWarehouseEnterpriseOlapEvidenceControllerCompatibilityTest {

    @Test
    void exposesLegacyEvidenceRouteFamilyWithDefaultTenantAndStatefulResults() {
        WebTestClient client = webClient(new CdpWarehouseEnterpriseOlapEvidenceApplicationService());

        client.post()
                .uri("/warehouse/enterprise-olap/evidence")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "evidenceKey": "backup_restore",
                          "status": "PASS",
                          "reason": "operator supplied proof",
                          "measuredAt": "2026-06-15T02:40:00",
                          "expiresAt": "2026-06-15T03:40:00",
                          "evidenceJson": "{\\"ok\\":true}"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.tenantId").isEqualTo(0)
                .jsonPath("$.data.evidenceKey").isEqualTo("backup_restore")
                .jsonPath("$.data.source").isEqualTo("operator")
                .jsonPath("$.data.createdBy").isEqualTo("system");

        client.get()
                .uri("/warehouse/enterprise-olap/evidence/latest")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(0)
                .jsonPath("$.data.status").isEqualTo("FAIL")
                .jsonPath("$.data.evidence[3].evidenceKey").isEqualTo("backup_restore");

        client.get()
                .uri("/warehouse/enterprise-olap/evidence/proof")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[3].key").isEqualTo("enterprise_olap:backup_restore")
                .jsonPath("$.data[3].status").isEqualTo("PASS");

        client.post()
                .uri("/warehouse/enterprise-olap/evidence/collect")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.triggerType").isEqualTo("MANUAL")
                .jsonPath("$.data.status").isEqualTo("PASS");

        client.get()
                .uri("/warehouse/enterprise-olap/evidence/collections?limit=1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].triggerType").isEqualTo("MANUAL");
    }

    @Test
    void forwardsTenantHeaderAndDefaultCollectionLimitToFacade() {
        RecordingFacade facade = new RecordingFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/warehouse/enterprise-olap/evidence")
                .header("X-Tenant-Id", "77")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"evidenceKey":"backup_restore","status":"WARN","reason":"manual review"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(77)
                .jsonPath("$.data.status").isEqualTo("WARN");

        assertThat(facade.lastTenantId).isEqualTo(77L);

        client.get()
                .uri("/warehouse/enterprise-olap/evidence/collections")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].limit").isEqualTo(20);

        assertThat(facade.lastLimit).isEqualTo(20);
    }

    @Test
    void validationErrorsUseApi001BadRequestEnvelope() {
        webClient(new CdpWarehouseEnterpriseOlapEvidenceApplicationService()).post()
                .uri("/warehouse/enterprise-olap/evidence")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("evidenceKey is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CdpWarehouseEnterpriseOlapEvidenceFacade facade) {
        return WebTestClient.bindToController(new CdpWarehouseEnterpriseOlapEvidenceController(facade)).build();
    }

    private static final class RecordingFacade extends CdpWarehouseEnterpriseOlapEvidenceApplicationService {
        private Long lastTenantId;
        private Integer lastLimit;

        @Override
        public Map<String, Object> record(Long tenantId, EvidenceCommand command, String actor) {
            lastTenantId = tenantId;
            return super.record(tenantId, command, actor);
        }

        @Override
        public List<Map<String, Object>> collections(Long tenantId, Integer limit) {
            lastTenantId = tenantId;
            lastLimit = limit;
            return List.of(Map.of("tenantId", tenantId, "limit", limit));
        }
    }
}
