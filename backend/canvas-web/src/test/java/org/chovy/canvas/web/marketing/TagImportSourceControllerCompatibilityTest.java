package org.chovy.canvas.web.marketing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.TagImportSourceFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class TagImportSourceControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final Long HEADER_TENANT_ID = 42L;
    private static final String DEFAULT_ACTOR = "operator-1";
    private static final String HEADER_ACTOR = "tag-operator";

    @Test
    void exposesFiveLegacyRouteShapesThroughFinalController() {
        RecordingTagImportSourceFacade facade = new RecordingTagImportSourceFacade();
        WebTestClient client = webClient(facade);

        List<RouteProbe> probes = List.of(
                get("?enabled=1", "list"),
                post("", "create", sourcePayload("CRM API", 1)),
                put("/11", "update", sourcePayload("CRM API Updated", 0)),
                delete("/11", "delete"),
                post("/11/run", "run", Map.of()));

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
    void headersDefaultsAndBodyPathQueryParametersAreMappedToFacade() {
        RecordingTagImportSourceFacade facade = new RecordingTagImportSourceFacade();

        webClient(facade)
                .get()
                .uri("/canvas/tag-import-sources?enabled=0")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.list[0].tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue())
                .jsonPath("$.data.list[0].enabled").isEqualTo(0)
                .jsonPath("$.data.list[0].status").isEqualTo("disabled")
                .jsonPath("$.data.list[0].sourceType").isEqualTo("API_PULL");

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastEnabled).isZero();

        webClient(facade)
                .put()
                .uri("/canvas/tag-import-sources/9")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sourcePayload("Updated CRM API", 1))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.id").isEqualTo(9)
                .jsonPath("$.data.name").isEqualTo("Updated CRM API")
                .jsonPath("$.data.method").isEqualTo("POST")
                .jsonPath("$.data.enabled").isEqualTo(1)
                .jsonPath("$.data.status").isEqualTo("enabled")
                .jsonPath("$.data.updatedBy").isEqualTo(HEADER_ACTOR);

        assertThat(facade.lastTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(HEADER_ACTOR);
        assertThat(facade.lastId).isEqualTo(9L);
        assertThat(facade.lastPayload).containsEntry("fieldMapping",
                "{\"idType\":\"id_type\",\"idValue\":\"id_value\",\"tagCode\":\"tag_code\"}");
    }

    @Test
    void runForwardsPathAndTenantAndReturnsLegacyImportResultFields() {
        RecordingTagImportSourceFacade facade = new RecordingTagImportSourceFacade();

        webClient(facade)
                .post()
                .uri("/canvas/tag-import-sources/13/run")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.batchId").isEqualTo(88013)
                .jsonPath("$.data.status").isEqualTo("SUCCESS")
                .jsonPath("$.data.totalRows").isEqualTo(2)
                .jsonPath("$.data.successRows").isEqualTo(2)
                .jsonPath("$.data.failedRows").isEqualTo(0);

        assertThat(facade.lastTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.lastId).isEqualTo(13L);
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingTagImportSourceFacade facade = new RecordingTagImportSourceFacade();
        facade.failCreate = true;

        webClient(facade)
                .post()
                .uri("/canvas/tag-import-sources")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", ""))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("name is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(TagImportSourceFacade facade) {
        return WebTestClient.bindToController(new TagImportSourceController(facade)).build();
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

    private static Map<String, Object> sourcePayload(String name, int enabled) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("url", "https://crm.example.test/tags");
        payload.put("method", "post");
        payload.put("headersJson", "{\"Authorization\":\"Bearer token\"}");
        payload.put("bodyTemplate", "{\"page\":1}");
        payload.put("pageParam", "page");
        payload.put("pageSizeParam", "size");
        payload.put("pageSize", 200);
        payload.put("recordsPath", "$.data");
        payload.put("fieldMapping", "{\"idType\":\"id_type\",\"idValue\":\"id_value\",\"tagCode\":\"tag_code\"}");
        payload.put("enabled", enabled);
        return payload;
    }

    private record RouteProbe(String method, String path, String operation, Map<String, Object> body) {
        WebTestClient.ResponseSpec exchange(WebTestClient client) {
            String uri = "/canvas/tag-import-sources" + path;
            if ("GET".equals(method)) {
                return client.get().uri(uri).exchange();
            }
            if ("PUT".equals(method)) {
                return client.put().uri(uri).contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange();
            }
            if ("DELETE".equals(method)) {
                return client.delete().uri(uri).exchange();
            }
            if (body.isEmpty()) {
                return client.post().uri(uri).exchange();
            }
            return client.post().uri(uri).contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange();
        }
    }

    private static final class RecordingTagImportSourceFacade implements TagImportSourceFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private Long lastId;
        private Integer lastEnabled;
        private String lastActor;
        private Map<String, Object> lastPayload = Map.of();
        private boolean failCreate;

        @Override
        public Map<String, Object> listSources(Long tenantId, Integer enabled) {
            operations.add("list");
            lastTenantId = tenantId;
            lastEnabled = enabled;
            return Map.of("total", 1L, "list", List.of(source(tenantId, 11L, "CRM API",
                    enabled == null ? 1 : enabled, DEFAULT_ACTOR, Map.of())));
        }

        @Override
        public Map<String, Object> createSource(Long tenantId, Map<String, Object> payload, String actor) {
            operations.add("create");
            if (failCreate) {
                throw new IllegalArgumentException("name is required");
            }
            lastTenantId = tenantId;
            lastActor = actor;
            lastPayload = new LinkedHashMap<>(payload);
            return source(tenantId, 11L, String.valueOf(payload.get("name")),
                    (Integer) payload.get("enabled"), actor, payload);
        }

        @Override
        public Map<String, Object> updateSource(Long tenantId, Long id, Map<String, Object> payload, String actor) {
            operations.add("update");
            lastTenantId = tenantId;
            lastId = id;
            lastActor = actor;
            lastPayload = new LinkedHashMap<>(payload);
            return source(tenantId, id, String.valueOf(payload.get("name")),
                    (Integer) payload.get("enabled"), actor, payload);
        }

        @Override
        public void deleteSource(Long tenantId, Long id) {
            operations.add("delete");
            lastTenantId = tenantId;
            lastId = id;
        }

        @Override
        public Map<String, Object> runSource(Long tenantId, Long id) {
            operations.add("run");
            lastTenantId = tenantId;
            lastId = id;
            return Map.of("batchId", 88000L + id, "status", "SUCCESS", "totalRows", 2,
                    "successRows", 2, "failedRows", 0);
        }

        private static Map<String, Object> source(Long tenantId, Long id, String name, int enabled, String actor,
                                                  Map<String, Object> payload) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tenantId", tenantId);
            row.put("id", id);
            row.put("sourceType", "API_PULL");
            row.put("name", name);
            row.put("url", payload.getOrDefault("url", "https://crm.example.test/tags"));
            row.put("method", String.valueOf(payload.getOrDefault("method", "GET")).toUpperCase());
            row.put("enabled", enabled);
            row.put("status", enabled == 1 ? "enabled" : "disabled");
            row.put("updatedBy", actor);
            return row;
        }
    }
}
