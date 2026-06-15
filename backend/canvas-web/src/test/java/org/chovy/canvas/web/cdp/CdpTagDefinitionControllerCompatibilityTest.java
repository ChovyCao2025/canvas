package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpTagDefinitionFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpTagDefinitionControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final Long HEADER_TENANT_ID = 42L;
    private static final String DEFAULT_ACTOR = "operator-1";
    private static final String HEADER_ACTOR = "tag-definition-operator";

    @Test
    void exposesAllLegacyTagDefinitionRoutesWithCompatibilityEnvelope() {
        RecordingTagDefinitionFacade facade = new RecordingTagDefinitionFacade();
        WebTestClient client = webClient(facade);

        List<RouteProbe> probes = List.of(
                get("", "list"),
                post("", "create", Map.of("tagCode", "vip_level")),
                put("/1", "update", Map.of("tagName", "VIP Segment")),
                delete("/1", "delete"),
                get("/vip_level/values?enabled=1", "listValues"),
                post("/vip_level/values", "createValue", Map.of("valueCode", "gold")),
                put("/values/1", "updateValue", Map.of("valueName", "Gold Members")),
                delete("/values/1", "deleteValue"));

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
    void headersDefaultsPathVariablesQueryParametersAndPayloadsAreMappedToFacade() {
        RecordingTagDefinitionFacade facade = new RecordingTagDefinitionFacade();

        webClient(facade)
                .post()
                .uri("/canvas/tag-definitions")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("tagCode", "ltv_band"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.updatedBy").isEqualTo(HEADER_ACTOR)
                .jsonPath("$.data.payload.tagCode").isEqualTo("ltv_band");

        assertThat(facade.lastTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(HEADER_ACTOR);
        assertThat(facade.lastPayload).containsEntry("tagCode", "ltv_band");

        webClient(facade)
                .put()
                .uri("/canvas/tag-definitions/12")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("tagName", "VIP Segment"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue())
                .jsonPath("$.data.id").isEqualTo(12)
                .jsonPath("$.data.updatedBy").isEqualTo(DEFAULT_ACTOR);

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(DEFAULT_ACTOR);
        assertThat(facade.lastId).isEqualTo(12L);

        webClient(facade)
                .get()
                .uri("/canvas/tag-definitions?page=2&size=3&tagType=PROFILE&enabled=1")
                .exchange()
                .expectStatus().isOk();

        assertThat(facade.lastPage).isEqualTo(2);
        assertThat(facade.lastSize).isEqualTo(3);
        assertThat(facade.lastTagType).isEqualTo("PROFILE");
        assertThat(facade.lastEnabled).isEqualTo(1);

        webClient(facade)
                .post()
                .uri("/canvas/tag-definitions/vip_level/values")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("valueCode", "gold"))
                .exchange()
                .expectStatus().isOk();

        assertThat(facade.lastTagCode).isEqualTo("vip_level");
        assertThat(facade.lastPayload).containsEntry("valueCode", "gold");
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingTagDefinitionFacade facade = new RecordingTagDefinitionFacade();
        facade.failCreate = true;

        webClient(facade)
                .post()
                .uri("/canvas/tag-definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of())
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("tagCode is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CdpTagDefinitionFacade facade) {
        return WebTestClient.bindToController(new CdpTagDefinitionController(facade)).build();
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
                case "GET" -> client.get().uri("/canvas/tag-definitions" + path).exchange();
                case "POST" -> client.post()
                        .uri("/canvas/tag-definitions" + path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .exchange();
                case "PUT" -> client.put()
                        .uri("/canvas/tag-definitions" + path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .exchange();
                case "DELETE" -> client.delete().uri("/canvas/tag-definitions" + path).exchange();
                default -> throw new IllegalStateException("Unsupported method: " + method);
            };
        }
    }

    private static final class RecordingTagDefinitionFacade implements CdpTagDefinitionFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private String lastActor;
        private Map<String, Object> lastPayload = Map.of();
        private Long lastId;
        private String lastTagCode;
        private Integer lastPage;
        private Integer lastSize;
        private String lastTagType;
        private Integer lastEnabled;
        private boolean failCreate;

        @Override
        public Map<String, Object> list(Long tenantId, Integer page, Integer size, String tagType, Integer enabled) {
            operations.add("list");
            lastTenantId = tenantId;
            lastPage = page;
            lastSize = size;
            lastTagType = tagType;
            lastEnabled = enabled;
            return Map.of("total", 1L, "records", List.of(view(tenantId, 1L, DEFAULT_ACTOR)));
        }

        @Override
        public Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor) {
            operations.add("create");
            if (failCreate) {
                throw new IllegalArgumentException("tagCode is required");
            }
            lastTenantId = tenantId;
            lastActor = actor;
            lastPayload = new LinkedHashMap<>(payload);
            Map<String, Object> row = view(tenantId, 1L, actor);
            row.put("payload", new LinkedHashMap<>(payload));
            return row;
        }

        @Override
        public Map<String, Object> update(Long tenantId, Long id, Map<String, Object> payload, String actor) {
            operations.add("update");
            lastTenantId = tenantId;
            lastId = id;
            lastActor = actor;
            lastPayload = new LinkedHashMap<>(payload);
            return view(tenantId, id, actor);
        }

        @Override
        public Map<String, Object> delete(Long tenantId, Long id, String actor) {
            operations.add("delete");
            lastTenantId = tenantId;
            lastId = id;
            lastActor = actor;
            return deleted(tenantId, id, actor);
        }

        @Override
        public Map<String, Object> listValues(Long tenantId, String tagCode, Integer enabled) {
            operations.add("listValues");
            lastTenantId = tenantId;
            lastTagCode = tagCode;
            lastEnabled = enabled;
            return Map.of("tenantId", tenantId, "tagCode", tagCode, "total", 1L, "records", List.of());
        }

        @Override
        public Map<String, Object> createValue(Long tenantId, String tagCode, Map<String, Object> payload,
                String actor) {
            operations.add("createValue");
            lastTenantId = tenantId;
            lastTagCode = tagCode;
            lastPayload = new LinkedHashMap<>(payload);
            lastActor = actor;
            return value(tenantId, tagCode, 1L, actor);
        }

        @Override
        public Map<String, Object> updateValue(Long tenantId, Long id, Map<String, Object> payload, String actor) {
            operations.add("updateValue");
            lastTenantId = tenantId;
            lastId = id;
            lastPayload = new LinkedHashMap<>(payload);
            lastActor = actor;
            return value(tenantId, "vip_level", id, actor);
        }

        @Override
        public Map<String, Object> deleteValue(Long tenantId, Long id, String actor) {
            operations.add("deleteValue");
            lastTenantId = tenantId;
            lastId = id;
            lastActor = actor;
            return deleted(tenantId, id, actor);
        }

        private static Map<String, Object> view(Long tenantId, Long id, String actor) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tenantId", tenantId);
            row.put("id", id);
            row.put("tagCode", "vip_level");
            row.put("tagName", "VIP Level");
            row.put("updatedBy", actor);
            return row;
        }

        private static Map<String, Object> value(Long tenantId, String tagCode, Long id, String actor) {
            Map<String, Object> row = view(tenantId, id, actor);
            row.put("tagCode", tagCode);
            row.put("valueCode", "gold");
            return row;
        }

        private static Map<String, Object> deleted(Long tenantId, Long id, String actor) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tenantId", tenantId);
            row.put("id", id);
            row.put("deleted", true);
            row.put("updatedBy", actor);
            return row;
        }
    }
}
