package org.chovy.canvas.web.risk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.risk.api.RiskGovernanceFacade;
import org.chovy.canvas.risk.application.RiskGovernanceApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class RiskGovernanceControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    @Test
    void exposesAllMissingLegacyRiskGovernanceRoutesThroughFinalController() {
        WebTestClient client = webClient(new RiskGovernanceApplicationService());

        List<RouteProbe> probes = List.of(
                get("/decisions/traces?sceneKey=MARKETING_BENEFIT_ISSUE&limit=10"),
                post("/lists", Map.of("listKey", "coupon_abuse", "listType", "black")),
                post("/lists/coupon_abuse/entries", Map.of("subjectValue", "user-1")),
                get("/lists/coupon_abuse/entries"),
                delete("/lists/coupon_abuse/entries/1"),
                post("/lists/coupon_abuse/entries/import", Map.of("values", "user-2,user-3")),
                post("/strategies", Map.of("strategyKey", "benefit_default", "sceneKey", "MARKETING_BENEFIT_ISSUE")),
                get("/strategies/benefit_default"),
                get("/strategies/benefit_default/versions"),
                post("/strategies/benefit_default/versions/1/validate", Map.of()),
                post("/strategies/benefit_default/versions/1/simulate", Map.of()),
                post("/strategies/benefit_default/versions/1/submit", Map.of()),
                post("/strategies/benefit_default/versions/1/approve", Map.of()),
                post("/strategies/benefit_default/versions/1/activate", Map.of()),
                post("/strategies/benefit_default/rollback", Map.of("targetVersion", 1)),
                post("/strategies/benefit_default/pause", Map.of()),
                get("/strategies/benefit_default/versions/1/diff/1"),
                post("/lab/simulations", Map.of(
                        "sceneKey", "MARKETING_BENEFIT_ISSUE",
                        "strategyKey", "benefit_default",
                        "candidateVersion", 1)),
                get("/lab/simulations?sceneKey=MARKETING_BENEFIT_ISSUE&limit=10"));

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
    }

    @Test
    void missingHeadersUseCompatibilityDefaultsAndBadRequestsMapToApi001() {
        RecordingRiskGovernanceFacade facade = new RecordingRiskGovernanceFacade();

        webClient(facade)
                .post()
                .uri("/canvas/risk/lists")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("listKey", "coupon_abuse"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue())
                .jsonPath("$.data.updatedBy").isEqualTo(DEFAULT_ACTOR);

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(DEFAULT_ACTOR);

        facade.failCreateList = true;

        webClient(facade)
                .post()
                .uri("/canvas/risk/lists")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("listType", "black"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("listKey is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(RiskGovernanceFacade facade) {
        return WebTestClient.bindToController(new RiskGovernanceController(facade)).build();
    }

    private static RouteProbe get(String path) {
        return new RouteProbe("GET", path, Map.of());
    }

    private static RouteProbe post(String path, Map<String, Object> body) {
        return new RouteProbe("POST", path, body);
    }

    private static RouteProbe delete(String path) {
        return new RouteProbe("DELETE", path, Map.of());
    }

    private record RouteProbe(String method, String path, Map<String, Object> body) {
        WebTestClient.ResponseSpec exchange(WebTestClient client) {
            return switch (method) {
                case "GET" -> client.get().uri("/canvas/risk" + path).exchange();
                case "DELETE" -> client.delete().uri("/canvas/risk" + path).exchange();
                default -> client.post()
                        .uri("/canvas/risk" + path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .exchange();
            };
        }
    }

    private static final class RecordingRiskGovernanceFacade extends RiskGovernanceApplicationService {
        private Long lastTenantId;
        private String lastActor;
        private boolean failCreateList;

        @Override
        public Map<String, Object> createList(Long tenantId, Map<String, Object> payload, String actor) {
            if (failCreateList) {
                throw new IllegalArgumentException("listKey is required");
            }
            lastTenantId = tenantId;
            lastActor = actor;
            return Map.of("tenantId", tenantId, "updatedBy", actor);
        }
    }
}
