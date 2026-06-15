package org.chovy.canvas.web.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.execution.api.MqTriggerRejectedFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class MqTriggerRejectedControllerCompatibilityTest {

    @Test
    void mapsLegacyListDetailAndReplayRoutesWithCompatibilityEnvelope() {
        RecordingMqTriggerRejectedFacade facade = new RecordingMqTriggerRejectedFacade();
        WebTestClient client = webClient(facade);

        client.get()
                .uri("/canvas/mq-trigger-rejected?tag=signup-topic&reason=NO_ROUTE&page=2&size=5")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.total").isEqualTo(1)
                .jsonPath("$.data.list[0].id").isEqualTo(1001);

        client.get()
                .uri("/canvas/mq-trigger-rejected/1001")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.tag").isEqualTo("signup-topic");

        client.post()
                .uri("/canvas/mq-trigger-rejected/1001/replay")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.count").isEqualTo(1)
                .jsonPath("$.data.requestIds[0]").isEqualTo("mq-replay-42-msg-1");

        assertThat(facade.operations).containsExactly("list", "detail", "replay");
        assertThat(facade.lastQuery).isEqualTo(new MqTriggerRejectedFacade.RejectedQuery(
                "signup-topic", "NO_ROUTE", 2, 5));
        assertThat(facade.lastDetailId).isEqualTo(1001L);
        assertThat(facade.lastReplayId).isEqualTo(1001L);
    }

    @Test
    void omittedPagingUsesLegacyDefaults() {
        RecordingMqTriggerRejectedFacade facade = new RecordingMqTriggerRejectedFacade();

        webClient(facade).get()
                .uri("/canvas/mq-trigger-rejected")
                .exchange()
                .expectStatus().isOk();

        assertThat(facade.lastQuery).isEqualTo(new MqTriggerRejectedFacade.RejectedQuery(null, null, 1, 20));
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingMqTriggerRejectedFacade facade = new RecordingMqTriggerRejectedFacade();
        facade.failDetail = true;

        webClient(facade).get()
                .uri("/canvas/mq-trigger-rejected/9999")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("rejected 消息不存在: 9999")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(MqTriggerRejectedFacade facade) {
        return WebTestClient.bindToController(new MqTriggerRejectedController(facade)).build();
    }

    private static final class RecordingMqTriggerRejectedFacade implements MqTriggerRejectedFacade {
        private final List<String> operations = new ArrayList<>();
        private RejectedQuery lastQuery;
        private Long lastDetailId;
        private Long lastReplayId;
        private boolean failDetail;

        @Override
        public RejectedPageView list(RejectedQuery query) {
            operations.add("list");
            lastQuery = query;
            return new RejectedPageView(1, query.page(), query.size(), List.of(rejected(1001L)));
        }

        @Override
        public RejectedView detail(Long id) {
            operations.add("detail");
            lastDetailId = id;
            if (failDetail) {
                throw new IllegalArgumentException("rejected 消息不存在: " + id);
            }
            return rejected(id);
        }

        @Override
        public ReplayResult replay(Long id) {
            operations.add("replay");
            lastReplayId = id;
            return new ReplayResult(1, List.of("mq-replay-42-msg-1"), 0, List.of());
        }

        private static RejectedView rejected(Long id) {
            return new RejectedView(id, "signup-topic", "msg-1", "NO_ROUTE",
                    Map.of("userId", "user-1", "messageCode", "signup.created",
                            "payload", Map.of("source", "test")),
                    "2026-06-14T10:00:00");
        }
    }
}
