package org.chovy.canvas.web.canvas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.canvas.api.CanvasStatsFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CanvasStatsControllerCompatibilityTest {

    @Test
    void exposesAllLegacyCanvasStatsRoutesThroughFinalController() {
        RecordingCanvasStatsFacade facade = new RecordingCanvasStatsFacade();
        WebTestClient client = webClient(facade);

        List<RouteProbe> probes = List.of(
                get("/canvas/42/execution/exec-42/trace", "trace"),
                get("/canvas/42/executions?size=5", "recentExecutions"),
                get("/canvas/42/stats?days=7", "stats"),
                get("/canvas/42/funnel", "funnel"),
                get("/canvas/42/trend?since=2026-06-12&until=2026-06-14", "trend"),
                get("/canvas/42/receipts", "receipts"),
                get("/canvas/42/attribution-summary", "attributionSummary"));

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
        assertThat(facade.lastCanvasId).isEqualTo(42L);
        assertThat(facade.lastExecutionId).isEqualTo("exec-42");
        assertThat(facade.lastSize).isEqualTo(5);
        assertThat(facade.lastSince).isEqualTo("2026-06-12");
        assertThat(facade.lastUntil).isEqualTo("2026-06-14");
    }

    @Test
    void routeEnvelopeIncludesCompactGoldenStatsShapes() {
        RecordingCanvasStatsFacade facade = new RecordingCanvasStatsFacade();

        webClient(facade)
                .get()
                .uri("/canvas/42/stats")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.total").isEqualTo(1)
                .jsonPath("$.data.success").isEqualTo(1)
                .jsonPath("$.data.failed").isEqualTo(0)
                .jsonPath("$.data.paused").isEqualTo(0)
                .jsonPath("$.data.successRate").isEqualTo("100.0%")
                .jsonPath("$.data.uniqueUsers").isEqualTo(1);

        webClient(facade)
                .get()
                .uri("/canvas/42/attribution-summary")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.conversions").isEqualTo(0)
                .jsonPath("$.data.conversionAmount").isEqualTo("0")
                .jsonPath("$.data.attributedSends").isEqualTo(0)
                .jsonPath("$.data.model").isEqualTo("LAST_TOUCH")
                .jsonPath("$.data.models").isEqualTo("LAST_TOUCH");
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingCanvasStatsFacade facade = new RecordingCanvasStatsFacade();
        facade.failTrend = true;

        webClient(facade)
                .get()
                .uri("/canvas/42/trend?since=2026-06-15&until=2026-06-14")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("since must be on or before until")
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CanvasStatsFacade facade) {
        return WebTestClient.bindToController(new CanvasStatsController(facade)).build();
    }

    private static RouteProbe get(String path, String operation) {
        return new RouteProbe(path, operation);
    }

    private record RouteProbe(String path, String operation) {
        WebTestClient.ResponseSpec exchange(WebTestClient client) {
            return client.get().uri(path).exchange();
        }
    }

    private static final class RecordingCanvasStatsFacade implements CanvasStatsFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastCanvasId;
        private String lastExecutionId;
        private Integer lastSize;
        private String lastSince;
        private String lastUntil;
        private boolean failTrend;

        @Override
        public List<Map<String, Object>> trace(Long canvasId, String executionId) {
            operations.add("trace");
            lastCanvasId = canvasId;
            lastExecutionId = executionId;
            return List.of(row(
                    "nodeId", "start",
                    "nodeType", "TRIGGER",
                    "nodeName", "Entry",
                    "status", 2,
                    "errorMsg", null,
                    "outputData", "{}",
                    "durationMs", 0L));
        }

        @Override
        public List<Map<String, Object>> recentExecutions(Long canvasId, int size) {
            operations.add("recentExecutions");
            lastCanvasId = canvasId;
            lastSize = size;
            return List.of(row(
                    "id", "exec-42",
                    "triggerType", "MANUAL",
                    "status", 2,
                    "userId", "system",
                    "createdAt", "2026-06-14T00:00:00"));
        }

        @Override
        public Map<String, Object> stats(Long canvasId, int days, String since, String until) {
            operations.add("stats");
            lastCanvasId = canvasId;
            return statsView();
        }

        @Override
        public List<Map<String, Object>> funnel(Long canvasId) {
            operations.add("funnel");
            lastCanvasId = canvasId;
            return List.of(row(
                    "nodeId", "start",
                    "nodeType", "TRIGGER",
                    "nodeName", "Entry",
                    "totalEntered", 1L,
                    "totalSuccess", 1L,
                    "totalFailed", 0L,
                    "totalSkipped", 0L,
                    "avgDurationMs", 0L,
                    "avgDurationSec", 0.0));
        }

        @Override
        public List<Map<String, Object>> trend(Long canvasId, int days, String since, String until) {
            operations.add("trend");
            lastCanvasId = canvasId;
            lastSince = since;
            lastUntil = until;
            if (failTrend) {
                throw new IllegalArgumentException("since must be on or before until");
            }
            return List.of(
                    row("date", "2026-06-12", "count", 0L),
                    row("date", "2026-06-13", "count", 0L),
                    row("date", "2026-06-14", "count", 1L));
        }

        @Override
        public Map<String, Object> receipts(Long canvasId) {
            operations.add("receipts");
            lastCanvasId = canvasId;
            return row("delivered", 1L, "failed", 0L);
        }

        @Override
        public Map<String, Object> attributionSummary(Long canvasId) {
            operations.add("attributionSummary");
            lastCanvasId = canvasId;
            return row(
                    "conversions", 0L,
                    "conversionAmount", "0",
                    "attributedSends", 0L,
                    "model", "LAST_TOUCH",
                    "models", "LAST_TOUCH");
        }

        private static Map<String, Object> statsView() {
            return row(
                    "total", 1L,
                    "success", 1L,
                    "failed", 0L,
                    "paused", 0L,
                    "successRate", "100.0%",
                    "uniqueUsers", 1L);
        }

        private static Map<String, Object> row(Object... keysAndValues) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 0; i < keysAndValues.length; i += 2) {
                row.put((String) keysAndValues[i], keysAndValues[i + 1]);
            }
            return row;
        }
    }
}
