package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseAvailabilityFacade;
import org.chovy.canvas.cdp.application.CdpWarehouseAvailabilityApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpWarehouseAvailabilityControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    @Test
    void exposesAllLegacyWarehouseAvailabilityRoutes() {
        WebTestClient client = webClient(new CdpWarehouseAvailabilityApplicationService());

        List<RouteProbe> probes = List.of(
                get("?mode=HYBRID"),
                post("/assets", Map.of("assetType", "table", "assetKey", "dwd_orders", "mode", "HYBRID")),
                get("/assets?assetType=table&assetKey=dwd_orders&mode=HYBRID&limit=10"),
                post("/contracts", Map.of("contractKey", "contract-orders", "consumerType", "bi",
                        "assetKey", "dwd_orders")),
                get("/contracts?consumerType=bi&status=ACTIVE"),
                post("/contracts/contract-orders/evaluate", Map.of()),
                post("/incidents/scan", Map.of("mode", "HYBRID")),
                post("/consumer-incidents/scan", Map.of("contractKey", "contract-orders", "consumerType", "bi")));

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
        RecordingWarehouseAvailabilityFacade facade = new RecordingWarehouseAvailabilityFacade();

        webClient(facade)
                .post()
                .uri("/warehouse/availability/assets")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("assetType", "table", "assetKey", "dwd_orders"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue())
                .jsonPath("$.data.updatedBy").isEqualTo(DEFAULT_ACTOR);

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(DEFAULT_ACTOR);

        facade.failRecordAsset = true;

        webClient(facade)
                .post()
                .uri("/warehouse/availability/assets")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("assetType", "table"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("assetKey is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CdpWarehouseAvailabilityFacade facade) {
        return WebTestClient.bindToController(new CdpWarehouseAvailabilityController(facade)).build();
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
                return client.get().uri("/warehouse/availability" + path).exchange();
            }
            return client.post()
                    .uri("/warehouse/availability" + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .exchange();
        }
    }

    private static final class RecordingWarehouseAvailabilityFacade extends CdpWarehouseAvailabilityApplicationService {
        private Long lastTenantId;
        private String lastActor;
        private boolean failRecordAsset;

        @Override
        public Map<String, Object> recordAssetAvailability(Long tenantId, Map<String, Object> payload, String actor) {
            if (failRecordAsset) {
                throw new IllegalArgumentException("assetKey is required");
            }
            lastTenantId = tenantId;
            lastActor = actor;
            return Map.of("tenantId", tenantId, "updatedBy", actor);
        }
    }
}
