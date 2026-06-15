package org.chovy.canvas.web.canvas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.canvas.api.CreatorCollaborationFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CreatorCollaborationControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    @Test
    void exposesAllLegacyCreatorCollaborationRoutesThroughFinalController() {
        RecordingCreatorCollaborationFacade facade = new RecordingCreatorCollaborationFacade();
        WebTestClient client = webClient(facade);

        List<RouteProbe> probes = List.of(
                post("/creators", "upsertCreator", Map.of("creatorKey", "creator-11", "displayName", "Creator One")),
                post("/campaigns", "upsertCampaign", Map.of("campaignKey", "campaign-21", "name", "Launch")),
                post("/collaborations", "upsertCollaboration", Map.of(
                        "collaborationKey", "collaboration-31",
                        "campaignKey", "campaign-21",
                        "creatorKey", "creator-11")),
                post("/deliverables", "upsertDeliverable", Map.of(
                        "deliverableKey", "deliverable-41",
                        "collaborationKey", "collaboration-31")),
                post("/mutations", "proposeMutation", Map.of("mutationType", "RATE_CARD_UPDATE")),
                post("/mutations/1/approve", "approveMutation", Map.of("comment", "ok")),
                post("/mutations/1/execute", "executeMutation", Map.of("mode", "sync")),
                get("/mutations?campaignId=21&collaborationId=31&status=EXECUTED&approvalStatus=APPROVED&limit=5",
                        "listMutations"),
                get("/summary?campaignId=21&creatorId=11&collaborationId=31&evaluatedAt=2026-06-14T10:00:00",
                        "summary"));

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
    void headersDefaultAndMapToFacadeForMutationRoutes() {
        RecordingCreatorCollaborationFacade facade = new RecordingCreatorCollaborationFacade();

        webClient(facade)
                .post()
                .uri("/canvas/creator-collaboration/mutations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("mutationType", "RATE_CARD_UPDATE"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue())
                .jsonPath("$.data.updatedBy").isEqualTo(DEFAULT_ACTOR)
                .jsonPath("$.data.payload.mutationType").isEqualTo("RATE_CARD_UPDATE");

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(DEFAULT_ACTOR);

        webClient(facade)
                .post()
                .uri("/canvas/creator-collaboration/mutations/9/approve")
                .header("X-Tenant-Id", "42")
                .header("X-Actor", "approver-1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("comment", "approved"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(42)
                .jsonPath("$.data.mutationId").isEqualTo(9)
                .jsonPath("$.data.updatedBy").isEqualTo("approver-1");

        assertThat(facade.lastTenantId).isEqualTo(42L);
        assertThat(facade.lastActor).isEqualTo("approver-1");
        assertThat(facade.lastPayload).containsEntry("comment", "approved");
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingCreatorCollaborationFacade facade = new RecordingCreatorCollaborationFacade();
        facade.failSummary = true;

        webClient(facade)
                .get()
                .uri("/canvas/creator-collaboration/summary?campaignId=999")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("Creator campaign not found: 999")
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CreatorCollaborationFacade facade) {
        return WebTestClient.bindToController(new CreatorCollaborationController(facade)).build();
    }

    private static RouteProbe post(String path, String operation, Map<String, Object> payload) {
        return new RouteProbe("POST", path, operation, payload);
    }

    private static RouteProbe get(String path, String operation) {
        return new RouteProbe("GET", path, operation, Map.of());
    }

    private record RouteProbe(String method, String path, String operation, Map<String, Object> payload) {
        WebTestClient.ResponseSpec exchange(WebTestClient client) {
            String uri = "/canvas/creator-collaboration" + path;
            if ("GET".equals(method)) {
                return client.get().uri(uri).exchange();
            }
            return client.post().uri(uri).contentType(MediaType.APPLICATION_JSON).bodyValue(payload).exchange();
        }
    }

    private static final class RecordingCreatorCollaborationFacade implements CreatorCollaborationFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private String lastActor;
        private Map<String, Object> lastPayload = Map.of();
        private boolean failSummary;

        @Override
        public Map<String, Object> upsertCreator(Long tenantId, Map<String, Object> payload, String actor) {
            operations.add("upsertCreator");
            return recordMutation(tenantId, actor, payload, 11L);
        }

        @Override
        public Map<String, Object> upsertCampaign(Long tenantId, Map<String, Object> payload, String actor) {
            operations.add("upsertCampaign");
            return recordMutation(tenantId, actor, payload, 21L);
        }

        @Override
        public Map<String, Object> upsertCollaboration(Long tenantId, Map<String, Object> payload, String actor) {
            operations.add("upsertCollaboration");
            return recordMutation(tenantId, actor, payload, 31L);
        }

        @Override
        public Map<String, Object> upsertDeliverable(Long tenantId, Map<String, Object> payload, String actor) {
            operations.add("upsertDeliverable");
            return recordMutation(tenantId, actor, payload, 41L);
        }

        @Override
        public Map<String, Object> proposeMutation(Long tenantId, Map<String, Object> payload, String actor) {
            operations.add("proposeMutation");
            return recordMutation(tenantId, actor, payload, 1L);
        }

        @Override
        public Map<String, Object> approveMutation(Long tenantId, Long mutationId, Map<String, Object> payload,
                                                   String actor) {
            operations.add("approveMutation");
            return recordMutation(tenantId, actor, payload, mutationId);
        }

        @Override
        public Map<String, Object> executeMutation(Long tenantId, Long mutationId, Map<String, Object> payload,
                                                   String actor) {
            operations.add("executeMutation");
            return recordMutation(tenantId, actor, payload, mutationId);
        }

        @Override
        public Map<String, Object> listMutations(Long tenantId, Map<String, Object> query) {
            operations.add("listMutations");
            lastTenantId = tenantId;
            return Map.of("total", 1L, "records", List.of(view(tenantId, 1L, DEFAULT_ACTOR, query)));
        }

        @Override
        public Map<String, Object> summary(Long tenantId, Map<String, Object> query) {
            operations.add("summary");
            lastTenantId = tenantId;
            if (failSummary) {
                throw new IllegalArgumentException("Creator campaign not found: 999");
            }
            return view(tenantId, (Long) query.get("campaignId"), DEFAULT_ACTOR, query);
        }

        private Map<String, Object> recordMutation(Long tenantId,
                                                   String actor,
                                                   Map<String, Object> payload,
                                                   Long id) {
            lastTenantId = tenantId;
            lastActor = actor;
            lastPayload = new LinkedHashMap<>(payload);
            return view(tenantId, id, actor, payload);
        }

        private static Map<String, Object> view(Long tenantId, Long id, String actor, Map<String, Object> payload) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tenantId", tenantId);
            row.put("mutationId", id);
            row.put("updatedBy", actor);
            row.put("payload", new LinkedHashMap<>(payload));
            return row;
        }
    }
}
