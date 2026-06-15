package org.chovy.canvas.web.marketing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.AbExperimentFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class AbExperimentControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final Long HEADER_TENANT_ID = 42L;
    private static final String DEFAULT_ACTOR = "operator-1";
    private static final String HEADER_ACTOR = "experiment-operator";

    @Test
    void exposesAllLegacyAbExperimentRoutesThroughFinalController() {
        RecordingAbExperimentFacade facade = new RecordingAbExperimentFacade();
        WebTestClient client = webClient(facade);

        List<RouteProbe> probes = List.of(
                get("?page=1&size=20&enabled=1", "list"),
                post("", "create", Map.of("experimentKey", "checkout-test")),
                put("/1", "update", Map.of("name", "Checkout test v2")),
                delete("/1", "delete"),
                get("/1/groups?includeDisabled=true", "listGroups"),
                post("/1/groups", "createGroup", Map.of("groupKey", "A")),
                put("/1/groups/10", "updateGroup", Map.of("weight", 70)),
                delete("/1/groups/10", "deleteGroup"),
                post("/1/governance/evaluate?controlVariantKey=A", "evaluateGovernance", Map.of()));

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
    void headersDefaultsPayloadsPathVariablesAndQueryParametersAreMappedToFacade() {
        RecordingAbExperimentFacade facade = new RecordingAbExperimentFacade();

        webClient(facade)
                .post()
                .uri("/canvas/ab-experiments")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "experimentKey": "checkout-test",
                          "name": "Checkout test"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.updatedBy").isEqualTo(HEADER_ACTOR)
                .jsonPath("$.data.payload.experimentKey").isEqualTo("checkout-test");

        assertThat(facade.lastTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(HEADER_ACTOR);
        assertThat(facade.lastPayload).containsEntry("experimentKey", "checkout-test");

        webClient(facade)
                .put()
                .uri("/canvas/ab-experiments/9/groups/11")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue())
                .jsonPath("$.data.updatedBy").isEqualTo(DEFAULT_ACTOR)
                .jsonPath("$.data.experimentId").isEqualTo(9)
                .jsonPath("$.data.groupId").isEqualTo(11)
                .jsonPath("$.data.payload").isMap();

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(DEFAULT_ACTOR);
        assertThat(facade.lastPayload).isEmpty();

        webClient(facade)
                .get()
                .uri("/canvas/ab-experiments?page=2&size=5&enabled=1")
                .exchange()
                .expectStatus().isOk();

        assertThat(facade.lastQuery).containsEntry("page", 2)
                .containsEntry("size", 5)
                .containsEntry("enabled", 1);

        webClient(facade)
                .post()
                .uri("/canvas/ab-experiments/9/governance/evaluate?controlVariantKey=B")
                .exchange()
                .expectStatus().isOk();

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastExperimentId).isEqualTo(9L);
        assertThat(facade.lastControlVariantKey).isEqualTo("B");
        assertThat(facade.lastActor).isEqualTo(DEFAULT_ACTOR);
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingAbExperimentFacade facade = new RecordingAbExperimentFacade();
        facade.failEvaluate = true;

        webClient(facade)
                .post()
                .uri("/canvas/ab-experiments/99/governance/evaluate")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("AB experiment not found: 99")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(AbExperimentFacade facade) {
        return WebTestClient.bindToController(new AbExperimentController(facade)).build();
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
            String uri = "/canvas/ab-experiments" + path;
            return switch (method) {
                case "GET" -> client.get().uri(uri).exchange();
                case "PUT" -> client.put().uri(uri).contentType(MediaType.APPLICATION_JSON).bodyValue(payload).exchange();
                case "DELETE" -> client.delete().uri(uri).exchange();
                default -> client.post().uri(uri).contentType(MediaType.APPLICATION_JSON).bodyValue(payload).exchange();
            };
        }
    }

    private static final class RecordingAbExperimentFacade implements AbExperimentFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private String lastActor;
        private Map<String, Object> lastPayload = Map.of();
        private Map<String, Object> lastQuery = Map.of();
        private Long lastExperimentId;
        private String lastControlVariantKey;
        private boolean failEvaluate;

        @Override
        public Map<String, Object> list(Long tenantId, Map<String, Object> query) {
            operations.add("list");
            lastTenantId = tenantId;
            lastQuery = new LinkedHashMap<>(query);
            return Map.of("total", 1L, "records", List.of(view(tenantId, "list", DEFAULT_ACTOR, lastQuery)));
        }

        @Override
        public Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor) {
            operations.add("create");
            return recordMutation(tenantId, actor, payload);
        }

        @Override
        public Map<String, Object> update(Long tenantId, Long id, Map<String, Object> payload, String actor) {
            operations.add("update");
            return recordMutationWithId(tenantId, actor, "experimentId", id, payload);
        }

        @Override
        public Map<String, Object> delete(Long tenantId, Long id) {
            operations.add("delete");
            lastTenantId = tenantId;
            return view(tenantId, "delete", DEFAULT_ACTOR, Map.of("experimentId", id));
        }

        @Override
        public List<Map<String, Object>> listGroups(Long tenantId, Long experimentId, boolean includeDisabled) {
            operations.add("listGroups");
            lastTenantId = tenantId;
            lastExperimentId = experimentId;
            return List.of(view(tenantId, "listGroups", DEFAULT_ACTOR,
                    Map.of("experimentId", experimentId, "includeDisabled", includeDisabled)));
        }

        @Override
        public Map<String, Object> createGroup(Long tenantId, Long experimentId, Map<String, Object> payload,
                                               String actor) {
            operations.add("createGroup");
            lastExperimentId = experimentId;
            return recordMutationWithId(tenantId, actor, "experimentId", experimentId, payload);
        }

        @Override
        public Map<String, Object> updateGroup(Long tenantId, Long experimentId, Long groupId,
                                               Map<String, Object> payload, String actor) {
            operations.add("updateGroup");
            lastExperimentId = experimentId;
            Map<String, Object> row = recordMutationWithId(tenantId, actor, "experimentId", experimentId, payload);
            row.put("groupId", groupId);
            return row;
        }

        @Override
        public Map<String, Object> deleteGroup(Long tenantId, Long experimentId, Long groupId) {
            operations.add("deleteGroup");
            lastTenantId = tenantId;
            lastExperimentId = experimentId;
            return view(tenantId, "deleteGroup", DEFAULT_ACTOR,
                    Map.of("experimentId", experimentId, "groupId", groupId));
        }

        @Override
        public Map<String, Object> evaluateGovernance(Long tenantId, Long experimentId, String controlVariantKey,
                                                      String actor) {
            operations.add("evaluateGovernance");
            lastTenantId = tenantId;
            lastExperimentId = experimentId;
            lastControlVariantKey = controlVariantKey;
            lastActor = actor;
            if (failEvaluate) {
                throw new IllegalArgumentException("AB experiment not found: " + experimentId);
            }
            Map<String, Object> row = view(tenantId, "evaluateGovernance", actor, Map.of());
            row.put("experimentId", experimentId);
            row.put("controlVariantKey", controlVariantKey);
            return row;
        }

        @Override
        public Map<String, Object> evaluateGovernance(Long tenantId, Long experimentId, String controlVariantKey) {
            return evaluateGovernance(tenantId, experimentId, controlVariantKey, DEFAULT_ACTOR);
        }

        private Map<String, Object> recordMutationWithId(Long tenantId, String actor, String idName, Long id,
                                                         Map<String, Object> payload) {
            Map<String, Object> row = recordMutation(tenantId, actor, payload);
            row.put(idName, id);
            return row;
        }

        private Map<String, Object> recordMutation(Long tenantId, String actor, Map<String, Object> payload) {
            lastTenantId = tenantId;
            lastActor = actor;
            lastPayload = new LinkedHashMap<>(payload);
            return view(tenantId, operations.get(operations.size() - 1), actor, lastPayload);
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
