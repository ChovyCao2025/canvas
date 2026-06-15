package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseTableFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpWarehouseTableControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final Long HEADER_TENANT_ID = 42L;
    private static final String DEFAULT_ACTOR = "operator-1";
    private static final String HEADER_ACTOR = "warehouse-operator";

    @Test
    void exposesAllLegacyWarehouseTableRoutesWithCompatibilityEnvelope() {
        RecordingWarehouseTableFacade facade = new RecordingWarehouseTableFacade();
        WebTestClient client = webClient(facade);

        List<RouteProbe> probes = List.of(
                get("/contracts?layer=DWD&lifecycleStatus=ACTIVE", "listContracts"),
                post("/contracts", "upsertContract", Map.of("tableKey", "dwd_user_profile")),
                post("/contracts/dwd_user_profile/inspect", "inspectContract", Map.of("operator", "inspector")),
                post("/inspect", "inspectAll", Map.of("operator", "inspector")),
                post("/contracts/dwd_user_profile/inspect-live", "inspectLiveContract", Map.of()),
                post("/inspect-live", "inspectLiveAll", Map.of()),
                post("/contracts/dwd_user_profile/remediation-plan?live=false", "planRemediation", Map.of()),
                post("/remediation-plan?live=true", "planAllRemediation", Map.of()),
                post("/incidents/scan?live=true&operator=scanner", "scanIncidents", Map.of()));

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
    void headersDefaultsPathVariablesQueryParametersAndPayloadsAreMappedToFacade() {
        RecordingWarehouseTableFacade facade = new RecordingWarehouseTableFacade();

        webClient(facade)
                .post()
                .uri("/warehouse/tables/contracts")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("tableKey", "ads_order_summary"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.updatedBy").isEqualTo(HEADER_ACTOR)
                .jsonPath("$.data.payload.tableKey").isEqualTo("ads_order_summary");

        assertThat(facade.lastTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(HEADER_ACTOR);
        assertThat(facade.lastPayload).containsEntry("tableKey", "ads_order_summary");

        webClient(facade)
                .post()
                .uri("/warehouse/tables/contracts/dwd_user_profile/inspect-live")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue())
                .jsonPath("$.data.tableKey").isEqualTo("dwd_user_profile")
                .jsonPath("$.data.live").isEqualTo(true)
                .jsonPath("$.data.inspectedBy").isEqualTo(DEFAULT_ACTOR);

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(DEFAULT_ACTOR);
        assertThat(facade.lastTableKey).isEqualTo("dwd_user_profile");

        webClient(facade)
                .get()
                .uri("/warehouse/tables/contracts?layer=ADS&lifecycleStatus=ACTIVE")
                .exchange()
                .expectStatus().isOk();

        assertThat(facade.lastLayer).isEqualTo("ADS");
        assertThat(facade.lastLifecycleStatus).isEqualTo("ACTIVE");
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingWarehouseTableFacade facade = new RecordingWarehouseTableFacade();
        facade.failUpsert = true;

        webClient(facade)
                .post()
                .uri("/warehouse/tables/contracts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of())
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("tableKey is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CdpWarehouseTableFacade facade) {
        return WebTestClient.bindToController(new CdpWarehouseTableController(facade)).build();
    }

    private static RouteProbe get(String path, String operation) {
        return new RouteProbe("GET", path, operation, Map.of());
    }

    private static RouteProbe post(String path, String operation, Map<String, Object> body) {
        return new RouteProbe("POST", path, operation, body);
    }

    private record RouteProbe(String method, String path, String operation, Map<String, Object> body) {
        WebTestClient.ResponseSpec exchange(WebTestClient client) {
            if ("GET".equals(method)) {
                return client.get().uri("/warehouse/tables" + path).exchange();
            }
            return client.post()
                    .uri("/warehouse/tables" + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .exchange();
        }
    }

    private static final class RecordingWarehouseTableFacade implements CdpWarehouseTableFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private String lastActor;
        private Map<String, Object> lastPayload = Map.of();
        private String lastTableKey;
        private String lastLayer;
        private String lastLifecycleStatus;
        private boolean failUpsert;

        @Override
        public Map<String, Object> listContracts(Long tenantId, String layer, String lifecycleStatus) {
            operations.add("listContracts");
            lastTenantId = tenantId;
            lastLayer = layer;
            lastLifecycleStatus = lifecycleStatus;
            return Map.of("total", 1L, "records", List.of(view(tenantId, "listContracts", DEFAULT_ACTOR)));
        }

        @Override
        public Map<String, Object> upsertContract(Long tenantId, Map<String, Object> payload, String actor) {
            operations.add("upsertContract");
            if (failUpsert) {
                throw new IllegalArgumentException("tableKey is required");
            }
            lastPayload = new LinkedHashMap<>(payload);
            return mutation(tenantId, actor, payload);
        }

        @Override
        public Map<String, Object> inspectContract(Long tenantId, String tableKey, String actor, boolean live) {
            operations.add(live ? "inspectLiveContract" : "inspectContract");
            return tableOperation(tenantId, tableKey, actor, live);
        }

        @Override
        public Map<String, Object> inspectAll(Long tenantId, String actor, boolean live) {
            operations.add(live ? "inspectLiveAll" : "inspectAll");
            return allOperation(tenantId, actor, live);
        }

        @Override
        public Map<String, Object> planRemediation(Long tenantId, String tableKey, boolean live, String actor) {
            operations.add("planRemediation");
            return tableOperation(tenantId, tableKey, actor, live);
        }

        @Override
        public Map<String, Object> planAllRemediation(Long tenantId, boolean live, String actor) {
            operations.add("planAllRemediation");
            return allOperation(tenantId, actor, live);
        }

        @Override
        public Map<String, Object> scanIncidents(Long tenantId, boolean live, String actor) {
            operations.add("scanIncidents");
            return allOperation(tenantId, actor, live);
        }

        private Map<String, Object> mutation(Long tenantId, String actor, Map<String, Object> payload) {
            lastTenantId = tenantId;
            lastActor = actor;
            Map<String, Object> row = view(tenantId, operations.get(operations.size() - 1), actor);
            row.put("payload", new LinkedHashMap<>(payload));
            return row;
        }

        private Map<String, Object> tableOperation(Long tenantId, String tableKey, String actor, boolean live) {
            lastTenantId = tenantId;
            lastActor = actor;
            lastTableKey = tableKey;
            Map<String, Object> row = view(tenantId, operations.get(operations.size() - 1), actor);
            row.put("tableKey", tableKey);
            row.put("live", live);
            row.put("inspectedBy", actor);
            return row;
        }

        private Map<String, Object> allOperation(Long tenantId, String actor, boolean live) {
            lastTenantId = tenantId;
            lastActor = actor;
            Map<String, Object> row = view(tenantId, operations.get(operations.size() - 1), actor);
            row.put("live", live);
            return row;
        }

        private static Map<String, Object> view(Long tenantId, String operation, String actor) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tenantId", tenantId);
            row.put("operation", operation);
            row.put("updatedBy", actor);
            return row;
        }
    }
}
