package org.chovy.canvas.web.marketing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MarketingIntegrationFacade;
import org.chovy.canvas.marketing.application.MarketingIntegrationApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class MarketingIntegrationControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    @Test
    void exposesAllLegacyMarketingIntegrationRoutesWithCompatibilityEnvelope() {
        WebTestClient client = webClient(new MarketingIntegrationApplicationService());

        List<RouteProbe> probes = List.of(
                post("/contracts", Map.of("providerKey", "meta", "providerFamily", "SOCIAL")),
                get("/contracts?status=ACTIVE&providerFamily=SOCIAL&limit=5"),
                get("/contracts/3001/audit-events?limit=5"),
                delete("/contracts/3001"),
                post("/contracts/3001/probe-runs", Map.of("probeKey", "auth")),
                get("/contract-probe-runs?status=PASSED&providerFamily=SOCIAL&limit=5"),
                post("/contract-probe-runs/scan?limit=5", Map.of()),
                get("/contract-slo-evaluations?limit=5"),
                post("/contracts/3001/probes", Map.of("probeKey", "auth")),
                get("/contracts/3001/probes?limit=5"),
                get("/probes?status=ACTIVE&limit=5"));

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
        RecordingMarketingIntegrationFacade facade = new RecordingMarketingIntegrationFacade();

        webClient(facade)
                .post()
                .uri("/canvas/marketing-integrations/contracts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("providerKey", "meta"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue())
                .jsonPath("$.data.updatedBy").isEqualTo(DEFAULT_ACTOR);

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(DEFAULT_ACTOR);

        facade.failUpsertContract = true;

        webClient(facade)
                .post()
                .uri("/canvas/marketing-integrations/contracts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of())
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("providerKey is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(MarketingIntegrationFacade facade) {
        return WebTestClient.bindToController(new MarketingIntegrationController(facade)).build();
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
            if ("GET".equals(method)) {
                return client.get().uri("/canvas/marketing-integrations" + path).exchange();
            }
            if ("DELETE".equals(method)) {
                return client.delete().uri("/canvas/marketing-integrations" + path).exchange();
            }
            return client.post()
                    .uri("/canvas/marketing-integrations" + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .exchange();
        }
    }

    private static final class RecordingMarketingIntegrationFacade extends MarketingIntegrationApplicationService {
        private Long lastTenantId;
        private String lastActor;
        private boolean failUpsertContract;

        @Override
        public Map<String, Object> upsertContract(Long tenantId, Map<String, Object> payload, String actor) {
            if (failUpsertContract) {
                throw new IllegalArgumentException("providerKey is required");
            }
            lastTenantId = tenantId;
            lastActor = actor;
            return Map.of("tenantId", tenantId, "updatedBy", actor);
        }
    }
}
