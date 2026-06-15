package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseRealtimeFacade;
import org.chovy.canvas.cdp.application.CdpWarehouseRealtimeApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpWarehouseRealtimeControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    @Test
    void exposesAllLegacyWarehouseRealtimeRoutesExceptExistingCutoverReadiness() {
        WebTestClient client = webClient(new CdpWarehouseRealtimeApplicationService());

        List<RouteProbe> probes = List.of(
                get("/status"),
                post("/schemas", Map.of("pipelineKey", "orders", "schemaRole", "source", "schemaVersion", "v1")),
                get("/schemas?pipelineKey=orders&schemaRole=source&limit=10"),
                get("/schemas/latest?pipelineKey=orders&schemaRole=source"),
                get("/pipelines/contracts?lifecycleStatus=active"),
                post("/pipelines/contracts", Map.of("pipelineKey", "orders", "displayName", "Orders")),
                post("/pipelines/checkpoints", Map.of("pipelineKey", "orders", "checkpointId", "cp-1")),
                get("/pipelines/status?recentLimit=5"),
                post("/jobs/incidents/scan?pipelineKey=orders&maxHeartbeatAgeSeconds=300&limit=10", Map.of()),
                post("/jobs/heartbeats", Map.of("pipelineKey", "orders", "jobKey", "job-1")),
                get("/jobs/status?pipelineKey=orders&maxHeartbeatAgeSeconds=300&limit=10"),
                post("/jobs/actions", Map.of("pipelineKey", "orders", "jobKey", "job-1", "action", "restart")),
                get("/jobs/actions/pending?pipelineKey=orders&jobKey=job-1&limit=10"),
                post("/jobs/actions/1/ack", Map.of()),
                post("/jobs/actions/1/complete", Map.of("status", "done", "resultMessage", "ok")),
                post("/pipelines/incidents/scan?recentLimit=5", Map.of()),
                post("/job-probes/targets", Map.of("pipelineKey", "orders", "jobKey", "job-1")),
                get("/job-probes/targets?includeDisabled=true&limit=10"),
                post("/job-probes/targets/1/enabled?enabled=false", Map.of()),
                post("/job-probes/scan?targetId=1&limit=10", Map.of()));

        for (RouteProbe probe : probes) {
            probe.exchange(client)
                    .expectStatus().isOk()
                    .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(0)
                    .jsonPath("$.message").isEqualTo("success")
                    .jsonPath("$.errorCode").doesNotExist()
                    .jsonPath("$.traceId").doesNotExist();
        }
    }

    @Test
    void missingHeadersUseCompatibilityDefaultsAndBadRequestsMapToApi001() {
        RecordingWarehouseRealtimeFacade facade = new RecordingWarehouseRealtimeFacade();

        webClient(facade)
                .post()
                .uri("/warehouse/realtime/pipelines/contracts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("pipelineKey", "orders"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue())
                .jsonPath("$.data.updatedBy").isEqualTo(DEFAULT_ACTOR);

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(DEFAULT_ACTOR);

        facade.failUpsertPipeline = true;

        webClient(facade)
                .post()
                .uri("/warehouse/realtime/pipelines/contracts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("displayName", "missing key"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("pipelineKey is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CdpWarehouseRealtimeFacade facade) {
        return WebTestClient.bindToController(new CdpWarehouseRealtimeController(facade)).build();
    }

    private static RouteProbe get(String path) {
        return new RouteProbe("GET", path, Map.of());
    }

    private static RouteProbe post(String path, Map<String, Object> body) {
        return new RouteProbe("POST", path, body);
    }

    private record RouteProbe(String method, String path, Map<String, Object> body) {
        WebTestClient.ResponseSpec exchange(WebTestClient client) {
            if ("GET".equals(method)) {
                return client.get().uri("/warehouse/realtime" + path).exchange();
            }
            return client.post()
                    .uri("/warehouse/realtime" + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .exchange();
        }
    }

    private static final class RecordingWarehouseRealtimeFacade extends CdpWarehouseRealtimeApplicationService {
        private Long lastTenantId;
        private String lastActor;
        private boolean failUpsertPipeline;

        @Override
        public Map<String, Object> upsertPipelineContract(Long tenantId, Map<String, Object> payload, String actor) {
            if (failUpsertPipeline) {
                throw new IllegalArgumentException("pipelineKey is required");
            }
            lastTenantId = tenantId;
            lastActor = actor;
            return Map.of("tenantId", tenantId, "updatedBy", actor);
        }
    }
}
