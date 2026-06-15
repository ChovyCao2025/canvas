package org.chovy.canvas.web.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.bi.api.AnalyticsViews.EventCountView;
import org.chovy.canvas.bi.application.AnalyticsApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class AnalyticsControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;

    @Test
    void exposesAllLegacyAnalyticsRoutesWithCompatibilityEnvelope() {
        WebTestClient client = webClient(new AnalyticsApplicationService());

        List<RouteProbe> probes = List.of(
                get("/events/counts?startDate=2026-06-01&endDate=2026-06-07"),
                get("/events?startDate=2026-06-01&endDate=2026-06-07"),
                get("/events/count?startDate=2026-06-01&endDate=2026-06-07&eventCode=purchase"),
                get("/users/user-42/timeline?startDate=2026-06-01&endDate=2026-06-07&page=1&size=2"),
                get("/events/attributes/channel/distribution?startDate=2026-06-01&endDate=2026-06-07"),
                get("/attributes/channel/distribution?startDate=2026-06-01&endDate=2026-06-07"),
                get("/funnels/signup-to-purchase?startDate=2026-06-01&endDate=2026-06-07"),
                post("/alerts/preview", Map.of(
                        "ruleKey", "purchase-spike",
                        "eventCode", "purchase",
                        "startDate", "2026-06-01",
                        "endDate", "2026-06-07",
                        "threshold", 80)),
                post("/exports", Map.of(
                        "reportType", "events",
                        "eventCode", "purchase",
                        "startDate", "2026-06-01",
                        "endDate", "2026-06-07",
                        "rowLimit", 1000,
                        "createdBy", "analyst")),
                get("/exports/9001"));

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
    void missingTenantHeaderDefaultsToSevenAndBadRequestsMapToApi001() {
        RecordingAnalyticsFacade facade = new RecordingAnalyticsFacade();

        webClient(facade)
                .get()
                .uri("/analytics/events/counts?startDate=2026-06-01&endDate=2026-06-07")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data[0].tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue());

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);

        facade.failEventCounts = true;

        webClient(facade)
                .get()
                .uri("/analytics/events/counts?startDate=&endDate=2026-06-07")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("startDate is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(AnalyticsApplicationService service) {
        return WebTestClient.bindToController(new AnalyticsController(service)).build();
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
                return client.get().uri("/analytics" + path).exchange();
            }
            return client.post()
                    .uri("/analytics" + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .exchange();
        }
    }

    private static final class RecordingAnalyticsFacade extends AnalyticsApplicationService {
        private Long lastTenantId;
        private boolean failEventCounts;

        @Override
        public List<EventCountView> eventCounts(Long tenantId, String startDate, String endDate) {
            if (failEventCounts) {
                throw new IllegalArgumentException("startDate is required");
            }
            lastTenantId = tenantId;
            return List.of(new EventCountView(tenantId, "purchase", 96));
        }
    }
}
