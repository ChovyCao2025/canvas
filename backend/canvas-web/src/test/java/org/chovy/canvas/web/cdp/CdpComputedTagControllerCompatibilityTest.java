package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpComputedTagFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpComputedTagControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final Long HEADER_TENANT_ID = 42L;
    private static final String DEFAULT_ACTOR = "operator-1";
    private static final String HEADER_ACTOR = "computed-tag-operator";

    @Test
    void exposesAllLegacyComputedTagRoutesWithCompatibilityEnvelope() {
        RecordingComputedTagFacade facade = new RecordingComputedTagFacade();
        WebTestClient client = webClient(facade);

        List<RouteProbe> probes = List.of(
                get("", "list"),
                post("", "create", Map.of("tagCode", "vip_score")),
                post("/vip_score/preview", "preview", Map.of()),
                post("/vip_score/activate", "activate", Map.of()),
                post("/vip_score/pause", "pause", Map.of()),
                post("/vip_score/run", "runNow", Map.of()),
                get("/vip_score/runs?limit=5", "listRuns"),
                get("/vip_score/lineage", "lineage"),
                post("/vip_score/impact-check", "impactCheck", Map.of(
                        "oldValueType", "NUMBER",
                        "newValueType", "STRING")));

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
        RecordingComputedTagFacade facade = new RecordingComputedTagFacade();

        webClient(facade)
                .post()
                .uri("/cdp/computed-tags")
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
                .post()
                .uri("/cdp/computed-tags/vip_score/run")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue())
                .jsonPath("$.data.tagCode").isEqualTo("vip_score")
                .jsonPath("$.data.triggeredBy").isEqualTo(DEFAULT_ACTOR);

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(DEFAULT_ACTOR);
        assertThat(facade.lastTagCode).isEqualTo("vip_score");

        webClient(facade)
                .get()
                .uri("/cdp/computed-tags/vip_score/runs?limit=5")
                .exchange()
                .expectStatus().isOk();

        assertThat(facade.lastLimit).isEqualTo(5);
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingComputedTagFacade facade = new RecordingComputedTagFacade();
        facade.failCreate = true;

        webClient(facade)
                .post()
                .uri("/cdp/computed-tags")
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

    private static WebTestClient webClient(CdpComputedTagFacade facade) {
        return WebTestClient.bindToController(new CdpComputedTagController(facade)).build();
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
                return client.get().uri("/cdp/computed-tags" + path).exchange();
            }
            return client.post()
                    .uri("/cdp/computed-tags" + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .exchange();
        }
    }

    private static final class RecordingComputedTagFacade implements CdpComputedTagFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private String lastActor;
        private Map<String, Object> lastPayload = Map.of();
        private String lastTagCode;
        private Integer lastLimit;
        private boolean failCreate;

        @Override
        public Map<String, Object> list(Long tenantId) {
            operations.add("list");
            lastTenantId = tenantId;
            return Map.of("total", 1L, "records", List.of(view(tenantId, "list", DEFAULT_ACTOR)));
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
            Map<String, Object> row = view(tenantId, "create", actor);
            row.put("payload", new LinkedHashMap<>(payload));
            return row;
        }

        @Override
        public Map<String, Object> preview(Long tenantId, String tagCode) {
            operations.add("preview");
            return tagOperation(tenantId, tagCode, DEFAULT_ACTOR);
        }

        @Override
        public Map<String, Object> activate(Long tenantId, String tagCode, String actor) {
            operations.add("activate");
            return tagOperation(tenantId, tagCode, actor);
        }

        @Override
        public Map<String, Object> pause(Long tenantId, String tagCode, String actor) {
            operations.add("pause");
            return tagOperation(tenantId, tagCode, actor);
        }

        @Override
        public Map<String, Object> runNow(Long tenantId, String tagCode, String actor) {
            operations.add("runNow");
            Map<String, Object> row = tagOperation(tenantId, tagCode, actor);
            row.put("triggeredBy", actor);
            return row;
        }

        @Override
        public Map<String, Object> listRuns(Long tenantId, String tagCode, Integer limit) {
            operations.add("listRuns");
            lastTenantId = tenantId;
            lastTagCode = tagCode;
            lastLimit = limit;
            return Map.of("tenantId", tenantId, "tagCode", tagCode, "limit", limit, "records", List.of());
        }

        @Override
        public Map<String, Object> lineage(Long tenantId, String tagCode) {
            operations.add("lineage");
            return tagOperation(tenantId, tagCode, DEFAULT_ACTOR);
        }

        @Override
        public Map<String, Object> impactCheck(Long tenantId, String tagCode, Map<String, Object> payload,
                String actor) {
            operations.add("impactCheck");
            lastPayload = new LinkedHashMap<>(payload);
            return tagOperation(tenantId, tagCode, actor);
        }

        private Map<String, Object> tagOperation(Long tenantId, String tagCode, String actor) {
            lastTenantId = tenantId;
            lastActor = actor;
            lastTagCode = tagCode;
            return view(tenantId, operations.get(operations.size() - 1), actor);
        }

        private static Map<String, Object> view(Long tenantId, String operation, String actor) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tenantId", tenantId);
            row.put("tagCode", "vip_score");
            row.put("operation", operation);
            row.put("updatedBy", actor);
            return row;
        }
    }
}
