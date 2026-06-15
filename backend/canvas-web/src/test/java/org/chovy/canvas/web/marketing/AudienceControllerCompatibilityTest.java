package org.chovy.canvas.web.marketing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.AudienceFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class AudienceControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final Long HEADER_TENANT_ID = 42L;
    private static final String DEFAULT_ACTOR = "operator-1";
    private static final String HEADER_ACTOR = "audience-operator";

    @Test
    void exposesAllLegacyAudienceRouteShapesThroughFinalController() {
        RecordingAudienceFacade facade = new RecordingAudienceFacade();
        WebTestClient client = webClient(facade);

        List<RouteProbe> probes = List.of(
                get("", "list"),
                get("/source-fields?dataSourceType=cdp_profile", "sourceFields"),
                post("/preview", "preview", Map.of("dataSourceType", "cdp_profile")),
                get("/1", "get"),
                get("/ready", "ready"),
                post("", "create", Map.of("name", "VIP")),
                put("/1", "update", Map.of("name", "VIP updated")),
                delete("/1", "delete"),
                post("/1/compute", "compute", Map.of()),
                get("/1/stat", "stat"));

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
    void headersDefaultsAndPayloadsAreMappedToFacade() {
        RecordingAudienceFacade facade = new RecordingAudienceFacade();

        webClient(facade)
                .post()
                .uri("/canvas/audiences")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "name": "High value buyers",
                          "dataSourceType": "cdp_profile"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.updatedBy").isEqualTo(HEADER_ACTOR)
                .jsonPath("$.data.payload.name").isEqualTo("High value buyers");

        assertThat(facade.lastTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(HEADER_ACTOR);
        assertThat(facade.lastPayload).containsEntry("name", "High value buyers");

        webClient(facade)
                .post()
                .uri("/canvas/audiences/10/compute")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue())
                .jsonPath("$.data.updatedBy").isEqualTo(DEFAULT_ACTOR)
                .jsonPath("$.data.payload").isMap();

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(DEFAULT_ACTOR);
        assertThat(facade.lastPayload).isEmpty();
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingAudienceFacade facade = new RecordingAudienceFacade();
        facade.failPreview = true;

        webClient(facade)
                .post()
                .uri("/canvas/audiences/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"dataSourceType": "missing"}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("Unsupported CDP audience source: missing")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(AudienceFacade facade) {
        return WebTestClient.bindToController(new AudienceController(facade)).build();
    }

    private static RouteProbe post(String path, String operation, Map<String, Object> payload) {
        return new RouteProbe("POST", path, operation, payload);
    }

    private static RouteProbe put(String path, String operation, Map<String, Object> payload) {
        return new RouteProbe("PUT", path, operation, payload);
    }

    private static RouteProbe get(String path, String operation) {
        return new RouteProbe("GET", path, operation, Map.of());
    }

    private static RouteProbe delete(String path, String operation) {
        return new RouteProbe("DELETE", path, operation, Map.of());
    }

    private record RouteProbe(String method, String path, String operation, Map<String, Object> payload) {
        WebTestClient.ResponseSpec exchange(WebTestClient client) {
            String uri = "/canvas/audiences" + path;
            return switch (method) {
                case "GET" -> client.get().uri(uri).exchange();
                case "PUT" -> client.put().uri(uri).contentType(MediaType.APPLICATION_JSON).bodyValue(payload).exchange();
                case "DELETE" -> client.delete().uri(uri).exchange();
                default -> client.post().uri(uri).contentType(MediaType.APPLICATION_JSON).bodyValue(payload).exchange();
            };
        }
    }

    private static final class RecordingAudienceFacade implements AudienceFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private String lastActor;
        private Map<String, Object> lastPayload = Map.of();
        private boolean failPreview;

        @Override
        public Map<String, Object> list(Long tenantId, Integer page, Integer size) {
            operations.add("list");
            lastTenantId = tenantId;
            return Map.of("total", 1L, "records", List.of(view(tenantId, "list", DEFAULT_ACTOR, Map.of())));
        }

        @Override
        public List<Map<String, Object>> sourceFields(String dataSourceType) {
            operations.add("sourceFields");
            return List.of(Map.of("name", "segment", "dataSourceType", dataSourceType));
        }

        @Override
        public Map<String, Object> preview(Long tenantId, Map<String, Object> payload) {
            operations.add("preview");
            lastTenantId = tenantId;
            lastPayload = new LinkedHashMap<>(payload);
            if (failPreview) {
                throw new IllegalArgumentException("Unsupported CDP audience source: missing");
            }
            return view(tenantId, "preview", DEFAULT_ACTOR, payload);
        }

        @Override
        public Map<String, Object> get(Long tenantId, Long id) {
            operations.add("get");
            lastTenantId = tenantId;
            return view(tenantId, "get", DEFAULT_ACTOR, Map.of("id", id));
        }

        @Override
        public List<Map<String, Object>> ready(Long tenantId) {
            operations.add("ready");
            lastTenantId = tenantId;
            return List.of(view(tenantId, "ready", DEFAULT_ACTOR, Map.of()));
        }

        @Override
        public Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor) {
            operations.add("create");
            return recordMutation(tenantId, actor, payload);
        }

        @Override
        public Map<String, Object> update(Long tenantId, Long id, Map<String, Object> payload, String actor) {
            operations.add("update");
            Map<String, Object> copy = new LinkedHashMap<>(payload);
            copy.put("id", id);
            return recordMutation(tenantId, actor, copy);
        }

        @Override
        public Map<String, Object> delete(Long tenantId, Long id) {
            operations.add("delete");
            lastTenantId = tenantId;
            return view(tenantId, "delete", DEFAULT_ACTOR, Map.of("id", id));
        }

        @Override
        public Map<String, Object> compute(Long tenantId, Long id, Map<String, Object> payload, String actor) {
            operations.add("compute");
            lastTenantId = tenantId;
            lastActor = actor;
            lastPayload = new LinkedHashMap<>(payload);
            return view(tenantId, "compute", actor, Map.of("id", id, "payload", lastPayload));
        }

        @Override
        public Map<String, Object> stat(Long tenantId, Long id) {
            operations.add("stat");
            lastTenantId = tenantId;
            return view(tenantId, "stat", DEFAULT_ACTOR, Map.of("id", id));
        }

        private Map<String, Object> recordMutation(Long tenantId, String actor, Map<String, Object> payload) {
            lastTenantId = tenantId;
            lastActor = actor;
            lastPayload = new LinkedHashMap<>(payload);
            return view(tenantId, operations.get(operations.size() - 1), actor, payload);
        }

        private static Map<String, Object> view(Long tenantId, String operation, String actor,
                                                Map<String, Object> payload) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tenantId", tenantId);
            row.put("operation", operation);
            row.put("updatedBy", actor);
            row.put("payload", new LinkedHashMap<>(payload));
            return row;
        }
    }
}
