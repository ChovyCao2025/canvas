package org.chovy.canvas.web.approvals;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.platform.api.ApprovalFacade;
import org.chovy.canvas.platform.application.ApprovalApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class ApprovalControllerCompatibilityTest {

    @Test
    void exposesAllLegacyApprovalRoutesWithCompatibilityEnvelope() {
        WebTestClient client = webClient(new ApprovalApplicationService());

        List<RouteProbe> probes = List.of(
                get("/tasks?status=PENDING"),
                get("/instances?targetType=CANVAS&targetId=canvas-101&status=PENDING"),
                post("/tasks/7001/approve", Map.of("comment", "approved")),
                post("/tasks/7001/reject", Map.of("comment", "rejected"), "8"),
                postAdmin("/external/lark/sync?limit=1"),
                postAdmin("/external/lark/instances/9001/sync"));

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
    void mapsHeadersDefaultsPathVariablesQueryAndPayloadToFacade() {
        RecordingApprovalFacade facade = new RecordingApprovalFacade();

        webClient(facade)
                .post()
                .uri("/approvals/tasks/7001/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("comment", "looks good"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(7)
                .jsonPath("$.data.taskId").isEqualTo(7001)
                .jsonPath("$.data.status").isEqualTo("APPROVED")
                .jsonPath("$.data.operator").isEqualTo("operator-1");

        assertThat(facade.lastTenantId).isEqualTo(7L);
        assertThat(facade.lastActor).isEqualTo("operator-1");
        assertThat(facade.lastRole).isEqualTo("OPERATOR");
        assertThat(facade.lastPayload).containsEntry("comment", "looks good");

        webClient(facade)
                .get()
                .uri("/approvals/tasks?status=APPROVED")
                .header("X-Tenant-Id", "42")
                .header("X-Actor", "reviewer-1")
                .header("X-Role", "TENANT_ADMIN")
                .exchange()
                .expectStatus().isOk();

        assertThat(facade.lastTenantId).isEqualTo(42L);
        assertThat(facade.lastActor).isEqualTo("reviewer-1");
        assertThat(facade.lastRole).isEqualTo("TENANT_ADMIN");
        assertThat(facade.lastStatus).isEqualTo("APPROVED");
    }

    @Test
    void badRequestAndForbiddenUseCompatibilityErrorEnvelopes() {
        WebTestClient client = webClient(new ApprovalApplicationService());

        client.post()
                .uri("/approvals/tasks/9999/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("comment", "missing"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("approval task not found: 9999")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        client.post()
                .uri("/approvals/external/lark/sync")
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(403)
                .jsonPath("$.errorCode").isEqualTo("AUTH_003")
                .jsonPath("$.message").isEqualTo("Lark approval sync requires admin role")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(ApprovalFacade facade) {
        return WebTestClient.bindToController(new ApprovalController(facade)).build();
    }

    private static RouteProbe get(String path) {
        return new RouteProbe("GET", path, Map.of(), null);
    }

    private static RouteProbe post(String path, Map<String, Object> body) {
        return new RouteProbe("POST", path, body, null);
    }

    private static RouteProbe post(String path, Map<String, Object> body, String tenantId) {
        return new RouteProbe("POST", path, body, tenantId);
    }

    private static RouteProbe postAdmin(String path) {
        return new RouteProbe("POST_ADMIN", path, Map.of(), null);
    }

    private record RouteProbe(String method, String path, Map<String, Object> body, String tenantId) {
        WebTestClient.ResponseSpec exchange(WebTestClient client) {
            if ("GET".equals(method)) {
                return client.get().uri("/approvals" + path).exchange();
            }
            WebTestClient.RequestBodySpec request = client.post().uri("/approvals" + path);
            if (tenantId != null) {
                request.header("X-Tenant-Id", tenantId);
            }
            if ("POST_ADMIN".equals(method)) {
                request.header("X-Role", "TENANT_ADMIN");
            }
            if (body.isEmpty()) {
                return request.exchange();
            }
            return request.contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange();
        }
    }

    private static final class RecordingApprovalFacade implements ApprovalFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private String lastActor;
        private String lastRole;
        private String lastStatus;
        private Map<String, Object> lastPayload = Map.of();

        @Override
        public List<Map<String, Object>> tasks(Long tenantId, String actor, String role, String status) {
            operations.add("tasks");
            capture(tenantId, actor, role, Map.of());
            lastStatus = status;
            return List.of(row("tenantId", tenantId, "status", status, "actor", actor, "role", role));
        }

        @Override
        public List<Map<String, Object>> instances(Long tenantId, String targetType, String targetId, String status) {
            operations.add("instances");
            lastTenantId = tenantId;
            return List.of(row("tenantId", tenantId, "targetType", targetType, "targetId", targetId,
                    "status", status));
        }

        @Override
        public Map<String, Object> approve(Long tenantId, Long taskId, Map<String, Object> payload, String actor,
                                           String role) {
            operations.add("approve");
            capture(tenantId, actor, role, payload);
            return row("tenantId", tenantId, "taskId", taskId, "status", "APPROVED", "operator", actor);
        }

        @Override
        public Map<String, Object> reject(Long tenantId, Long taskId, Map<String, Object> payload, String actor,
                                          String role) {
            operations.add("reject");
            capture(tenantId, actor, role, payload);
            return row("tenantId", tenantId, "taskId", taskId, "status", "REJECTED", "operator", actor);
        }

        @Override
        public Map<String, Object> syncLarkApprovals(Long tenantId, Integer limit, String actor, String role) {
            operations.add("syncLarkApprovals");
            capture(tenantId, actor, role, Map.of());
            return row("tenantId", tenantId, "synced", limit, "operator", actor);
        }

        @Override
        public Map<String, Object> syncLarkApprovalInstance(Long tenantId, Long instanceId, String actor,
                                                            String role) {
            operations.add("syncLarkApprovalInstance");
            capture(tenantId, actor, role, Map.of());
            return row("tenantId", tenantId, "id", instanceId, "externalProvider", "LARK");
        }

        private void capture(Long tenantId, String actor, String role, Map<String, Object> payload) {
            lastTenantId = tenantId;
            lastActor = actor;
            lastRole = role;
            lastPayload = new LinkedHashMap<>(payload);
        }

        private static Map<String, Object> row(Object... pairs) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 0; i < pairs.length; i += 2) {
                row.put((String) pairs[i], pairs[i + 1]);
            }
            return row;
        }
    }
}
