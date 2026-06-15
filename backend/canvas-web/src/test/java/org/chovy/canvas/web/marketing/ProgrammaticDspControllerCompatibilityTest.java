package org.chovy.canvas.web.marketing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.ProgrammaticDspFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class ProgrammaticDspControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final Long HEADER_TENANT_ID = 42L;
    private static final String DEFAULT_ACTOR = "operator-1";
    private static final String HEADER_ACTOR = "dsp-operator";

    @Test
    void exposesAllLegacyProgrammaticDspRoutesThroughFinalController() {
        RecordingProgrammaticDspFacade facade = new RecordingProgrammaticDspFacade();
        WebTestClient client = webClient(facade);

        List<RouteProbe> probes = List.of(
                post("/seats", "upsertSeat", Map.of("seatKey", "seat-a")),
                post("/campaigns", "upsertCampaign", Map.of("campaignKey", "cmp-a", "seatId", 1)),
                post("/line-items", "upsertLineItem", Map.of("lineItemKey", "li-a", "campaignId", 1)),
                post("/supply-paths", "upsertSupplyPath", Map.of("supplyPathKey", "sp-a", "seatId", 1)),
                post("/snapshots", "recordSnapshot", Map.of("lineItemId", 1)),
                get("/summary?seatId=1&campaignId=1&lineItemId=1", "summary"),
                post("/mutations", "proposeMutation", Map.of("lineItemId", 1, "mutationType", "BID_UPDATE")),
                post("/mutations/1/approve", "approveMutation", Map.of("comment", "approved")),
                post("/mutations/1/execute", "executeMutation", Map.of("providerRequestId", "req-1")),
                get("/mutations?lineItemId=1&status=EXECUTED&approvalStatus=APPROVED&limit=10", "listMutations"));

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
    void mapsHeadersPayloadsPathVariablesAndQueryParametersToFacade() {
        RecordingProgrammaticDspFacade facade = new RecordingProgrammaticDspFacade();

        webClient(facade)
                .post()
                .uri("/canvas/programmatic-dsp/seats")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "seatKey": "seat-a",
                          "name": "Seat A"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.updatedBy").isEqualTo(HEADER_ACTOR)
                .jsonPath("$.data.payload.seatKey").isEqualTo("seat-a");

        assertThat(facade.lastTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(HEADER_ACTOR);
        assertThat(facade.lastPayload).containsEntry("seatKey", "seat-a");

        webClient(facade)
                .post()
                .uri("/canvas/programmatic-dsp/mutations/99/execute")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue())
                .jsonPath("$.data.updatedBy").isEqualTo(DEFAULT_ACTOR)
                .jsonPath("$.data.mutationId").isEqualTo(99)
                .jsonPath("$.data.payload").isMap();

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(DEFAULT_ACTOR);
        assertThat(facade.lastPayload).isEmpty();

        webClient(facade)
                .get()
                .uri("/canvas/programmatic-dsp/summary?seatId=1&campaignId=2&lineItemId=3"
                        + "&startDate=2026-06-01&endDate=2026-06-30&evaluatedAt=2026-06-14T11:00:00")
                .exchange()
                .expectStatus().isOk();

        assertThat(facade.lastQuery).containsEntry("seatId", 1L)
                .containsEntry("campaignId", 2L)
                .containsEntry("lineItemId", 3L);
        assertThat(facade.lastQuery.get("startDate").toString()).isEqualTo("2026-06-01");
        assertThat(facade.lastQuery.get("evaluatedAt").toString()).isEqualTo("2026-06-14T11:00");
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingProgrammaticDspFacade facade = new RecordingProgrammaticDspFacade();
        facade.failSnapshot = true;

        webClient(facade)
                .post()
                .uri("/canvas/programmatic-dsp/snapshots")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"lineItemId": 99}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("DSP line item not found: 99")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(ProgrammaticDspFacade facade) {
        return WebTestClient.bindToController(new ProgrammaticDspController(facade)).build();
    }

    private static RouteProbe post(String path, String operation, Map<String, Object> payload) {
        return new RouteProbe("POST", path, operation, payload);
    }

    private static RouteProbe get(String path, String operation) {
        return new RouteProbe("GET", path, operation, Map.of());
    }

    private record RouteProbe(String method, String path, String operation, Map<String, Object> payload) {
        WebTestClient.ResponseSpec exchange(WebTestClient client) {
            String uri = "/canvas/programmatic-dsp" + path;
            if ("GET".equals(method)) {
                return client.get().uri(uri).exchange();
            }
            return client.post().uri(uri).contentType(MediaType.APPLICATION_JSON).bodyValue(payload).exchange();
        }
    }

    private static final class RecordingProgrammaticDspFacade implements ProgrammaticDspFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private String lastActor;
        private Map<String, Object> lastPayload = Map.of();
        private Map<String, Object> lastQuery = Map.of();
        private boolean failSnapshot;

        @Override
        public Map<String, Object> upsertSeat(Long tenantId, Map<String, Object> payload, String actor) {
            operations.add("upsertSeat");
            return recordMutation(tenantId, actor, payload);
        }

        @Override
        public Map<String, Object> upsertCampaign(Long tenantId, Map<String, Object> payload, String actor) {
            operations.add("upsertCampaign");
            return recordMutation(tenantId, actor, payload);
        }

        @Override
        public Map<String, Object> upsertLineItem(Long tenantId, Map<String, Object> payload, String actor) {
            operations.add("upsertLineItem");
            return recordMutation(tenantId, actor, payload);
        }

        @Override
        public Map<String, Object> upsertSupplyPath(Long tenantId, Map<String, Object> payload, String actor) {
            operations.add("upsertSupplyPath");
            return recordMutation(tenantId, actor, payload);
        }

        @Override
        public Map<String, Object> recordSnapshot(Long tenantId, Map<String, Object> payload, String actor) {
            operations.add("recordSnapshot");
            if (failSnapshot) {
                throw new IllegalArgumentException("DSP line item not found: 99");
            }
            return recordMutation(tenantId, actor, payload);
        }

        @Override
        public Map<String, Object> summary(Long tenantId, Map<String, Object> query) {
            operations.add("summary");
            lastTenantId = tenantId;
            lastQuery = new LinkedHashMap<>(query);
            return view(tenantId, "summary", DEFAULT_ACTOR, Map.of("query", lastQuery));
        }

        @Override
        public Map<String, Object> proposeMutation(Long tenantId, Map<String, Object> payload, String actor) {
            operations.add("proposeMutation");
            return recordMutation(tenantId, actor, payload);
        }

        @Override
        public Map<String, Object> approveMutation(Long tenantId, Long mutationId, Map<String, Object> payload,
                                                   String actor) {
            operations.add("approveMutation");
            return recordMutationWithId(tenantId, actor, mutationId, payload);
        }

        @Override
        public Map<String, Object> executeMutation(Long tenantId, Long mutationId, Map<String, Object> payload,
                                                   String actor) {
            operations.add("executeMutation");
            return recordMutationWithId(tenantId, actor, mutationId, payload);
        }

        @Override
        public List<Map<String, Object>> listMutations(Long tenantId, Map<String, Object> query) {
            operations.add("listMutations");
            lastTenantId = tenantId;
            lastQuery = new LinkedHashMap<>(query);
            return List.of(view(tenantId, "listMutations", DEFAULT_ACTOR, Map.of("query", lastQuery)));
        }

        private Map<String, Object> recordMutationWithId(Long tenantId, String actor, Long mutationId,
                                                         Map<String, Object> payload) {
            Map<String, Object> row = recordMutation(tenantId, actor, new LinkedHashMap<>(payload));
            row.put("mutationId", mutationId);
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
            if (payload.containsKey("mutationId")) {
                row.put("mutationId", payload.get("mutationId"));
            }
            return row;
        }
    }
}
