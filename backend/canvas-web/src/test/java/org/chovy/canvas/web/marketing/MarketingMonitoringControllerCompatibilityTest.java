package org.chovy.canvas.web.marketing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MarketingMonitoringFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class MarketingMonitoringControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final Long HEADER_TENANT_ID = 42L;
    private static final String DEFAULT_ACTOR = "operator-1";
    private static final String HEADER_ACTOR = "growth-operator";

    @Test
    void mutatingRoutesPreserveEnvelopeTenantActorAndPayloadMapping() {
        RecordingMarketingMonitoringFacade facade = new RecordingMarketingMonitoringFacade();

        webClient(facade)
                .post()
                .uri("/canvas/marketing-monitoring/sources")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "sourceKey": "twitter-launch",
                          "providerType": "twitter"
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
                .jsonPath("$.data.payload.sourceKey").isEqualTo("twitter-launch");

        assertThat(facade.lastExecuteTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.lastExecuteActor).isEqualTo(HEADER_ACTOR);
        assertThat(facade.lastExecuteOperation).isEqualTo("upsertSource");
        assertThat(facade.lastExecutePayload).containsEntry("sourceKey", "twitter-launch");
    }

    @Test
    void queryRoutesPreserveFiltersLimitAndArrayEnvelope() {
        RecordingMarketingMonitoringFacade facade = new RecordingMarketingMonitoringFacade();

        webClient(facade)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/marketing-monitoring/inferences")
                        .queryParam("itemId", 10)
                        .queryParam("sentimentLabel", "negative")
                        .queryParam("modelKey", "risk-model")
                        .queryParam("providerStatus", "ok")
                        .queryParam("fallbackUsed", true)
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
                .jsonPath("$.data[0].operation").isEqualTo("inferences");

        assertThat(facade.lastListTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.lastListOperation).isEqualTo("inferences");
        assertThat(facade.lastListCriteria).containsEntry("itemId", 10L)
                .containsEntry("sentimentLabel", "negative")
                .containsEntry("modelKey", "risk-model")
                .containsEntry("providerStatus", "ok")
                .containsEntry("fallbackUsed", true);
        assertThat(facade.lastListLimit).isEqualTo(25);
    }

    @Test
    void missingHeadersUseCompatibilityDefaults() {
        RecordingMarketingMonitoringFacade facade = new RecordingMarketingMonitoringFacade();

        webClient(facade)
                .post()
                .uri("/canvas/marketing-monitoring/items")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"externalId": "post-1"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue())
                .jsonPath("$.data.updatedBy").isEqualTo(DEFAULT_ACTOR);

        assertThat(facade.lastExecuteTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastExecuteActor).isEqualTo(DEFAULT_ACTOR);
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingMarketingMonitoringFacade facade = new RecordingMarketingMonitoringFacade();
        facade.failExecute = true;

        webClient(facade)
                .post()
                .uri("/canvas/marketing-monitoring/provider-credentials")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"credentialKey": ""}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("credentialKey is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    @Test
    void exposesAllLegacyMarketingMonitoringRouteShapesThroughFinalController() {
        RecordingMarketingMonitoringFacade facade = new RecordingMarketingMonitoringFacade();
        WebTestClient client = webClient(facade);

        List<RouteProbe> probes = List.of(
                post("/sources", "upsertSource", Map.of("sourceKey", "twitter-launch")),
                post("/items", "ingestItem", Map.of("externalId", "post-1")),
                get("/items?sentimentLabel=negative&competitorKey=rival&limit=5", "items"),
                get("/alerts?status=open&limit=5", "alerts"),
                post("/alerts/100/resolve", "resolveAlert", Map.of("alertId", 100L)),
                post("/alert-channels", "upsertAlertChannel", Map.of("channelKey", "ops")),
                post("/alerts/100/dispatch", "dispatchAlert", Map.of("alertId", 100L)),
                get("/alert-deliveries?alertId=100&status=sent&limit=5", "alertDeliveries"),
                post("/sources/10/polling", "configureSourcePolling", Map.of("sourceId", 10L)),
                post("/sources/10/poll", "pollSource", Map.of("sourceId", 10L)),
                post("/trends/snapshots/build", "buildTrendSnapshot", Map.of("brandKey", "canvas")),
                get("/trends/snapshots?sourceId=10&brandKey=canvas&competitorKey=rival&limit=5", "trendSnapshots"),
                post("/items/20/inferences", "analyzeInference", Map.of("itemId", 20L)),
                get("/inferences?itemId=20&limit=5", "inferences"),
                post("/provider-credentials", "upsertProviderCredential", Map.of("credentialKey", "twitter-main")),
                get("/provider-credentials?providerType=twitter&authType=oauth&status=active&limit=5", "providerCredentials"),
                post("/provider-credentials/twitter-main/refresh", "refreshProviderCredential", Map.of("credentialKey", "twitter-main")),
                post("/provider-credentials/refresh-due", "refreshDueProviderCredentials", Map.of("limit", 5)),
                post("/provider-credentials/twitter-main/revoke", "revokeProviderCredential", Map.of("credentialKey", "twitter-main")),
                post("/provider-credentials/twitter-main/disable", "disableProviderCredential", Map.of("credentialKey", "twitter-main")),
                get("/provider-credentials/events?credentialKey=twitter-main&eventType=refreshed&status=ok&limit=5", "providerCredentialEvents"),
                post("/provider-credentials/oauth/authorizations", "startProviderOAuthAuthorization", Map.of("providerType", "twitter")),
                post("/provider-credentials/oauth/callback", "completeProviderOAuthAuthorization", Map.of("code", "abc", "state", "state-1")),
                get("/provider-credentials/oauth/authorizations?credentialKey=twitter-main&providerType=twitter&status=started&limit=5", "providerOAuthAuthorizations"),
                get("/provider-credentials/oauth/events?authState=state-1&credentialKey=twitter-main&eventType=started&status=ok&limit=5", "providerOAuthAuthorizationEvents"),
                post("/anomaly-rules", "upsertAnomalyRule", Map.of("ruleKey", "spike")),
                post("/anomalies/detect", "detectAnomalies", Map.of("ruleId", 1L)),
                get("/anomalies?ruleId=1&status=open&limit=5", "anomalies"),
                post("/anomalies/77/resolve", "resolveAnomaly", Map.of("eventId", 77L)),
                post("/sources/10/webhook-secret/rotate", "rotateWebhookSecret", Map.of("sourceId", 10L)));

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

    private static WebTestClient webClient(MarketingMonitoringFacade facade) {
        return WebTestClient.bindToController(new MarketingMonitoringController(facade)).build();
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
                return client.get().uri("/canvas/marketing-monitoring" + path).exchange();
            }
            return client.post()
                    .uri("/canvas/marketing-monitoring" + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(expectedPayload.isEmpty() ? Map.of("probe", operation) : expectedPayload)
                    .exchange();
        }
    }

    private static final class RecordingMarketingMonitoringFacade implements MarketingMonitoringFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastExecuteTenantId;
        private String lastExecuteOperation;
        private Map<String, Object> lastExecutePayload;
        private String lastExecuteActor;
        private Long lastListTenantId;
        private String lastListOperation;
        private Map<String, Object> lastListCriteria;
        private Integer lastListLimit;
        private boolean failExecute;

        @Override
        public Map<String, Object> execute(Long tenantId, String operation, Map<String, Object> payload, String actor) {
            if (failExecute) {
                throw new IllegalArgumentException("credentialKey is required");
            }
            operations.add(operation);
            lastExecuteTenantId = tenantId;
            lastExecuteOperation = operation;
            lastExecutePayload = payload;
            lastExecuteActor = actor;
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("tenantId", tenantId);
            view.put("operation", operation);
            view.put("payload", payload);
            view.put("updatedBy", actor);
            return view;
        }

        @Override
        public List<Map<String, Object>> list(Long tenantId, String operation, Map<String, Object> criteria, Integer limit) {
            operations.add(operation);
            lastListTenantId = tenantId;
            lastListOperation = operation;
            lastListCriteria = criteria;
            lastListLimit = limit;
            return List.of(Map.of("tenantId", tenantId, "operation", operation, "criteria", criteria));
        }
    }
}
