package org.chovy.canvas.web.canvas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.chovy.canvas.canvas.api.CanvasTriggerFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CanvasTriggerControllerCompatibilityTest {

    @Test
    void behaviorTriggerRouteParsesRawJsonAndReturnsLegacyEnvelope() {
        RecordingCanvasTriggerFacade facade = new RecordingCanvasTriggerFacade();

        webClient(facade)
                .post()
                .uri("/canvas/trigger/behavior")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "canvasId": 42,
                          "userId": "user-1",
                          "eventCode": "ORDER_PAID",
                          "eventId": "evt-1",
                          "behaviorData": {
                            "orderId": "order-9",
                            "amount": 88.5
                          }
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.accepted").isEqualTo(true)
                .jsonPath("$.data.canvasId").isEqualTo(42)
                .jsonPath("$.data.userId").isEqualTo("user-1")
                .jsonPath("$.data.eventCode").isEqualTo("ORDER_PAID")
                .jsonPath("$.data.eventId").isEqualTo("evt-1")
                .jsonPath("$.data.behaviorData.orderId").isEqualTo("order-9");

        assertThat(facade.lastCommand.canvasId()).isEqualTo(42L);
        assertThat(facade.lastCommand.userId()).isEqualTo("user-1");
        assertThat(facade.lastCommand.eventCode()).isEqualTo("ORDER_PAID");
        assertThat(facade.lastCommand.eventId()).isEqualTo("evt-1");
        assertThat(facade.lastCommand.behaviorData()).containsEntry("orderId", "order-9");
    }

    @Test
    void behaviorTriggerRejectsMissingRequiredFieldWithApi001Envelope() {
        RecordingCanvasTriggerFacade facade = new RecordingCanvasTriggerFacade();

        webClient(facade)
                .post()
                .uri("/canvas/trigger/behavior")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "canvasId": 42,
                          "eventCode": "ORDER_PAID",
                          "eventId": "evt-1"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("userId is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CanvasTriggerFacade facade) {
        return WebTestClient.bindToController(new CanvasTriggerController(facade)).build();
    }

    private static final class RecordingCanvasTriggerFacade implements CanvasTriggerFacade {
        private BehaviorTriggerCommand lastCommand;

        @Override
        public BehaviorTriggerResult triggerBehavior(BehaviorTriggerCommand command) {
            lastCommand = command;
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("accepted", true);
            data.put("canvasId", command.canvasId());
            data.put("userId", command.userId());
            data.put("eventCode", command.eventCode());
            data.put("eventId", command.eventId());
            data.put("behaviorData", command.behaviorData());
            return new BehaviorTriggerResult(data);
        }
    }
}
