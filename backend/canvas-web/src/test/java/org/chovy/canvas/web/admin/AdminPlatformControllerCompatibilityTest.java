package org.chovy.canvas.web.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.platform.api.AdminPlatformFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class AdminPlatformControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final Long HEADER_TENANT_ID = 42L;
    private static final String DEFAULT_ACTOR = "operator-1";
    private static final String HEADER_ACTOR = "admin-operator";

    @Test
    void exposesAllLegacyAdminRouteShapesThroughFinalController() {
        RecordingAdminPlatformFacade facade = new RecordingAdminPlatformFacade();
        WebTestClient client = webClient(facade);

        List<RouteProbe> probes = List.of(
                get("/admin/users", "users"),
                post("/admin/users", "createUser", Map.of("username", "auditor", "displayName", "Auditor")),
                put("/admin/users/1001", "updateUser", Map.of("displayName", "Operator")),
                put("/admin/users/1001/disable", "disableUser", Map.of()),
                get("/admin/projects", "projects"),
                post("/admin/projects", "createProject", Map.of("projectKey", "retention", "projectName", "Retention")),
                get("/admin/projects/2001", "project"),
                put("/admin/projects/2001", "updateProject", Map.of("projectName", "Retention Growth")),
                put("/admin/projects/2001/disable", "disableProject", Map.of()),
                get("/admin/projects/2001/members", "projectMembers"),
                put("/admin/projects/2001/members/1001", "setProjectMember", Map.of("role", "OWNER")),
                delete("/admin/projects/2001/members/1001", "removeProjectMember"),
                get("/admin/projects/2001/canvases?page=1&size=20", "projectCanvases"),
                get("/admin/projects/2001/stats", "projectStats"),
                get("/admin/system-options?category=canvas&enabled=1&keyword=review&tenantId=7", "systemOptions"),
                put("/admin/system-options/3001", "updateSystemOption", Map.of("optionValue", "false")),
                get("/admin/tenants", "tenants"),
                post("/admin/tenants", "createTenant", Map.of("name", "Retail", "tenantKey", "retail")),
                put("/admin/tenants/7/disable", "disableTenant", Map.of()),
                put("/admin/tenants/7/activate", "activateTenant", Map.of()),
                get("/admin/tenants/7/usage", "tenantUsage"));

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
    void missingHeadersUseCompatibilityDefaultsForTenantAndActor() {
        RecordingAdminPlatformFacade facade = new RecordingAdminPlatformFacade();

        webClient(facade)
                .post()
                .uri("/admin/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("projectKey", "retention", "projectName", "Retention"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.operation").isEqualTo("createProject")
                .jsonPath("$.data.tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue())
                .jsonPath("$.data.updatedBy").isEqualTo(DEFAULT_ACTOR);

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(DEFAULT_ACTOR);
    }

    @Test
    void explicitHeadersAndBodiesAreForwardedToFacade() {
        RecordingAdminPlatformFacade facade = new RecordingAdminPlatformFacade();

        webClient(facade)
                .put()
                .uri("/admin/users/1001")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("displayName", "Operator Lead", "role", "ADMIN"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.operation").isEqualTo("updateUser")
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.updatedBy").isEqualTo(HEADER_ACTOR)
                .jsonPath("$.data.payload.displayName").isEqualTo("Operator Lead");

        assertThat(facade.lastTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(HEADER_ACTOR);
        assertThat(facade.lastPayload).containsEntry("role", "ADMIN");
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingAdminPlatformFacade facade = new RecordingAdminPlatformFacade();
        facade.failCreateUser = true;

        webClient(facade)
                .post()
                .uri("/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("displayName", "Missing username"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("username is required")
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(AdminPlatformFacade facade) {
        return WebTestClient.bindToController(new AdminPlatformController(facade)).build();
    }

    private static RouteProbe get(String path, String operation) {
        return new RouteProbe("GET", path, operation, Map.of());
    }

    private static RouteProbe post(String path, String operation, Map<String, Object> body) {
        return new RouteProbe("POST", path, operation, body);
    }

    private static RouteProbe put(String path, String operation, Map<String, Object> body) {
        return new RouteProbe("PUT", path, operation, body);
    }

    private static RouteProbe delete(String path, String operation) {
        return new RouteProbe("DELETE", path, operation, Map.of());
    }

    private record RouteProbe(String method, String path, String operation, Map<String, Object> body) {
        WebTestClient.ResponseSpec exchange(WebTestClient client) {
            return switch (method) {
                case "GET" -> client.get().uri(path).exchange();
                case "DELETE" -> client.delete().uri(path).exchange();
                case "PUT" -> client.put().uri(path).contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange();
                case "POST" -> client.post().uri(path).contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange();
                default -> throw new IllegalStateException("unsupported method " + method);
            };
        }
    }

    private static final class RecordingAdminPlatformFacade implements AdminPlatformFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private String lastActor;
        private Map<String, Object> lastPayload;
        private boolean failCreateUser;

        @Override
        public List<Map<String, Object>> users(Long tenantId) {
            operations.add("users");
            lastTenantId = tenantId;
            return List.of(result("users", tenantId, null, Map.of()));
        }

        @Override
        public Map<String, Object> createUser(Long tenantId, Map<String, Object> payload, String actor) {
            if (failCreateUser) {
                throw new IllegalArgumentException("username is required");
            }
            return mutation("createUser", tenantId, payload, actor);
        }

        @Override
        public Map<String, Object> updateUser(Long tenantId, Long id, Map<String, Object> payload, String actor) {
            return mutation("updateUser", tenantId, payload, actor);
        }

        @Override
        public Map<String, Object> disableUser(Long tenantId, Long id, String actor) {
            return mutation("disableUser", tenantId, Map.of("id", id), actor);
        }

        @Override
        public List<Map<String, Object>> projects(Long tenantId) {
            operations.add("projects");
            lastTenantId = tenantId;
            return List.of(result("projects", tenantId, null, Map.of()));
        }

        @Override
        public Map<String, Object> createProject(Long tenantId, Map<String, Object> payload, String actor) {
            return mutation("createProject", tenantId, payload, actor);
        }

        @Override
        public Map<String, Object> project(Long tenantId, Long projectId) {
            return query("project", tenantId, Map.of("projectId", projectId));
        }

        @Override
        public Map<String, Object> updateProject(Long tenantId, Long projectId, Map<String, Object> payload,
                                                 String actor) {
            return mutation("updateProject", tenantId, payload, actor);
        }

        @Override
        public Map<String, Object> disableProject(Long tenantId, Long projectId, String actor) {
            return mutation("disableProject", tenantId, Map.of("projectId", projectId), actor);
        }

        @Override
        public List<Map<String, Object>> projectMembers(Long tenantId, Long projectId) {
            operations.add("projectMembers");
            lastTenantId = tenantId;
            return List.of(result("projectMembers", tenantId, null, Map.of("projectId", projectId)));
        }

        @Override
        public Map<String, Object> setProjectMember(Long tenantId, Long projectId, Long userId,
                                                    Map<String, Object> payload, String actor) {
            return mutation("setProjectMember", tenantId, payload, actor);
        }

        @Override
        public Map<String, Object> removeProjectMember(Long tenantId, Long projectId, Long userId) {
            return query("removeProjectMember", tenantId, Map.of("projectId", projectId, "userId", userId));
        }

        @Override
        public Map<String, Object> projectCanvases(Long tenantId, Long projectId, Integer page, Integer size) {
            return query("projectCanvases", tenantId, Map.of("projectId", projectId, "page", page, "size", size));
        }

        @Override
        public Map<String, Object> projectStats(Long tenantId, Long projectId) {
            return query("projectStats", tenantId, Map.of("projectId", projectId));
        }

        @Override
        public List<Map<String, Object>> systemOptions(Long tenantId, String category, Integer enabled,
                                                       String keyword, Long requestedTenantId) {
            operations.add("systemOptions");
            lastTenantId = tenantId;
            return List.of(result("systemOptions", tenantId, null, Map.of("category", category)));
        }

        @Override
        public Map<String, Object> updateSystemOption(Long tenantId, Long id, Map<String, Object> payload,
                                                      String actor) {
            return mutation("updateSystemOption", tenantId, payload, actor);
        }

        @Override
        public List<Map<String, Object>> tenants() {
            operations.add("tenants");
            return List.of(result("tenants", null, null, Map.of()));
        }

        @Override
        public Map<String, Object> createTenant(Map<String, Object> payload, String actor) {
            return mutation("createTenant", null, payload, actor);
        }

        @Override
        public Map<String, Object> disableTenant(Long id, String actor) {
            return mutation("disableTenant", null, Map.of("id", id), actor);
        }

        @Override
        public Map<String, Object> activateTenant(Long id, String actor) {
            return mutation("activateTenant", null, Map.of("id", id), actor);
        }

        @Override
        public Map<String, Object> tenantUsage(Long id) {
            operations.add("tenantUsage");
            return result("tenantUsage", id, null, Map.of());
        }

        private Map<String, Object> mutation(String operation, Long tenantId, Map<String, Object> payload,
                                             String actor) {
            operations.add(operation);
            lastTenantId = tenantId;
            lastActor = actor;
            lastPayload = new LinkedHashMap<>(payload);
            return result(operation, tenantId, actor, Map.of("payload", lastPayload));
        }

        private Map<String, Object> query(String operation, Long tenantId, Map<String, Object> fields) {
            operations.add(operation);
            lastTenantId = tenantId;
            return result(operation, tenantId, null, fields);
        }

        private static Map<String, Object> result(String operation, Long tenantId, String actor,
                                                  Map<String, Object> fields) {
            Map<String, Object> result = new LinkedHashMap<>(fields);
            result.put("operation", operation);
            if (tenantId != null) {
                result.put("tenantId", tenantId);
            }
            if (actor != null) {
                result.put("updatedBy", actor);
            }
            return result;
        }
    }
}
