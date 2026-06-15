package org.chovy.canvas.web.marketing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.GrowthActivityFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class GrowthActivityControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final Long HEADER_TENANT_ID = 42L;
    private static final String DEFAULT_ACTOR = "operator-1";
    private static final String HEADER_ACTOR = "growth-operator";

    @Test
    void mutatingRoutesPreserveEnvelopeTenantActorAndPayloadMapping() {
        RecordingGrowthActivityFacade facade = new RecordingGrowthActivityFacade();

        webClient(facade)
                .post()
                .uri("/canvas/growth-activities/10/reward-pools")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "poolKey": "coupon-pool",
                          "rewardType": "coupon"
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
                .jsonPath("$.data.activityId").isEqualTo(10)
                .jsonPath("$.data.operation").isEqualTo("upsertRewardPool")
                .jsonPath("$.data.updatedBy").isEqualTo(HEADER_ACTOR)
                .jsonPath("$.data.payload.poolKey").isEqualTo("coupon-pool");

        assertThat(facade.lastExecuteTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.lastExecuteActivityId).isEqualTo(10L);
        assertThat(facade.lastExecuteOperation).isEqualTo("upsertRewardPool");
        assertThat(facade.lastExecuteActor).isEqualTo(HEADER_ACTOR);
        assertThat(facade.lastExecutePayload).containsEntry("poolKey", "coupon-pool");
    }

    @Test
    void queryRoutesPreserveFiltersLimitAndArrayEnvelope() {
        RecordingGrowthActivityFacade facade = new RecordingGrowthActivityFacade();

        webClient(facade)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/growth-activities")
                        .queryParam("activityType", "referral")
                        .queryParam("status", "draft")
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
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].operation").isEqualTo("listActivities");

        assertThat(facade.lastListActivitiesTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.lastActivityType).isEqualTo("referral");
        assertThat(facade.lastActivityStatus).isEqualTo("draft");
        assertThat(facade.lastActivityLimit).isEqualTo(25);
    }

    @Test
    void missingHeadersUseCompatibilityDefaults() {
        RecordingGrowthActivityFacade facade = new RecordingGrowthActivityFacade();

        webClient(facade)
                .post()
                .uri("/canvas/growth-activities")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"activityKey": "spring-referral"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue())
                .jsonPath("$.data.updatedBy").isEqualTo(DEFAULT_ACTOR);

        assertThat(facade.lastUpsertTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastUpsertActor).isEqualTo(DEFAULT_ACTOR);
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingGrowthActivityFacade facade = new RecordingGrowthActivityFacade();
        facade.failExecute = true;

        webClient(facade)
                .post()
                .uri("/canvas/growth-activities/10/grants")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"participantId": ""}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("participantId is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    @Test
    void exposesAllLegacyGrowthActivityRouteShapesThroughFinalController() {
        RecordingGrowthActivityFacade facade = new RecordingGrowthActivityFacade();
        WebTestClient client = webClient(facade);

        List<RouteProbe> probes = List.of(
                post("", "upsertActivity", Map.of("activityKey", "spring-referral")),
                get("?activityType=referral&status=draft&limit=5", "listActivities"),
                get("/10", "getActivity"),
                get("/10/report", "report"),
                get("/10/readiness", "readiness"),
                get("/10/reward-pools", "rewardPools"),
                post("/10/reward-pools", "upsertRewardPool", Map.of("poolKey", "coupon-pool")),
                get("/10/grants", "grants"),
                post("/10/grants", "createGrant", Map.of("participantId", 200L)),
                post("/10/grants/20/retry", "retryGrant", Map.of("grantId", 20L)),
                post("/10/grants/20/reconcile", "reconcileGrant", Map.of("grantId", 20L)),
                post("/10/grants/20/cancel", "cancelGrant", Map.of("grantId", 20L)),
                get("/10/referral-codes", "referralCodes"),
                post("/10/referral-codes", "generateReferralCode", Map.of("participantId", 200L)),
                get("/10/referrals", "referrals"),
                post("/10/referrals", "upsertReferral", Map.of("referrerParticipantId", 200L)),
                post("/10/referrals/30/qualify", "qualifyReferral", Map.of("relationId", 30L)),
                get("/10/tasks", "tasks"),
                post("/10/tasks", "upsertTask", Map.of("taskKey", "share")),
                get("/10/task-progress", "taskProgress"),
                post("/10/task-progress", "recordTaskProgress", Map.of("participantId", 200L)),
                post("/10/task-progress/40/reset", "resetTaskProgress", Map.of("progressId", 40L)),
                post("/10/publish", "publish", Map.of()),
                post("/10/pause", "pause", Map.of()),
                post("/10/close", "close", Map.of()));

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

    private static WebTestClient webClient(GrowthActivityFacade facade) {
        return WebTestClient.bindToController(new GrowthActivityController(facade)).build();
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
                return client.get().uri("/canvas/growth-activities" + path).exchange();
            }
            return client.post()
                    .uri("/canvas/growth-activities" + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(expectedPayload.isEmpty() ? Map.of("probe", operation) : expectedPayload)
                    .exchange();
        }
    }

    private static final class RecordingGrowthActivityFacade implements GrowthActivityFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastUpsertTenantId;
        private String lastUpsertActor;
        private Long lastExecuteTenantId;
        private Long lastExecuteActivityId;
        private String lastExecuteOperation;
        private Map<String, Object> lastExecutePayload;
        private String lastExecuteActor;
        private Long lastListActivitiesTenantId;
        private String lastActivityType;
        private String lastActivityStatus;
        private Integer lastActivityLimit;
        private boolean failExecute;

        @Override
        public Map<String, Object> upsertActivity(Long tenantId, Map<String, Object> payload, String actor) {
            operations.add("upsertActivity");
            lastUpsertTenantId = tenantId;
            lastUpsertActor = actor;
            return view(tenantId, null, "upsertActivity", payload, actor);
        }

        @Override
        public List<Map<String, Object>> listActivities(Long tenantId, String activityType, String status, Integer limit) {
            operations.add("listActivities");
            lastListActivitiesTenantId = tenantId;
            lastActivityType = activityType;
            lastActivityStatus = status;
            lastActivityLimit = limit;
            return List.of(view(tenantId, null, "listActivities",
                    Map.of("activityType", activityType, "status", status, "limit", limit), DEFAULT_ACTOR));
        }

        @Override
        public Map<String, Object> getActivity(Long tenantId, Long activityId) {
            operations.add("getActivity");
            return view(tenantId, activityId, "getActivity", Map.of(), DEFAULT_ACTOR);
        }

        @Override
        public Map<String, Object> report(Long tenantId, Long activityId) {
            operations.add("report");
            return view(tenantId, activityId, "report", Map.of(), DEFAULT_ACTOR);
        }

        @Override
        public Map<String, Object> readiness(Long tenantId, Long activityId) {
            operations.add("readiness");
            return view(tenantId, activityId, "readiness", Map.of(), DEFAULT_ACTOR);
        }

        @Override
        public Map<String, Object> transitionActivity(Long tenantId, Long activityId, String transition, String actor) {
            operations.add(transition);
            return view(tenantId, activityId, transition, Map.of(), actor);
        }

        @Override
        public Map<String, Object> execute(Long tenantId, Long activityId, String operation, Map<String, Object> payload, String actor) {
            if (failExecute) {
                throw new IllegalArgumentException("participantId is required");
            }
            operations.add(operation);
            lastExecuteTenantId = tenantId;
            lastExecuteActivityId = activityId;
            lastExecuteOperation = operation;
            lastExecutePayload = payload;
            lastExecuteActor = actor;
            return view(tenantId, activityId, operation, payload, actor);
        }

        @Override
        public List<Map<String, Object>> list(Long tenantId, Long activityId, String resource, Map<String, Object> criteria, Integer limit) {
            operations.add(resource);
            return List.of(view(tenantId, activityId, resource, criteria, DEFAULT_ACTOR));
        }

        private static Map<String, Object> view(Long tenantId, Long activityId, String operation,
                                                Map<String, Object> payload, String actor) {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("tenantId", tenantId);
            if (activityId != null) {
                view.put("activityId", activityId);
            }
            view.put("operation", operation);
            view.put("payload", payload);
            view.put("updatedBy", actor);
            return view;
        }
    }
}
