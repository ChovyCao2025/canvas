package org.chovy.canvas.web.marketing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MarketingFormFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class MarketingFormControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final Long HEADER_TENANT_ID = 42L;
    private static final String DEFAULT_ACTOR = "operator-1";
    private static final String HEADER_ACTOR = "form-operator";

    @Test
    void exposesSixLegacyManagementRouteShapesThroughFinalController() {
        RecordingMarketingFormFacade facade = new RecordingMarketingFormFacade();
        WebTestClient client = webClient(facade);

        List<RouteProbe> probes = List.of(
                get("", "list"),
                get("/1", "get"),
                post("", "create", Map.of("name", "Lead capture")),
                put("/1", "update", Map.of("name", "Lead capture updated")),
                put("/1/status", "setStatus", Map.of("active", false)),
                get("/submissions?formId=1&limit=2", "submissions"));

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
        RecordingMarketingFormFacade facade = new RecordingMarketingFormFacade();

        webClient(facade)
                .post()
                .uri("/canvas/marketing-forms")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Default Form"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue())
                .jsonPath("$.data.updatedBy").isEqualTo(DEFAULT_ACTOR)
                .jsonPath("$.data.payload.name").isEqualTo("Default Form");

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(DEFAULT_ACTOR);

        webClient(facade)
                .put()
                .uri("/canvas/marketing-forms/9/status")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("active", false))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.id").isEqualTo(9)
                .jsonPath("$.data.status").isEqualTo("inactive")
                .jsonPath("$.data.updatedBy").isEqualTo(HEADER_ACTOR);

        assertThat(facade.lastTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(HEADER_ACTOR);
    }

    @Test
    void submissionsForwardFilteringAndLimitParameters() {
        RecordingMarketingFormFacade facade = new RecordingMarketingFormFacade();

        webClient(facade)
                .get()
                .uri("/canvas/marketing-forms/submissions?formId=7&limit=3")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data[0].formId").isEqualTo(7)
                .jsonPath("$.data[0].limit").isEqualTo(3);

        assertThat(facade.lastTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.lastFormId).isEqualTo(7L);
        assertThat(facade.lastLimit).isEqualTo(3);
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingMarketingFormFacade facade = new RecordingMarketingFormFacade();
        facade.failCreate = true;

        webClient(facade)
                .post()
                .uri("/canvas/marketing-forms")
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

    private static WebTestClient webClient(MarketingFormFacade facade) {
        return WebTestClient.bindToController(new MarketingFormController(facade)).build();
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

    private record RouteProbe(String method, String path, String operation, Map<String, Object> body) {
        WebTestClient.ResponseSpec exchange(WebTestClient client) {
            String uri = "/canvas/marketing-forms" + path;
            if ("GET".equals(method)) {
                return client.get().uri(uri).exchange();
            }
            if ("PUT".equals(method)) {
                return client.put().uri(uri).contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange();
            }
            return client.post().uri(uri).contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange();
        }
    }

    private static final class RecordingMarketingFormFacade implements MarketingFormFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private Long lastFormId;
        private Integer lastLimit;
        private String lastActor;
        private Map<String, Object> lastPayload = Map.of();
        private boolean failCreate;

        @Override
        public List<Map<String, Object>> listForms(Long tenantId) {
            operations.add("list");
            lastTenantId = tenantId;
            return List.of(view(tenantId, 1L, "list", DEFAULT_ACTOR, Map.of()));
        }

        @Override
        public Map<String, Object> getForm(Long tenantId, Long id) {
            operations.add("get");
            lastTenantId = tenantId;
            return view(tenantId, id, "get", DEFAULT_ACTOR, Map.of());
        }

        @Override
        public Map<String, Object> createForm(Long tenantId, Map<String, Object> payload, String actor) {
            operations.add("create");
            if (failCreate) {
                throw new IllegalArgumentException("name is required");
            }
            lastTenantId = tenantId;
            lastActor = actor;
            lastPayload = new LinkedHashMap<>(payload);
            return view(tenantId, 10L, "active", actor, payload);
        }

        @Override
        public Map<String, Object> updateForm(Long tenantId, Long id, Map<String, Object> payload, String actor) {
            operations.add("update");
            lastTenantId = tenantId;
            lastActor = actor;
            lastPayload = new LinkedHashMap<>(payload);
            return view(tenantId, id, "active", actor, payload);
        }

        @Override
        public Map<String, Object> setStatus(Long tenantId, Long id, Map<String, Object> payload, String actor) {
            operations.add("setStatus");
            lastTenantId = tenantId;
            lastActor = actor;
            boolean active = Boolean.TRUE.equals(payload.get("active"));
            return view(tenantId, id, active ? "active" : "inactive", actor, Map.of("active", active));
        }

        @Override
        public List<Map<String, Object>> submissions(Long tenantId, Long formId, Integer limit) {
            operations.add("submissions");
            lastTenantId = tenantId;
            lastFormId = formId;
            lastLimit = limit;
            return List.of(Map.of("tenantId", tenantId, "formId", formId, "limit", limit));
        }

        private static Map<String, Object> view(Long tenantId, Long id, String status, String actor,
                                                Map<String, Object> payload) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tenantId", tenantId);
            row.put("id", id);
            row.put("status", status);
            row.put("updatedBy", actor);
            row.put("payload", new LinkedHashMap<>(payload));
            return row;
        }
    }
}
