package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseDataPathProbeFacade;
import org.chovy.canvas.cdp.api.CdpWarehouseDataPathProbeFacade.RunCommand;
import org.chovy.canvas.cdp.application.CdpWarehouseDataPathProbeApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpWarehouseDataPathProbeControllerCompatibilityTest {

    @Test
    void exposesLegacySyntheticOdsProbeRoutesWithCompatibilityEnvelope() {
        WebTestClient client = webClient(new CdpWarehouseDataPathProbeApplicationService());

        client.post()
                .uri("/warehouse/data-path-probes/synthetic-ods/run?probeKey=ods-cert"
                        + "&eventCode=__warehouse_probe_custom&strict=false"
                        + "&verifyAttempts=1&verifyDelayMs=0&sourceMode=MYSQL_CDC")
                .header("X-Tenant-Id", "9")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.tenantId").isEqualTo(9)
                .jsonPath("$.data.probeKey").isEqualTo("ods-cert")
                .jsonPath("$.data.eventCode").isEqualTo("__warehouse_probe_custom")
                .jsonPath("$.data.strict").isEqualTo(false)
                .jsonPath("$.data.sourceMode").isEqualTo("MYSQL_CDC")
                .jsonPath("$.data.sourceStatus").isEqualTo("PASS")
                .jsonPath("$.data.sinkStatus").isEqualTo("SKIPPED")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        client.get()
                .uri("/warehouse/data-path-probes/synthetic-ods/runs?limit=5")
                .header("X-Tenant-Id", "9")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].tenantId").isEqualTo(9)
                .jsonPath("$.data[0].probeKey").isEqualTo("ods-cert");
    }

    @Test
    void defaultsTenantAndRunQueryParameters() {
        RecordingFacade facade = new RecordingFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/warehouse/data-path-probes/synthetic-ods/run")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(0)
                .jsonPath("$.data.sourceMode").isEqualTo("DIRECT_SINK");

        client.get()
                .uri("/warehouse/data-path-probes/synthetic-ods/runs")
                .exchange()
                .expectStatus().isOk();

        assertThat(facade.lastTenantId).isEqualTo(0L);
        assertThat(facade.lastLimit).isEqualTo(20);
        assertThat(facade.lastCommand).isEqualTo(new RunCommand(null, null, true, 3, 100, "DIRECT_SINK"));
    }

    @Test
    void mapsInvalidProbeParametersToApi001() {
        WebTestClient client = webClient(new CdpWarehouseDataPathProbeApplicationService());

        client.post()
                .uri("/warehouse/data-path-probes/synthetic-ods/run?eventCode=customer_signup")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("eventCode must use reserved __warehouse_probe prefix")
                .jsonPath("$.data").doesNotExist();
    }

    private static WebTestClient webClient(CdpWarehouseDataPathProbeFacade facade) {
        return WebTestClient.bindToController(new CdpWarehouseDataPathProbeController(facade)).build();
    }

    private static final class RecordingFacade implements CdpWarehouseDataPathProbeFacade {
        private Long lastTenantId;
        private int lastLimit;
        private RunCommand lastCommand;

        @Override
        public Map<String, Object> run(Long tenantId, RunCommand command) {
            lastTenantId = tenantId;
            lastCommand = command;
            return Map.of(
                    "tenantId", tenantId,
                    "sourceMode", command.sourceMode());
        }

        @Override
        public List<Map<String, Object>> recent(Long tenantId, int limit) {
            lastTenantId = tenantId;
            lastLimit = limit;
            return List.of(Map.of("tenantId", tenantId));
        }
    }
}
