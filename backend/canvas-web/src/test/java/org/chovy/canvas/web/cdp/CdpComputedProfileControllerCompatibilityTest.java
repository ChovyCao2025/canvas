package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpComputedProfileFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpComputedProfileControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final Long HEADER_TENANT_ID = 42L;
    private static final String DEFAULT_ACTOR = "operator-1";
    private static final String HEADER_ACTOR = "computed-profile-operator";

    @Test
    void exposesAllLegacyComputedProfileRoutesWithCompatibilityEnvelope() {
        RecordingComputedProfileFacade facade = new RecordingComputedProfileFacade();
        WebTestClient client = webClient(facade);

        List<RouteProbe> probes = List.of(
                get("", "list"),
                post("", "create", Map.of("attributeCode", "ltv_band")),
                post("/1/preview", "preview", Map.of()),
                post("/1/activate", "activate", Map.of()),
                post("/1/pause", "pause", Map.of()),
                post("/1/run", "runNow", Map.of()),
                get("/1/runs?limit=5", "listRuns"),
                get("/1/changes?userId=user-1&limit=5", "listChanges"));

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
        RecordingComputedProfileFacade facade = new RecordingComputedProfileFacade();

        webClient(facade)
                .post()
                .uri("/cdp/computed-profile-attributes")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("attributeCode", "ltv_band"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.updatedBy").isEqualTo(HEADER_ACTOR)
                .jsonPath("$.data.payload.attributeCode").isEqualTo("ltv_band");

        assertThat(facade.lastTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(HEADER_ACTOR);
        assertThat(facade.lastPayload).containsEntry("attributeCode", "ltv_band");

        webClient(facade)
                .post()
                .uri("/cdp/computed-profile-attributes/12/run")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue())
                .jsonPath("$.data.id").isEqualTo(12)
                .jsonPath("$.data.triggeredBy").isEqualTo(DEFAULT_ACTOR);

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(DEFAULT_ACTOR);
        assertThat(facade.lastId).isEqualTo(12L);

        webClient(facade)
                .get()
                .uri("/cdp/computed-profile-attributes/12/changes?userId=user-1&limit=5")
                .exchange()
                .expectStatus().isOk();

        assertThat(facade.lastId).isEqualTo(12L);
        assertThat(facade.lastUserId).isEqualTo("user-1");
        assertThat(facade.lastLimit).isEqualTo(5);
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingComputedProfileFacade facade = new RecordingComputedProfileFacade();
        facade.failCreate = true;

        webClient(facade)
                .post()
                .uri("/cdp/computed-profile-attributes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of())
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("attributeCode is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CdpComputedProfileFacade facade) {
        return WebTestClient.bindToController(new CdpComputedProfileController(facade)).build();
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
                return client.get().uri("/cdp/computed-profile-attributes" + path).exchange();
            }
            return client.post()
                    .uri("/cdp/computed-profile-attributes" + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .exchange();
        }
    }

    private static final class RecordingComputedProfileFacade implements CdpComputedProfileFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private String lastActor;
        private Map<String, Object> lastPayload = Map.of();
        private Long lastId;
        private String lastUserId;
        private Integer lastLimit;
        private boolean failCreate;

        @Override
        public Map<String, Object> list(Long tenantId) {
            operations.add("list");
            lastTenantId = tenantId;
            return Map.of("total", 1L, "records", List.of(view(tenantId, 1L, "list", DEFAULT_ACTOR)));
        }

        @Override
        public Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor) {
            operations.add("create");
            if (failCreate) {
                throw new IllegalArgumentException("attributeCode is required");
            }
            lastTenantId = tenantId;
            lastActor = actor;
            lastPayload = new LinkedHashMap<>(payload);
            Map<String, Object> row = view(tenantId, 1L, "create", actor);
            row.put("payload", new LinkedHashMap<>(payload));
            return row;
        }

        @Override
        public Map<String, Object> preview(Long tenantId, Long id) {
            operations.add("preview");
            return idOperation(tenantId, id, DEFAULT_ACTOR);
        }

        @Override
        public Map<String, Object> activate(Long tenantId, Long id, String actor) {
            operations.add("activate");
            return idOperation(tenantId, id, actor);
        }

        @Override
        public Map<String, Object> pause(Long tenantId, Long id, String actor) {
            operations.add("pause");
            return idOperation(tenantId, id, actor);
        }

        @Override
        public Map<String, Object> runNow(Long tenantId, Long id, String actor) {
            operations.add("runNow");
            Map<String, Object> row = idOperation(tenantId, id, actor);
            row.put("triggeredBy", actor);
            return row;
        }

        @Override
        public Map<String, Object> listRuns(Long tenantId, Long id, Integer limit) {
            operations.add("listRuns");
            lastTenantId = tenantId;
            lastId = id;
            lastLimit = limit;
            return Map.of("tenantId", tenantId, "id", id, "limit", limit, "records", List.of());
        }

        @Override
        public Map<String, Object> listChanges(Long tenantId, Long id, String userId, Integer limit) {
            operations.add("listChanges");
            lastTenantId = tenantId;
            lastId = id;
            lastUserId = userId;
            lastLimit = limit;
            return Map.of("tenantId", tenantId, "id", id, "userId", userId, "limit", limit, "records", List.of());
        }

        private Map<String, Object> idOperation(Long tenantId, Long id, String actor) {
            lastTenantId = tenantId;
            lastActor = actor;
            lastId = id;
            return view(tenantId, id, operations.get(operations.size() - 1), actor);
        }

        private static Map<String, Object> view(Long tenantId, Long id, String operation, String actor) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tenantId", tenantId);
            row.put("id", id);
            row.put("operation", operation);
            row.put("updatedBy", actor);
            return row;
        }
    }
}
