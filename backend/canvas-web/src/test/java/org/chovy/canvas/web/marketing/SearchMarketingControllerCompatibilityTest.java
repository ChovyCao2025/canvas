package org.chovy.canvas.web.marketing;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.SearchMarketingFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class SearchMarketingControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final Long HEADER_TENANT_ID = 42L;
    private static final String DEFAULT_ACTOR = "operator-1";
    private static final String HEADER_ACTOR = "search-operator";

    @Test
    void mutatingRoutesPreserveEnvelopeTenantActorAndPayloadMapping() {
        RecordingSearchMarketingFacade facade = new RecordingSearchMarketingFacade();

        webClient(facade)
                .post()
                .uri("/canvas/search-marketing/sources")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "provider": "google",
                          "sourceKey": "gsc-main",
                          "channel": "SEO"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.operation").isEqualTo("upsertSource")
                .jsonPath("$.data.updatedBy").isEqualTo(HEADER_ACTOR)
                .jsonPath("$.data.payload.sourceKey").isEqualTo("gsc-main");

        assertThat(facade.lastTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(HEADER_ACTOR);
        assertThat(facade.lastPayload).containsEntry("sourceKey", "gsc-main");
    }

    @Test
    void queryRoutesPreserveFiltersLimitsAndIsoDates() {
        RecordingSearchMarketingFacade facade = new RecordingSearchMarketingFacade();

        webClient(facade)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/search-marketing/snapshots")
                        .queryParam("channel", "SEO")
                        .queryParam("sourceId", 10)
                        .queryParam("keywordId", 20)
                        .queryParam("startDate", "2026-06-01")
                        .queryParam("endDate", "2026-06-14")
                        .queryParam("limit", 25)
                        .build())
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data").isArray()
                .jsonPath("$.data[0].operation").isEqualTo("listSnapshots");

        assertThat(facade.lastTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.lastCriteria).containsEntry("channel", "SEO")
                .containsEntry("sourceId", 10L)
                .containsEntry("keywordId", 20L)
                .containsEntry("startDate", LocalDate.parse("2026-06-01"))
                .containsEntry("endDate", LocalDate.parse("2026-06-14"))
                .containsEntry("limit", 25);
    }

    @Test
    void missingHeadersAndOptionalBodiesUseCompatibilityDefaults() {
        RecordingSearchMarketingFacade facade = new RecordingSearchMarketingFacade();

        webClient(facade)
                .post()
                .uri("/canvas/search-marketing/sources/10/sync")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue())
                .jsonPath("$.data.updatedBy").isEqualTo(DEFAULT_ACTOR)
                .jsonPath("$.data.payload.runType").isEqualTo("PERFORMANCE");

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(DEFAULT_ACTOR);
        assertThat(facade.lastPayload).containsEntry("runType", "PERFORMANCE");

        webClient(facade)
                .post()
                .uri("/canvas/search-marketing/sources/sync-due")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.payload.limit").isEqualTo(50);

        assertThat(facade.lastPayload).containsEntry("limit", 50);
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingSearchMarketingFacade facade = new RecordingSearchMarketingFacade();
        facade.failUpsertSource = true;

        webClient(facade)
                .post()
                .uri("/canvas/search-marketing/sources")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"provider": ""}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("provider is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    @Test
    void exposesAllLegacySearchMarketingRouteShapesThroughFinalController() {
        RecordingSearchMarketingFacade facade = new RecordingSearchMarketingFacade();
        WebTestClient client = webClient(facade);

        List<RouteProbe> probes = List.of(
                get("/sources?provider=google&channel=SEO&enabled=true&limit=5", "listSources"),
                post("/sources", "upsertSource", Map.of("provider", "google")),
                get("/keywords?channel=SEO&status=active&limit=5", "listKeywords"),
                post("/keywords", "upsertKeyword", Map.of("keywordText", "canvas")),
                get("/snapshots?channel=SEO&sourceId=10&keywordId=20&startDate=2026-06-01&endDate=2026-06-14&limit=5", "listSnapshots"),
                post("/snapshots", "upsertSnapshot", Map.of("snapshotDate", "2026-06-14")),
                get("/opportunities?channel=SEO&sourceId=10&status=open&severity=high&limit=5", "listOpportunities"),
                post("/opportunities/evaluate", "evaluateOpportunities", Map.of("channel", "SEO")),
                post("/opportunities/10/status", "updateOpportunityStatus", Map.of("status", "accepted")),
                post("/opportunities/10/mutations", "createOpportunityMutation", Map.of("mutationKey", "title-fix")),
                get("/mutations?sourceId=10&status=pending&approvalStatus=pending&limit=5", "listMutations"),
                post("/mutations", "upsertMutation", Map.of("mutationKey", "bid-fix")),
                post("/mutations/20/approve", "approveMutation", Map.of("decision", "approved")),
                post("/mutations/20/execute", "executeMutation", Map.of("dryRun", false)),
                get("/url-inspections?sourceId=10&indexedState=indexed&startDate=2026-06-01&endDate=2026-06-14&limit=5", "listUrlInspections"),
                get("/sync-runs?sourceId=10&runType=PERFORMANCE&status=SCHEDULED&limit=5", "listSyncRuns"),
                post("/sources/10/sync", "syncSource", Map.of("runType", "PERFORMANCE")),
                post("/sources/sync-due", "syncDue", Map.of("limit", 50)),
                get("/provider-changes?sourceId=10&mutationId=20&provider=google&reconciliationStatus=RECONCILED&limit=5", "listProviderChanges"),
                post("/mutations/20/reconcile", "reconcileMutation", Map.of("mutationId", 20L)),
                get("/impact-windows?opportunityId=10&mutationId=20&sourceId=30&status=PENDING&decision=KEEP&limit=5", "listImpactWindows"),
                post("/impact-windows/evaluate-due", "evaluateDueImpactWindows", Map.of("limit", 50)),
                get("/readiness", "readiness"),
                get("/summary?channel=SEO&sourceId=10&keywordId=20&startDate=2026-06-01&endDate=2026-06-14", "summary"));

        for (RouteProbe probe : probes) {
            probe.exchange(client)
                    .expectStatus().isOk()
                    .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(0)
                    .jsonPath("$.message").isEqualTo("success");
        }

        assertThat(facade.operations).containsExactlyElementsOf(probes.stream()
                .map(RouteProbe::operation)
                .toList());
    }

    private static WebTestClient webClient(SearchMarketingFacade facade) {
        return WebTestClient.bindToController(new SearchMarketingController(facade)).build();
    }

    private static RouteProbe post(String path, String operation, Map<String, Object> expectedPayload) {
        return new RouteProbe("POST", path, operation, expectedPayload);
    }

    private static RouteProbe get(String path, String operation) {
        return new RouteProbe("GET", path, operation, Map.of());
    }

    private record RouteProbe(String method, String path, String operation, Map<String, Object> expectedPayload) {
        WebTestClient.ResponseSpec exchange(WebTestClient client) {
            if ("GET".equals(method)) {
                return client.get().uri("/canvas/search-marketing" + path).exchange();
            }
            return client.post()
                    .uri("/canvas/search-marketing" + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(expectedPayload)
                    .exchange();
        }
    }

    private static final class RecordingSearchMarketingFacade implements SearchMarketingFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private String lastActor;
        private Map<String, Object> lastPayload;
        private Map<String, Object> lastCriteria;
        private boolean failUpsertSource;

        @Override
        public List<Map<String, Object>> listSources(Long tenantId, String provider, String channel,
                                                     Boolean enabled, Integer limit) {
            operations.add("listSources");
            lastTenantId = tenantId;
            lastCriteria = criteria("provider", provider, "channel", channel, "enabled", enabled, "limit", limit);
            return List.of(view(tenantId, "listSources", lastCriteria, DEFAULT_ACTOR));
        }

        @Override
        public Map<String, Object> upsertSource(Long tenantId, Map<String, Object> payload, String actor) {
            if (failUpsertSource) {
                throw new IllegalArgumentException("provider is required");
            }
            return recordMutation(tenantId, "upsertSource", payload, actor);
        }

        @Override
        public List<Map<String, Object>> listKeywords(Long tenantId, String channel, String status, Integer limit) {
            operations.add("listKeywords");
            return List.of(view(tenantId, "listKeywords", Map.of(), DEFAULT_ACTOR));
        }

        @Override
        public Map<String, Object> upsertKeyword(Long tenantId, Map<String, Object> payload, String actor) {
            return recordMutation(tenantId, "upsertKeyword", payload, actor);
        }

        @Override
        public List<Map<String, Object>> listSnapshots(Long tenantId, String channel, Long sourceId, Long keywordId,
                                                       LocalDate startDate, LocalDate endDate, Integer limit) {
            operations.add("listSnapshots");
            lastTenantId = tenantId;
            lastCriteria = criteria("channel", channel, "sourceId", sourceId, "keywordId", keywordId,
                    "startDate", startDate, "endDate", endDate, "limit", limit);
            return List.of(view(tenantId, "listSnapshots", lastCriteria, DEFAULT_ACTOR));
        }

        @Override
        public Map<String, Object> upsertSnapshot(Long tenantId, Map<String, Object> payload, String actor) {
            return recordMutation(tenantId, "upsertSnapshot", payload, actor);
        }

        @Override
        public List<Map<String, Object>> listOpportunities(Long tenantId, String channel, Long sourceId,
                                                           String status, String severity, Integer limit) {
            operations.add("listOpportunities");
            return List.of(view(tenantId, "listOpportunities", Map.of(), DEFAULT_ACTOR));
        }

        @Override
        public Map<String, Object> evaluateOpportunities(Long tenantId, Map<String, Object> payload, String actor) {
            return recordMutation(tenantId, "evaluateOpportunities", payload, actor);
        }

        @Override
        public Map<String, Object> updateOpportunityStatus(Long tenantId, Long opportunityId,
                                                           Map<String, Object> payload, String actor) {
            return recordMutation(tenantId, "updateOpportunityStatus", with(payload, "opportunityId", opportunityId), actor);
        }

        @Override
        public Map<String, Object> createOpportunityMutation(Long tenantId, Long opportunityId,
                                                             Map<String, Object> payload, String actor) {
            return recordMutation(tenantId, "createOpportunityMutation", with(payload, "opportunityId", opportunityId), actor);
        }

        @Override
        public List<Map<String, Object>> listMutations(Long tenantId, Long sourceId, String status,
                                                       String approvalStatus, Integer limit) {
            operations.add("listMutations");
            return List.of(view(tenantId, "listMutations", Map.of(), DEFAULT_ACTOR));
        }

        @Override
        public Map<String, Object> upsertMutation(Long tenantId, Map<String, Object> payload, String actor) {
            return recordMutation(tenantId, "upsertMutation", payload, actor);
        }

        @Override
        public Map<String, Object> approveMutation(Long tenantId, Long mutationId, Map<String, Object> payload, String actor) {
            return recordMutation(tenantId, "approveMutation", with(payload, "mutationId", mutationId), actor);
        }

        @Override
        public Map<String, Object> executeMutation(Long tenantId, Long mutationId, Map<String, Object> payload, String actor) {
            return recordMutation(tenantId, "executeMutation", with(payload, "mutationId", mutationId), actor);
        }

        @Override
        public List<Map<String, Object>> listUrlInspections(Long tenantId, Long sourceId, String indexedState,
                                                            LocalDate startDate, LocalDate endDate, Integer limit) {
            operations.add("listUrlInspections");
            return List.of(view(tenantId, "listUrlInspections", Map.of(), DEFAULT_ACTOR));
        }

        @Override
        public List<Map<String, Object>> listSyncRuns(Long tenantId, Long sourceId, String runType,
                                                      String status, Integer limit) {
            operations.add("listSyncRuns");
            return List.of(view(tenantId, "listSyncRuns", Map.of(), DEFAULT_ACTOR));
        }

        @Override
        public Map<String, Object> syncSource(Long tenantId, Long sourceId, Map<String, Object> payload, String actor) {
            return recordMutation(tenantId, "syncSource", with(payload, "sourceId", sourceId), actor);
        }

        @Override
        public Map<String, Object> syncDue(Long tenantId, Map<String, Object> payload, String actor) {
            return recordMutation(tenantId, "syncDue", payload, actor);
        }

        @Override
        public List<Map<String, Object>> listProviderChanges(Long tenantId, Long sourceId, Long mutationId,
                                                             String provider, String reconciliationStatus, Integer limit) {
            operations.add("listProviderChanges");
            return List.of(view(tenantId, "listProviderChanges", Map.of(), DEFAULT_ACTOR));
        }

        @Override
        public Map<String, Object> reconcileMutation(Long tenantId, Long mutationId, String actor) {
            return recordMutation(tenantId, "reconcileMutation", Map.of("mutationId", mutationId), actor);
        }

        @Override
        public List<Map<String, Object>> listImpactWindows(Long tenantId, Long opportunityId, Long mutationId,
                                                           Long sourceId, String status, String decision, Integer limit) {
            operations.add("listImpactWindows");
            return List.of(view(tenantId, "listImpactWindows", Map.of(), DEFAULT_ACTOR));
        }

        @Override
        public Map<String, Object> evaluateDueImpactWindows(Long tenantId, Map<String, Object> payload, String actor) {
            return recordMutation(tenantId, "evaluateDueImpactWindows", payload, actor);
        }

        @Override
        public Map<String, Object> readiness(Long tenantId) {
            operations.add("readiness");
            return view(tenantId, "readiness", Map.of(), DEFAULT_ACTOR);
        }

        @Override
        public Map<String, Object> summary(Long tenantId, String channel, Long sourceId, Long keywordId,
                                           LocalDate startDate, LocalDate endDate) {
            operations.add("summary");
            return view(tenantId, "summary", Map.of(), DEFAULT_ACTOR);
        }

        private Map<String, Object> recordMutation(Long tenantId, String operation,
                                                   Map<String, Object> payload, String actor) {
            operations.add(operation);
            lastTenantId = tenantId;
            lastActor = actor;
            lastPayload = payload;
            return view(tenantId, operation, payload, actor);
        }

        private static Map<String, Object> view(Long tenantId, String operation,
                                                Map<String, Object> payload, String actor) {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("tenantId", tenantId);
            view.put("operation", operation);
            view.put("payload", payload);
            view.put("updatedBy", actor);
            return view;
        }

        private static Map<String, Object> with(Map<String, Object> payload, String key, Object value) {
            Map<String, Object> result = new LinkedHashMap<>(payload);
            result.put(key, value);
            return result;
        }

        private static Map<String, Object> criteria(Object... values) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (int i = 0; i < values.length; i += 2) {
                result.put((String) values[i], values[i + 1]);
            }
            return result;
        }
    }
}
