package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseMetricChangeReviewFacade;
import org.chovy.canvas.cdp.api.CdpWarehouseMetricChangeReviewFacade.MetricChangeCommand;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpWarehouseMetricChangeReviewControllerCompatibilityTest {

    @Test
    void exposesLegacyWarehouseMetricChangeReviewRoutesWithCompatibilityEnvelope() {
        RecordingFacade facade = new RecordingFacade();
        WebTestClient client = webClient(facade);

        List<RouteProbe> probes = List.of(
                get("?datasetKey=dwd_user_profile&metricKey=profile_completeness&status=PENDING_REVIEW", "list"),
                post("", "create", createPayload()),
                post("/1/approve", "approve", Map.of("reviewNote", "approved")),
                post("/1/reject", "reject", Map.of("reviewNote", "rejected")),
                post("/1/apply", "apply", Map.of()));

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
    void mapsTenantDefaultHeaderQueryParametersPathVariablesAndRequestBodies() {
        RecordingFacade facade = new RecordingFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/warehouse/metric-change-reviews")
                .header("X-Tenant-Id", "42")
                .header("X-Username", "alice")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createPayload())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(42)
                .jsonPath("$.data.requestedBy").isEqualTo("alice");

        assertThat(facade.lastTenantId).isEqualTo(42L);
        assertThat(facade.lastUsername).isEqualTo("alice");
        assertThat(facade.lastCommand.datasetKey()).isEqualTo("dwd_user_profile");
        assertThat(facade.lastCommand.metricKey()).isEqualTo("profile_completeness");
        assertThat(facade.lastCommand.proposedAllowedDimensions()).containsExactly("country", "channel");

        client.get()
                .uri("/warehouse/metric-change-reviews?datasetKey=dwd_user_profile&metricKey=profile_completeness&status=pending_review")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].tenantId").isEqualTo(0)
                .jsonPath("$.data[0].requestedBy").isEqualTo("system");

        assertThat(facade.lastTenantId).isEqualTo(0L);
        assertThat(facade.lastDatasetKey).isEqualTo("dwd_user_profile");
        assertThat(facade.lastMetricKey).isEqualTo("profile_completeness");
        assertThat(facade.lastStatus).isEqualTo("pending_review");

        client.post()
                .uri("/warehouse/metric-change-reviews/99/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("reviewNote", "approved"))
                .exchange()
                .expectStatus().isOk();

        assertThat(facade.lastReviewId).isEqualTo(99L);
        assertThat(facade.lastUsername).isEqualTo("system");
        assertThat(facade.lastReviewNote).isEqualTo("approved");
    }

    @Test
    void mapsBadRequestsAndConflictsToLegacyErrorEnvelopes() {
        RecordingFacade facade = new RecordingFacade();
        WebTestClient client = webClient(facade);

        facade.failCreate = new IllegalArgumentException("datasetKey is required");
        client.post()
                .uri("/warehouse/metric-change-reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("datasetKey is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        facade.failCreate = new IllegalStateException("open metric change review already exists");
        client.post()
                .uri("/warehouse/metric-change-reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createPayload())
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo(409)
                .jsonPath("$.errorCode").isEqualTo("API_004")
                .jsonPath("$.message").isEqualTo("open metric change review already exists")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CdpWarehouseMetricChangeReviewFacade facade) {
        return WebTestClient.bindToController(new CdpWarehouseMetricChangeReviewController(facade)).build();
    }

    private static RouteProbe get(String path, String operation) {
        return new RouteProbe("GET", path, operation, Map.of());
    }

    private static RouteProbe post(String path, String operation, Map<String, Object> body) {
        return new RouteProbe("POST", path, operation, body);
    }

    private static Map<String, Object> createPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("datasetKey", "dwd_user_profile");
        payload.put("metricKey", "profile_completeness");
        payload.put("proposedExpression", "count_if(profile_complete)");
        payload.put("proposedAllowedDimensions", List.of("country", "channel"));
        payload.put("reason", "better completeness signal");
        return payload;
    }

    private record RouteProbe(String method, String path, String operation, Map<String, Object> body) {
        WebTestClient.ResponseSpec exchange(WebTestClient client) {
            if ("GET".equals(method)) {
                return client.get().uri("/warehouse/metric-change-reviews" + path).exchange();
            }
            return client.post()
                    .uri("/warehouse/metric-change-reviews" + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .exchange();
        }
    }

    private static final class RecordingFacade implements CdpWarehouseMetricChangeReviewFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private String lastUsername;
        private String lastDatasetKey;
        private String lastMetricKey;
        private String lastStatus;
        private Long lastReviewId;
        private String lastReviewNote;
        private MetricChangeCommand lastCommand;
        private RuntimeException failCreate;

        @Override
        public List<Map<String, Object>> list(Long tenantId, String datasetKey, String metricKey, String status) {
            operations.add("list");
            lastTenantId = tenantId;
            lastDatasetKey = datasetKey;
            lastMetricKey = metricKey;
            lastStatus = status;
            return List.of(Map.of(
                    "tenantId", tenantId,
                    "requestedBy", "system",
                    "operation", "list"));
        }

        @Override
        public Map<String, Object> create(Long tenantId, String username, MetricChangeCommand command) {
            operations.add("create");
            if (failCreate != null) {
                throw failCreate;
            }
            lastTenantId = tenantId;
            lastUsername = username;
            lastCommand = command;
            return Map.of("tenantId", tenantId, "requestedBy", username, "id", 1L);
        }

        @Override
        public Map<String, Object> approve(Long tenantId, String username, Long reviewId, String reviewNote) {
            operations.add("approve");
            lastTenantId = tenantId;
            lastUsername = username;
            lastReviewId = reviewId;
            lastReviewNote = reviewNote;
            return Map.of("id", reviewId, "status", "APPROVED");
        }

        @Override
        public Map<String, Object> reject(Long tenantId, String username, Long reviewId, String reviewNote) {
            operations.add("reject");
            lastTenantId = tenantId;
            lastUsername = username;
            lastReviewId = reviewId;
            lastReviewNote = reviewNote;
            return Map.of("id", reviewId, "status", "REJECTED");
        }

        @Override
        public Map<String, Object> apply(Long tenantId, String username, Long reviewId) {
            operations.add("apply");
            lastTenantId = tenantId;
            lastUsername = username;
            lastReviewId = reviewId;
            return Map.of("id", reviewId, "status", "APPLIED");
        }
    }
}
