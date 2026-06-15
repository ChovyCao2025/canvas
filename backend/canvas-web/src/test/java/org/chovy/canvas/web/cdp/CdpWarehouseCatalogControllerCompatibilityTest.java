package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseCatalogFacade;
import org.chovy.canvas.cdp.api.CdpWarehouseCatalogFacade.DatasetCommand;
import org.chovy.canvas.cdp.api.CdpWarehouseCatalogFacade.Direction;
import org.chovy.canvas.cdp.api.CdpWarehouseCatalogFacade.LineageCommand;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpWarehouseCatalogControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 0L;
    private static final Long HEADER_TENANT_ID = 42L;

    @Test
    void exposesLegacyWarehouseCatalogRoutesWithCompatibilityEnvelope() {
        RecordingWarehouseCatalogFacade facade = new RecordingWarehouseCatalogFacade();
        WebTestClient client = webClient(facade);

        List<RouteProbe> probes = List.of(
                get("/datasets?layer=DWD&status=ACTIVE", "listDatasets"),
                post("/datasets", "upsertDataset", datasetPayload("dwd_user_profile")),
                post("/lineage", "createLineageEdge", lineagePayload("ods_event", "dwd_user_profile")),
                get("/datasets/dwd_user_profile/lineage?direction=BOTH", "lineage"),
                get("/datasets/dwd_user_profile/lineage/transitive?direction=DOWNSTREAM&maxDepth=2",
                        "transitiveLineage"));

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
    void mapsTenantHeaderDefaultTenantQueryParametersPathVariablesAndRequestBodies() {
        RecordingWarehouseCatalogFacade facade = new RecordingWarehouseCatalogFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/warehouse/catalog/datasets")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(datasetPayload("ads_order_summary"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.datasetKey").isEqualTo("ads_order_summary")
                .jsonPath("$.data.freshnessSlaMinutes").isEqualTo(30);

        assertThat(facade.lastTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.lastDatasetCommand.datasetKey()).isEqualTo("ads_order_summary");
        assertThat(facade.lastDatasetCommand.physicalName()).isEqualTo("warehouse.ads_order_summary");
        assertThat(facade.lastDatasetCommand.status()).isEqualTo("ACTIVE");

        client.get()
                .uri("/warehouse/catalog/datasets?layer=ADS&status=ACTIVE")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue());

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastLayer).isEqualTo("ADS");
        assertThat(facade.lastStatus).isEqualTo("ACTIVE");

        client.get()
                .uri("/warehouse/catalog/datasets/ads_order_summary/lineage/transitive?direction=UPSTREAM&maxDepth=4")
                .exchange()
                .expectStatus().isOk();

        assertThat(facade.lastDatasetKey).isEqualTo("ads_order_summary");
        assertThat(facade.lastDirection).isEqualTo(Direction.UPSTREAM);
        assertThat(facade.lastMaxDepth).isEqualTo(4);
    }

    @Test
    void badRequestsUseLegacyApi001Envelope() {
        RecordingWarehouseCatalogFacade facade = new RecordingWarehouseCatalogFacade();
        facade.failUpsert = true;

        webClient(facade)
                .post()
                .uri("/warehouse/catalog/datasets")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of())
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("datasetKey is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CdpWarehouseCatalogFacade facade) {
        return WebTestClient.bindToController(new CdpWarehouseCatalogController(facade)).build();
    }

    private static RouteProbe get(String path, String operation) {
        return new RouteProbe("GET", path, operation, Map.of());
    }

    private static RouteProbe post(String path, String operation, Map<String, Object> body) {
        return new RouteProbe("POST", path, operation, body);
    }

    private static Map<String, Object> datasetPayload(String datasetKey) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("datasetKey", datasetKey);
        payload.put("layer", "ADS");
        payload.put("physicalName", "warehouse." + datasetKey);
        payload.put("displayName", "Dataset " + datasetKey);
        payload.put("subjectArea", "Customer");
        payload.put("sourceSystem", "crm");
        payload.put("ownerName", "owner-a");
        payload.put("description", "dataset description");
        payload.put("freshnessSlaMinutes", 30);
        payload.put("piiLevel", "NORMAL");
        payload.put("status", "ACTIVE");
        payload.put("schemaJson", "{\"type\":\"struct\"}");
        return payload;
    }

    private static Map<String, Object> lineagePayload(String upstream, String downstream) {
        return Map.of(
                "upstreamDatasetKey", upstream,
                "downstreamDatasetKey", downstream,
                "transformType", "SQL",
                "transformRef", "sql-1",
                "dependencyType", "HARD",
                "description", "lineage",
                "active", true);
    }

    private record RouteProbe(String method, String path, String operation, Map<String, Object> body) {
        WebTestClient.ResponseSpec exchange(WebTestClient client) {
            if ("GET".equals(method)) {
                return client.get().uri("/warehouse/catalog" + path).exchange();
            }
            return client.post()
                    .uri("/warehouse/catalog" + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .exchange();
        }
    }

    private static final class RecordingWarehouseCatalogFacade implements CdpWarehouseCatalogFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private String lastLayer;
        private String lastStatus;
        private String lastDatasetKey;
        private Direction lastDirection;
        private Integer lastMaxDepth;
        private DatasetCommand lastDatasetCommand;
        private boolean failUpsert;

        @Override
        public List<Map<String, Object>> listDatasets(Long tenantId, String layer, String status) {
            operations.add("listDatasets");
            lastTenantId = tenantId;
            lastLayer = layer;
            lastStatus = status;
            return List.of(Map.of("tenantId", tenantId, "operation", "listDatasets"));
        }

        @Override
        public Map<String, Object> upsertDataset(Long tenantId, DatasetCommand command) {
            operations.add("upsertDataset");
            if (failUpsert) {
                throw new IllegalArgumentException("datasetKey is required");
            }
            lastTenantId = tenantId;
            lastDatasetCommand = command;
            return Map.of(
                    "tenantId", tenantId,
                    "datasetKey", command.datasetKey(),
                    "freshnessSlaMinutes", command.freshnessSlaMinutes());
        }

        @Override
        public Map<String, Object> createLineageEdge(Long tenantId, LineageCommand command) {
            operations.add("createLineageEdge");
            lastTenantId = tenantId;
            return Map.of(
                    "tenantId", tenantId,
                    "upstreamDatasetKey", command.upstreamDatasetKey(),
                    "downstreamDatasetKey", command.downstreamDatasetKey());
        }

        @Override
        public Map<String, Object> lineage(Long tenantId, String datasetKey, Direction direction) {
            operations.add("lineage");
            lastTenantId = tenantId;
            lastDatasetKey = datasetKey;
            lastDirection = direction;
            return Map.of("tenantId", tenantId, "datasetKey", datasetKey, "direction", direction.name());
        }

        @Override
        public Map<String, Object> transitiveLineage(
                Long tenantId,
                String datasetKey,
                Direction direction,
                Integer maxDepth) {
            operations.add("transitiveLineage");
            lastTenantId = tenantId;
            lastDatasetKey = datasetKey;
            lastDirection = direction;
            lastMaxDepth = maxDepth;
            return Map.of("tenantId", tenantId, "datasetKey", datasetKey, "maxDepth", maxDepth);
        }
    }
}
