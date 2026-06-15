package org.chovy.canvas.web.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.platform.api.AiFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class AiControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final Long HEADER_TENANT_ID = 42L;
    private static final String DEFAULT_ACTOR = "operator-1";
    private static final String HEADER_ACTOR = "ai-operator";

    @Test
    void mutatingRoutesPreserveEnvelopeTenantActorAndPayloadMapping() {
        RecordingAiFacade facade = new RecordingAiFacade();

        webClient(facade)
                .post()
                .uri("/ai/providers")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "OpenAI", "providerKey", "openai"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.operation").isEqualTo("createProvider")
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.updatedBy").isEqualTo(HEADER_ACTOR)
                .jsonPath("$.data.payload.providerKey").isEqualTo("openai");

        assertThat(facade.lastTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(HEADER_ACTOR);
        assertThat(facade.lastPayload).containsEntry("providerKey", "openai");
    }

    @Test
    void missingHeadersAndOptionalBodiesUseCompatibilityDefaults() {
        RecordingAiFacade facade = new RecordingAiFacade();

        webClient(facade)
                .post()
                .uri("/ai/decisions/recompute")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.operation").isEqualTo("recomputeDecision")
                .jsonPath("$.data.tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue())
                .jsonPath("$.data.updatedBy").isEqualTo(DEFAULT_ACTOR)
                .jsonPath("$.data.payload").isMap();

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(DEFAULT_ACTOR);
        assertThat(facade.lastPayload).isEmpty();
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingAiFacade facade = new RecordingAiFacade();
        facade.failCreateProvider = true;

        webClient(facade)
                .post()
                .uri("/ai/providers")
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

    @Test
    void exposesAllLegacyAiRouteShapesThroughFinalController() {
        RecordingAiFacade facade = new RecordingAiFacade();
        WebTestClient client = webClient(facade);

        List<RouteProbe> probes = List.of(
                post("/decisions/recompute", "recomputeDecision", Map.of("decisionScope", "DAILY_MARKETING")),
                get("/decisions/latest-run?decisionScope=DAILY_MARKETING", "latestDecisionRun"),
                get("/decisions/recommendations?runId=11&decisionType=COUPON&eligibilityStatus=ELIGIBLE&limit=5", "decisionRecommendations"),
                post("/decisions/recommendations/7001/feedback", "recordDecisionFeedback", Map.of("decision", "accepted")),
                get("/predictions/latest-run", "latestPredictionRun"),
                get("/predictions/readiness", "predictionReadiness"),
                get("/predictions/churn-distribution", "churnDistribution"),
                get("/predictions/top-risk-users?limit=5", "topRiskUsers"),
                post("/predictions/recompute", "recomputePrediction", Map.of("force", true)),
                get("/prompt-templates", "promptTemplates"),
                post("/prompt-templates", "createPromptTemplate", Map.of("name", "Retention", "template", "Hi")),
                get("/prompt-templates/7001", "promptTemplate"),
                put("/prompt-templates/7001", "updatePromptTemplate", Map.of("name", "Retention v2")),
                post("/prompt-templates/7001/disable", "disablePromptTemplate", Map.of()),
                post("/prompt-templates/render", "renderPromptTemplate", Map.of("templateId", 7001)),
                post("/prompt-templates/evaluate", "evaluatePromptTemplate", Map.of("templateId", 7001)),
                get("/prompt-templates/evaluation-audits", "evaluationAudits"),
                get("/providers", "providers"),
                post("/providers", "createProvider", Map.of("name", "OpenAI")),
                get("/providers/7001", "provider"),
                put("/providers/7001", "updateProvider", Map.of("displayName", "Primary")),
                post("/providers/7001/disable", "disableProvider", Map.of()),
                get("/providers/7001/models", "providerModels"));

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

    private static WebTestClient webClient(AiFacade facade) {
        return WebTestClient.bindToController(new AiController(facade)).build();
    }

    private static RouteProbe post(String path, String operation, Map<String, Object> expectedPayload) {
        return new RouteProbe("POST", path, operation, expectedPayload);
    }

    private static RouteProbe put(String path, String operation, Map<String, Object> expectedPayload) {
        return new RouteProbe("PUT", path, operation, expectedPayload);
    }

    private static RouteProbe get(String path, String operation) {
        return new RouteProbe("GET", path, operation, Map.of());
    }

    private record RouteProbe(String method, String path, String operation, Map<String, Object> expectedPayload) {
        WebTestClient.ResponseSpec exchange(WebTestClient client) {
            if ("GET".equals(method)) {
                return client.get().uri("/ai" + path).exchange();
            }
            WebTestClient.RequestBodySpec spec = "PUT".equals(method)
                    ? client.put().uri("/ai" + path)
                    : client.post().uri("/ai" + path);
            return spec.contentType(MediaType.APPLICATION_JSON).bodyValue(expectedPayload).exchange();
        }
    }

    private static final class RecordingAiFacade implements AiFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private String lastActor;
        private Map<String, Object> lastPayload;
        private boolean failCreateProvider;

        @Override
        public Map<String, Object> recomputeDecision(Long tenantId, Map<String, Object> payload, String actor) {
            return mutation("recomputeDecision", tenantId, payload, actor);
        }

        @Override
        public Map<String, Object> latestDecisionRun(Long tenantId, String decisionScope) {
            return query("latestDecisionRun", tenantId, Map.of("decisionScope", decisionScope));
        }

        @Override
        public List<Map<String, Object>> decisionRecommendations(Long tenantId, Long runId, String decisionType,
                                                                 String eligibilityStatus, Integer limit) {
            return list("decisionRecommendations", tenantId, Map.of("limit", limit));
        }

        @Override
        public Map<String, Object> recordDecisionFeedback(Long tenantId, Long recommendationId,
                                                          Map<String, Object> payload, String actor) {
            return mutation("recordDecisionFeedback", tenantId, payload, actor);
        }

        @Override
        public Map<String, Object> latestPredictionRun(Long tenantId) {
            return query("latestPredictionRun", tenantId, Map.of());
        }

        @Override
        public Map<String, Object> predictionReadiness(Long tenantId) {
            return query("predictionReadiness", tenantId, Map.of());
        }

        @Override
        public List<Map<String, Object>> churnDistribution(Long tenantId) {
            return list("churnDistribution", tenantId, Map.of());
        }

        @Override
        public List<Map<String, Object>> topRiskUsers(Long tenantId, Integer limit) {
            return list("topRiskUsers", tenantId, Map.of("limit", limit));
        }

        @Override
        public Map<String, Object> recomputePrediction(Long tenantId, Map<String, Object> payload) {
            return mutation("recomputePrediction", tenantId, payload, null);
        }

        @Override
        public List<Map<String, Object>> promptTemplates(Long tenantId) {
            return list("promptTemplates", tenantId, Map.of());
        }

        @Override
        public Map<String, Object> createPromptTemplate(Long tenantId, Map<String, Object> payload, String actor) {
            return mutation("createPromptTemplate", tenantId, payload, actor);
        }

        @Override
        public Map<String, Object> promptTemplate(Long tenantId, Long id) {
            return query("promptTemplate", tenantId, Map.of("id", id));
        }

        @Override
        public Map<String, Object> updatePromptTemplate(Long tenantId, Long id, Map<String, Object> payload,
                                                        String actor) {
            return mutation("updatePromptTemplate", tenantId, payload, actor);
        }

        @Override
        public Map<String, Object> disablePromptTemplate(Long tenantId, Long id, String actor) {
            return mutation("disablePromptTemplate", tenantId, Map.of("id", id), actor);
        }

        @Override
        public Map<String, Object> renderPromptTemplate(Long tenantId, Map<String, Object> payload) {
            return mutation("renderPromptTemplate", tenantId, payload, null);
        }

        @Override
        public Map<String, Object> evaluatePromptTemplate(Long tenantId, Map<String, Object> payload) {
            return mutation("evaluatePromptTemplate", tenantId, payload, null);
        }

        @Override
        public List<Map<String, Object>> evaluationAudits(Long tenantId) {
            return list("evaluationAudits", tenantId, Map.of());
        }

        @Override
        public List<Map<String, Object>> providers(Long tenantId) {
            return list("providers", tenantId, Map.of());
        }

        @Override
        public Map<String, Object> createProvider(Long tenantId, Map<String, Object> payload, String actor) {
            if (failCreateProvider) {
                throw new IllegalArgumentException("name is required");
            }
            return mutation("createProvider", tenantId, payload, actor);
        }

        @Override
        public Map<String, Object> provider(Long tenantId, Long id) {
            return query("provider", tenantId, Map.of("id", id));
        }

        @Override
        public Map<String, Object> updateProvider(Long tenantId, Long id, Map<String, Object> payload, String actor) {
            return mutation("updateProvider", tenantId, payload, actor);
        }

        @Override
        public Map<String, Object> disableProvider(Long tenantId, Long id, String actor) {
            return mutation("disableProvider", tenantId, Map.of("id", id), actor);
        }

        @Override
        public List<Map<String, Object>> providerModels(Long tenantId, Long id) {
            return list("providerModels", tenantId, Map.of("id", id));
        }

        private Map<String, Object> mutation(String operation, Long tenantId, Map<String, Object> payload,
                                             String actor) {
            operations.add(operation);
            lastTenantId = tenantId;
            lastActor = actor;
            lastPayload = payload == null ? Map.of() : new LinkedHashMap<>(payload);
            Map<String, Object> data = query(operation, tenantId, Map.of("payload", lastPayload));
            data.put("updatedBy", actor);
            return data;
        }

        private Map<String, Object> query(String operation, Long tenantId, Map<String, Object> values) {
            if (!operations.contains(operation)) {
                operations.add(operation);
            }
            lastTenantId = tenantId;
            Map<String, Object> data = new LinkedHashMap<>(values);
            data.put("operation", operation);
            data.put("tenantId", tenantId);
            return data;
        }

        private List<Map<String, Object>> list(String operation, Long tenantId, Map<String, Object> values) {
            return List.of(query(operation, tenantId, values));
        }
    }
}
