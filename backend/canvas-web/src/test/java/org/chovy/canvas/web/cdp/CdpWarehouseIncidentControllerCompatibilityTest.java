package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseIncidentFacade;
import org.chovy.canvas.cdp.application.CdpWarehouseIncidentApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpWarehouseIncidentControllerCompatibilityTest {

    @Test
    void exposesLegacyListAckAndResolveRoutesWithCompatibilityEnvelope() {
        WebTestClient client = webClient(new CdpWarehouseIncidentApplicationService());

        client.get()
                .uri("/warehouse/incidents?status=open&limit=1")
                .header("X-Tenant-Id", "9")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data[0].id").isEqualTo(9002)
                .jsonPath("$.data[0].tenantId").isEqualTo(9)
                .jsonPath("$.data[0].status").isEqualTo("OPEN")
                .jsonPath("$.data[0].incidentKey").isEqualTo("AVAILABILITY:HYBRID:OFFLINE_AGGREGATE")
                .jsonPath("$.data[0].occurrenceCount").isEqualTo(1)
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        client.post()
                .uri("/warehouse/incidents/9002/ack")
                .header("X-Tenant-Id", "9")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("operator", "alice"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data").isEqualTo(true);

        client.post()
                .uri("/warehouse/incidents/9002/resolve")
                .header("X-Tenant-Id", "9")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("operator", "bob"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data").isEqualTo(true);
    }

    @Test
    void defaultsTenantAndNullOperatorBodyWithoutChangingFalseMissContract() {
        RecordingFacade facade = new RecordingFacade();
        WebTestClient client = webClient(facade);

        client.get()
                .uri("/warehouse/incidents")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].tenantId").isEqualTo(0);

        client.post()
                .uri("/warehouse/incidents/42/ack")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data").isEqualTo(false);

        assertThat(facade.lastTenantId).isEqualTo(0L);
        assertThat(facade.lastStatus).isNull();
        assertThat(facade.lastLimit).isEqualTo(20);
        assertThat(facade.lastOperator).isEqualTo("operator");
    }

    private static WebTestClient webClient(CdpWarehouseIncidentFacade facade) {
        return WebTestClient.bindToController(new CdpWarehouseIncidentController(facade)).build();
    }

    private static final class RecordingFacade implements CdpWarehouseIncidentFacade {
        private Long lastTenantId;
        private String lastStatus;
        private int lastLimit;
        private String lastOperator;

        @Override
        public List<Map<String, Object>> listIncidents(Long tenantId, String status, int limit) {
            lastTenantId = tenantId;
            lastStatus = status;
            lastLimit = limit;
            return List.of(Map.of("tenantId", tenantId));
        }

        @Override
        public boolean acknowledge(Long tenantId, Long incidentId, String operator) {
            lastTenantId = tenantId;
            lastOperator = operator;
            return false;
        }

        @Override
        public boolean resolve(Long tenantId, Long incidentId, String operator) {
            lastTenantId = tenantId;
            lastOperator = operator;
            return false;
        }
    }
}
