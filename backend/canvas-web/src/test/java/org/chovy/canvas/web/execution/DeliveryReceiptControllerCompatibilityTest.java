package org.chovy.canvas.web.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.execution.api.DeliveryReceiptFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class DeliveryReceiptControllerCompatibilityTest {

    @Test
    void receiveRoutePreservesLegacySecretHeaderEnvelopeAndReceiptCommand() {
        RecordingDeliveryReceiptFacade facade = new RecordingDeliveryReceiptFacade();

        webClient(facade, "receipt-secret").post()
                .uri("/delivery/receipts")
                .header("X-Canvas-Receipt-Secret", "receipt-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "provider": "twilio",
                          "providerMessageId": "pm-1001",
                          "receiptType": "DELIVERED",
                          "idempotencyKey": "receipt-1001",
                          "receivedAt": "2026-06-15T08:57:00",
                          "rawPayload": {"status": "delivered", "segment": 2}
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
                .jsonPath("$.data.id").isEqualTo(5009)
                .jsonPath("$.data.provider").isEqualTo("twilio")
                .jsonPath("$.data.providerMessageId").isEqualTo("pm-1001")
                .jsonPath("$.data.receiptType").isEqualTo("DELIVERED")
                .jsonPath("$.data.rawPayloadJson").isEqualTo("{\"status\":\"delivered\",\"segment\":2}");

        assertThat(facade.commands).containsExactly(new DeliveryReceiptFacade.ReceiptCommand(
                "twilio",
                "pm-1001",
                "DELIVERED",
                "receipt-1001",
                LocalDateTime.parse("2026-06-15T08:57:00"),
                Map.of("status", "delivered", "segment", 2)));
    }

    @Test
    void receiveRouteRejectsMissingRequiredProviderWithLegacyErrorEnvelope() {
        RecordingDeliveryReceiptFacade facade = new RecordingDeliveryReceiptFacade();

        webClient(facade, "receipt-secret").post()
                .uri("/delivery/receipts")
                .header("X-Canvas-Receipt-Secret", "receipt-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "providerMessageId": "pm-1001",
                          "receiptType": "DELIVERED"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("provider is required")
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        assertThat(facade.commands).isEmpty();
    }

    @Test
    void receiveRouteRejectsInvalidReceiptSecretWithLegacyErrorEnvelope() {
        RecordingDeliveryReceiptFacade facade = new RecordingDeliveryReceiptFacade();

        webClient(facade, "receipt-secret").post()
                .uri("/delivery/receipts")
                .header("X-Canvas-Receipt-Secret", "wrong")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "provider": "twilio",
                          "providerMessageId": "pm-1001",
                          "receiptType": "DELIVERED"
                        }
                        """)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.code").isEqualTo(403)
                .jsonPath("$.message").isEqualTo("invalid receipt signature")
                .jsonPath("$.errorCode").isEqualTo("API_403")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        assertThat(facade.commands).isEmpty();
    }

    private static WebTestClient webClient(DeliveryReceiptFacade facade, String receiptSecret) {
        return WebTestClient.bindToController(new DeliveryReceiptController(facade, receiptSecret)).build();
    }

    private static final class RecordingDeliveryReceiptFacade implements DeliveryReceiptFacade {
        private final List<ReceiptCommand> commands = new ArrayList<>();

        @Override
        public ReceiptView recordReceipt(ReceiptCommand command) {
            commands.add(command);
            return new ReceiptView(
                    5009L,
                    null,
                    null,
                    command.provider(),
                    command.providerMessageId(),
                    command.receiptType(),
                    "{\"status\":\"delivered\",\"segment\":2}",
                    command.idempotencyKey(),
                    command.receivedAt(),
                    LocalDateTime.parse("2026-06-15T08:57:01"));
        }
    }
}
