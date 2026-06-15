package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseAudienceFacade;
import org.chovy.canvas.cdp.application.CdpWarehouseAudienceApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpWarehouseAudienceControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    @Test
    void exposesAllLegacyWarehouseAudienceRoutesWithCompatibilityEnvelope() {
        WebTestClient client = webClient(new CdpWarehouseAudienceApplicationService());

        List<RouteProbe> probes = List.of(
                post("/42/materialize", Map.of()),
                post("/42/materialize-gated", Map.of("mode", "HYBRID", "allowWarn", true)),
                post("/42/materialize-contract-gated", Map.of("contractKey", "contract-audience")),
                post("/42/materialization/rollback", Map.of("targetVersion", 1L, "reason", "bad segment")),
                post("/materialization/refresh-due", Map.of("limit", 1)),
                post("/materialization/refresh-due-gated", Map.of("limit", 1, "mode", "HYBRID")),
                get("/materialization-runs?audienceId=42&status=SUCCEEDED&limit=10"));

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
    void missingHeadersUseCompatibilityDefaultsAndBodyOperatorWins() {
        RecordingWarehouseAudienceFacade facade = new RecordingWarehouseAudienceFacade();

        webClient(facade)
                .post()
                .uri("/warehouse/audiences/42/materialize")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue())
                .jsonPath("$.data.operator").isEqualTo(DEFAULT_ACTOR);

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(DEFAULT_ACTOR);

        webClient(facade)
                .post()
                .uri("/warehouse/audiences/42/materialize-gated")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("operator", "body-operator"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.operator").isEqualTo("body-operator");

        assertThat(facade.lastActor).isEqualTo("body-operator");
    }

    @Test
    void badRequestsUseLegacyApi001Envelope() {
        webClient(new CdpWarehouseAudienceApplicationService())
                .post()
                .uri("/warehouse/audiences/42/materialize-contract-gated")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of())
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("contractKey is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CdpWarehouseAudienceFacade facade) {
        return WebTestClient.bindToController(new CdpWarehouseAudienceController(facade)).build();
    }

    private static RouteProbe get(String path) {
        return new RouteProbe("GET", path, Map.of());
    }

    private static RouteProbe post(String path, Map<String, Object> body) {
        return new RouteProbe("POST", path, body);
    }

    private record RouteProbe(String method, String path, Map<String, Object> body) {
        WebTestClient.ResponseSpec exchange(WebTestClient client) {
            if ("GET".equals(method)) {
                return client.get().uri("/warehouse/audiences" + path).exchange();
            }
            return client.post()
                    .uri("/warehouse/audiences" + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .exchange();
        }
    }

    private static final class RecordingWarehouseAudienceFacade extends CdpWarehouseAudienceApplicationService {
        private Long lastTenantId;
        private String lastActor;

        @Override
        public Map<String, Object> materialize(Long tenantId, Long audienceId, String actor) {
            lastTenantId = tenantId;
            lastActor = actor;
            return Map.of("tenantId", tenantId, "audienceId", audienceId, "operator", actor);
        }

        @Override
        public Map<String, Object> materializeGated(
                Long tenantId,
                Long audienceId,
                Map<String, Object> payload,
                String actor) {
            lastTenantId = tenantId;
            lastActor = actor;
            return Map.of("tenantId", tenantId, "audienceId", audienceId, "operator", actor);
        }
    }
}
