package org.chovy.canvas.web.marketing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MauticInsightFacade;
import org.chovy.canvas.marketing.application.MauticInsightApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class MauticInsightControllerCompatibilityTest {

    @Test
    void exposesSixLegacyReadOnlyRouteShapesThroughFinalController() {
        RecordingMauticInsightFacade facade = new RecordingMauticInsightFacade();
        WebTestClient client = webClient(facade);

        List<RouteProbe> probes = List.of(
                get("/audience-membership?audienceId=1001&userId=user-1", "audienceMembership"),
                get("/journey-path?executionId=exec-100", "journeyPath"),
                get("/channel-preference?userId=user-1&preferredChannel=sms", "channelPreference"),
                get("/suppression-timeline?userId=user-1", "suppressionTimeline"),
                get("/publish-health?canvasId=3001", "publishHealth"),
                get("/frequency-templates", "frequencyTemplates"));

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
    void forwardsQueryParametersAndAppliesChannelDefault() {
        RecordingMauticInsightFacade facade = new RecordingMauticInsightFacade();

        webClient(facade)
                .get()
                .uri("/canvas/mautic-insights/audience-membership?audienceId=1001&userId=user-1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.audienceId").isEqualTo(1001)
                .jsonPath("$.data.userId").isEqualTo("user-1");

        assertThat(facade.lastAudienceId).isEqualTo(1001L);
        assertThat(facade.lastUserId).isEqualTo("user-1");

        webClient(facade)
                .get()
                .uri("/canvas/mautic-insights/channel-preference?userId=user-2")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.userId").isEqualTo("user-2")
                .jsonPath("$.data.requestedPreferredChannel").isEqualTo("EMAIL");

        assertThat(facade.lastUserId).isEqualTo("user-2");
        assertThat(facade.lastPreferredChannel).isEqualTo("EMAIL");
    }

    @Test
    void payloadsAreDeterministicEnoughForCompatibilityClients() {
        WebTestClient client = webClient(new MauticInsightApplicationService());

        client.get()
                .uri("/canvas/mautic-insights/journey-path?executionId=exec-100")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.executionId").isEqualTo("exec-100")
                .jsonPath("$.data.successCount").isEqualTo(2)
                .jsonPath("$.data.steps[0].nodeId").isEqualTo("start-1")
                .jsonPath("$.data.steps[1].statusLabel").isEqualTo("SUCCESS")
                .jsonPath("$.data.steps[2].statusLabel").isEqualTo("FAILED");

        client.get()
                .uri("/canvas/mautic-insights/frequency-templates")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].templateKey").isEqualTo("global_weekly_guard")
                .jsonPath("$.data[3].scope").isEqualTo("NODE");
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingMauticInsightFacade facade = new RecordingMauticInsightFacade();
        facade.failJourneyPath = true;

        webClient(facade)
                .get()
                .uri("/canvas/mautic-insights/journey-path?executionId=missing")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("executionId is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(MauticInsightFacade facade) {
        return WebTestClient.bindToController(new MauticInsightController(facade)).build();
    }

    private static RouteProbe get(String path, String operation) {
        return new RouteProbe(path, operation);
    }

    private record RouteProbe(String path, String operation) {
        WebTestClient.ResponseSpec exchange(WebTestClient client) {
            return client.get().uri("/canvas/mautic-insights" + path).exchange();
        }
    }

    private static final class RecordingMauticInsightFacade implements MauticInsightFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastAudienceId;
        private Long lastCanvasId;
        private String lastExecutionId;
        private String lastUserId;
        private String lastPreferredChannel;
        private boolean failJourneyPath;

        @Override
        public Map<String, Object> audienceMembership(Long audienceId, String userId) {
            operations.add("audienceMembership");
            lastAudienceId = audienceId;
            lastUserId = userId;
            return ordered("audienceId", audienceId, "userId", userId);
        }

        @Override
        public Map<String, Object> journeyPath(String executionId) {
            operations.add("journeyPath");
            if (failJourneyPath) {
                throw new IllegalArgumentException("executionId is required");
            }
            lastExecutionId = executionId;
            return ordered("executionId", executionId);
        }

        @Override
        public Map<String, Object> channelPreference(String userId, String preferredChannel) {
            operations.add("channelPreference");
            lastUserId = userId;
            lastPreferredChannel = preferredChannel;
            return ordered("userId", userId, "requestedPreferredChannel", preferredChannel);
        }

        @Override
        public Map<String, Object> suppressionTimeline(String userId) {
            operations.add("suppressionTimeline");
            lastUserId = userId;
            return ordered("userId", userId, "records", List.of());
        }

        @Override
        public Map<String, Object> publishHealth(Long canvasId) {
            operations.add("publishHealth");
            lastCanvasId = canvasId;
            return ordered("canvasId", canvasId);
        }

        @Override
        public List<Map<String, Object>> frequencyTemplates() {
            operations.add("frequencyTemplates");
            return List.of(ordered("templateKey", "global_weekly_guard"));
        }

        private static Map<String, Object> ordered(Object... pairs) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (int i = 0; i < pairs.length; i += 2) {
                result.put(String.valueOf(pairs[i]), pairs[i + 1]);
            }
            return result;
        }
    }
}
