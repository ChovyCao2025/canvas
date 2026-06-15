package org.chovy.canvas.web.ops;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.platform.api.OpsFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class OpsControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final Long HEADER_TENANT_ID = 42L;
    private static final String DEFAULT_ACTOR = "operator-1";
    private static final String HEADER_ACTOR = "ops-admin";

    @Test
    void exposesAllLegacyOpsRoutesWithCompatibilityEnvelope() {
        RecordingOpsFacade facade = new RecordingOpsFacade();
        WebTestClient client = webClient(facade);

        List<RouteProbe> probes = List.of(
                post("/cache/invalidate/101", "invalidateCache", Map.of()),
                post("/recovery/runtime-state/rebuild", "rebuildRuntimeState", Map.of()),
                get("/runtime/status", "runtimeStatus"),
                get("/audit-events", "auditEvents"),
                post("/canvas/101/pause", "PAUSE", Map.of("reason", "incident")),
                post("/canvas/101/offline", "OFFLINE", Map.of("reason", "incident")),
                post("/canvas/101/resume", "RESUME", Map.of("reason", "incident")),
                post("/canvas/101/kill", "KILL", Map.of("reason", "incident", "mode", "FORCE")),
                post("/canvas/101/rollback", "ROLLBACK", Map.of("reason", "incident")));

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

        assertThat(facade.operations).containsExactlyElementsOf(probes.stream()
                .map(RouteProbe::operation)
                .toList());
    }

    @Test
    void headersDefaultsPathVariablesPayloadsAndQueryParamsAreMappedToFacade() {
        RecordingOpsFacade facade = new RecordingOpsFacade();

        webClient(facade)
                .post()
                .uri("/ops/canvas/101/kill")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .header("X-Role", "TENANT_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("reason", "bad deploy", "mode", "FORCE"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.canvasId").isEqualTo(101)
                .jsonPath("$.data.action").isEqualTo("KILL")
                .jsonPath("$.data.mode").isEqualTo("FORCE")
                .jsonPath("$.data.operator").isEqualTo(HEADER_ACTOR);

        assertThat(facade.lastTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(HEADER_ACTOR);
        assertThat(facade.lastRole).isEqualTo("TENANT_ADMIN");
        assertThat(facade.lastPayload).containsEntry("reason", "bad deploy");

        webClient(facade)
                .get()
                .uri("/ops/audit-events?limit=2")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue())
                .jsonPath("$.data[0].limit").isEqualTo(2);

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(DEFAULT_ACTOR);
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingOpsFacade facade = new RecordingOpsFacade();
        facade.failEmergency = true;

        webClient(facade)
                .post()
                .uri("/ops/canvas/101/pause")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of())
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("reason is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(OpsFacade facade) {
        return WebTestClient.bindToController(new OpsController(facade)).build();
    }

    private static RouteProbe get(String path, String operation) {
        return new RouteProbe("GET", path, operation, Map.of());
    }

    private static RouteProbe post(String path, String operation, Map<String, Object> body) {
        return new RouteProbe("POST", path, operation, body);
    }

    private record RouteProbe(String method, String path, String operation, Map<String, Object> body) {
        WebTestClient.ResponseSpec exchange(WebTestClient client) {
            if ("GET".equals(method)) {
                return client.get().uri("/ops" + path).exchange();
            }
            WebTestClient.RequestBodySpec request = client.post().uri("/ops" + path);
            if (body.isEmpty()) {
                return request.exchange();
            }
            return request.contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange();
        }
    }

    private static final class RecordingOpsFacade implements OpsFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private Long lastCanvasId;
        private String lastActor = DEFAULT_ACTOR;
        private String lastRole;
        private Map<String, Object> lastPayload = Map.of();
        private boolean failEmergency;

        @Override
        public Map<String, Object> invalidateCache(Long tenantId, Long canvasId, String actor) {
            operations.add("invalidateCache");
            capture(tenantId, canvasId, "OPERATOR", actor, Map.of());
            return Map.of("tenantId", tenantId, "canvasId", canvasId, "invalidated", true);
        }

        @Override
        public Map<String, Object> rebuildRuntimeState(Long tenantId, String actor) {
            operations.add("rebuildRuntimeState");
            capture(tenantId, null, "OPERATOR", actor, Map.of());
            return Map.of("tenantId", tenantId, "rebuilt", true);
        }

        @Override
        public Map<String, Object> runtimeStatus(Long tenantId, String role, String actor) {
            operations.add("runtimeStatus");
            capture(tenantId, null, role, actor, Map.of());
            return Map.of("status", "UP", "tenantId", tenantId, "role", role, "username", actor);
        }

        @Override
        public List<Map<String, Object>> auditEvents(Long tenantId, Integer limit) {
            operations.add("auditEvents");
            capture(tenantId, null, "OPERATOR", DEFAULT_ACTOR, Map.of());
            return List.of(Map.of("tenantId", tenantId, "limit", limit, "action", "CACHE_INVALIDATE"));
        }

        @Override
        public Map<String, Object> emergencyAction(Long tenantId, Long canvasId, String action,
                                                   Map<String, Object> payload, String role, String actor) {
            operations.add(action);
            if (failEmergency) {
                throw new IllegalArgumentException("reason is required");
            }
            capture(tenantId, canvasId, role, actor, payload);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("tenantId", tenantId);
            result.put("canvasId", canvasId);
            result.put("action", action);
            result.put("operator", actor);
            result.put("mode", payload.getOrDefault("mode", "GRACEFUL"));
            return result;
        }

        private void capture(Long tenantId, Long canvasId, String role, String actor, Map<String, Object> payload) {
            lastTenantId = tenantId;
            lastCanvasId = canvasId;
            lastRole = role;
            lastActor = actor;
            lastPayload = new LinkedHashMap<>(payload);
        }
    }
}
