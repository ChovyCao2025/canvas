package org.chovy.canvas.web.canvas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.chovy.canvas.canvas.api.CanvasEventReportFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CanvasEventReportControllerCompatibilityTest {

    @Test
    void reportRoutePreservesLegacyEnvelopeAndDelegatesRawJsonBody() {
        RecordingCanvasEventReportFacade facade = new RecordingCanvasEventReportFacade();

        webClient(facade)
                .post()
                .uri("/canvas/events/report")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "eventCode": "ORDER_PAID",
                          "userId": "user-123",
                          "attributes": {
                            "orderId": "order-9",
                            "amount": 88.5
                          },
                          "idempotencyKey": "idem-123"
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
                .jsonPath("$.data.eventCode").isEqualTo("ORDER_PAID")
                .jsonPath("$.data.userId").isEqualTo("user-123")
                .jsonPath("$.data.accepted").isEqualTo(true)
                .jsonPath("$.data.triggeredCanvasCount").isEqualTo(2)
                .jsonPath("$.data.idempotencyKey").isEqualTo("idem-123");

        assertThat(facade.lastRawBody).contains("\"eventCode\": \"ORDER_PAID\"");
        assertThat(facade.lastRawBody).contains("\"userId\": \"user-123\"");
        assertThat(facade.lastRawBody).contains("\"attributes\"");
        assertThat(facade.lastRawBody).contains("\"idempotencyKey\": \"idem-123\"");
    }

    @Test
    void reportRouteMapsValidationFailureToApi001BadRequestEnvelope() {
        RecordingCanvasEventReportFacade facade = new RecordingCanvasEventReportFacade();

        webClient(facade)
                .post()
                .uri("/canvas/events/report")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "eventCode": "",
                          "userId": "user-123"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("eventCode is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CanvasEventReportFacade facade) {
        return WebTestClient.bindToController(new CanvasEventReportController(facade)).build();
    }

    private static final class RecordingCanvasEventReportFacade implements CanvasEventReportFacade {
        private String lastRawBody;

        @Override
        public Map<String, Object> report(String rawBody) {
            lastRawBody = rawBody;
            if (rawBody.contains("\"eventCode\": \"\"")) {
                throw new IllegalArgumentException("eventCode is required");
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("eventCode", "ORDER_PAID");
            result.put("userId", "user-123");
            result.put("accepted", true);
            result.put("triggeredCanvasCount", 2);
            result.put("idempotencyKey", "idem-123");
            return result;
        }
    }
}
