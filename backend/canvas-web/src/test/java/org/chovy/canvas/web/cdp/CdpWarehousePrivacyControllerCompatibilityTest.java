package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehousePrivacyFacade;
import org.chovy.canvas.cdp.application.CdpWarehousePrivacyApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpWarehousePrivacyControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    @Test
    void exposesAllLegacyWarehousePrivacyRoutesWithCompatibilityEnvelope() {
        WebTestClient client = webClient(new CdpWarehousePrivacyApplicationService());

        List<RouteProbe> probes = List.of(
                post("/erasure/requests", Map.of("subjectType", "USER_ID", "subjectValue", "user-1")),
                post("/erasure/requests/1001/proofs", Map.of("assetKey", "profile")),
                post("/erasure/requests/1001/execute", Map.of("mode", "dry-run")),
                post("/erasure/requests/1001/audience-rebuild", Map.of("audienceId", 88L)),
                post("/erasure/audience-rebuild/automation/run", Map.of("strategy", "recent")),
                get("/erasure/audience-rebuild/automation/runs?limit=5"),
                get("/erasure/audience-rebuild/automation/runs/1401"),
                get("/erasure/requests?status=REQUESTED&limit=5"),
                get("/erasure/requests/1001"),
                get("/erasure/summary"),
                post("/tombstones", Map.of("subjectType", "USER_ID", "subjectValue", "user-1")),
                post("/tombstones/from-erasure-request", Map.of("requestId", 1001L)),
                post("/tombstones/2001/revoke", Map.of("reason", "appeal")),
                get("/tombstones?status=ACTIVE&limit=5"),
                get("/tombstones/decision?subjectType=USER_ID&subjectValue=user-1"));

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
        RecordingWarehousePrivacyFacade facade = new RecordingWarehousePrivacyFacade();

        webClient(facade)
                .post()
                .uri("/warehouse/privacy/erasure/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("subjectValue", "user-1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue())
                .jsonPath("$.data.createdBy").isEqualTo(DEFAULT_ACTOR);

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(DEFAULT_ACTOR);

        facade.failCreateErasure = true;

        webClient(facade)
                .post()
                .uri("/warehouse/privacy/erasure/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of())
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("subjectValue is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CdpWarehousePrivacyFacade facade) {
        return WebTestClient.bindToController(new CdpWarehousePrivacyController(facade)).build();
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
                return client.get().uri("/warehouse/privacy" + path).exchange();
            }
            return client.post()
                    .uri("/warehouse/privacy" + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .exchange();
        }
    }

    private static final class RecordingWarehousePrivacyFacade extends CdpWarehousePrivacyApplicationService {
        private Long lastTenantId;
        private String lastActor;
        private boolean failCreateErasure;

        @Override
        public Map<String, Object> createErasureRequest(Long tenantId, Map<String, Object> payload, String actor) {
            if (failCreateErasure) {
                throw new IllegalArgumentException("subjectValue is required");
            }
            lastTenantId = tenantId;
            lastActor = actor;
            return Map.of("tenantId", tenantId, "createdBy", actor);
        }
    }
}
