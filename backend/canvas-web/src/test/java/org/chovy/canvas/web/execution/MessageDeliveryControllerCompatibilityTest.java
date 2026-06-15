package org.chovy.canvas.web.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.chovy.canvas.execution.api.MessageDeliveryFacade;
import org.chovy.canvas.execution.api.MessageDeliveryFacade.DeliveryPageView;
import org.chovy.canvas.execution.api.MessageDeliveryFacade.DeliverySearchQuery;
import org.chovy.canvas.execution.api.MessageDeliveryFacade.ReconcileResultView;
import org.chovy.canvas.execution.api.MessageDeliveryFacade.ReplayResultView;
import org.chovy.canvas.execution.domain.MessageDeliveryCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class MessageDeliveryControllerCompatibilityTest {

    @Test
    void listRoutePreservesLegacyFilterPageContractAndEnvelope() {
        RecordingMessageDeliveryFacade facade = new RecordingMessageDeliveryFacade();

        webClient(facade).get()
                .uri("/message-deliveries?tenantId=9&canvasId=42&executionId=exec-1&userId=user-1"
                        + "&channel=SMS&provider=twilio&status=DEAD&providerMessageId=pm-1&page=2&size=1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.total").isEqualTo(2)
                .jsonPath("$.data.list[0].id").isEqualTo(1002)
                .jsonPath("$.data.list[0].providerMessageId").isEqualTo("pm-1");

        assertThat(facade.queries).containsExactly(new DeliverySearchQuery(
                9L, 42L, "exec-1", "user-1", "SMS", "twilio", "DEAD", "pm-1", 2, 1));
    }

    @Test
    void listRouteUsesLegacyPageDefaults() {
        RecordingMessageDeliveryFacade facade = new RecordingMessageDeliveryFacade();

        webClient(facade).get().uri("/message-deliveries")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.total").isEqualTo(2);

        assertThat(facade.queries).containsExactly(new DeliverySearchQuery(
                null, null, null, null, null, null, null, null, 1, 20));
    }

    @Test
    void detailMissingReturnsLegacyFailureEnvelopeMessage() {
        RecordingMessageDeliveryFacade facade = new RecordingMessageDeliveryFacade();

        webClient(facade).get().uri("/message-deliveries/9999")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(1)
                .jsonPath("$.message").isEqualTo("message delivery not found: 9999")
                .jsonPath("$.data").doesNotExist();
    }

    @Test
    void receiptsReplayAndReconcileRoutesPreserveLegacyShapes() {
        RecordingMessageDeliveryFacade facade = new RecordingMessageDeliveryFacade();
        WebTestClient client = webClient(facade);

        client.get().uri("/message-deliveries/1001/receipts")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].id").isEqualTo(5002)
                .jsonPath("$.data[0].receiptType").isEqualTo("DELIVERED")
                .jsonPath("$.data[1].id").isEqualTo(5001)
                .jsonPath("$.data[1].receiptType").isEqualTo("SENT");

        client.post().uri("/message-deliveries/1002/replay")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.outboxId").isEqualTo(1002)
                .jsonPath("$.data.status").isEqualTo("PENDING");

        client.post().uri("/message-deliveries/1001/replay")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(1)
                .jsonPath("$.message").isEqualTo("delivery is not replayable: 1001")
                .jsonPath("$.data").doesNotExist();

        client.post().uri("/message-deliveries/reconcile")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.requeued").isEqualTo(1);
    }

    private static WebTestClient webClient(MessageDeliveryFacade facade) {
        return WebTestClient.bindToController(new MessageDeliveryController(facade)).build();
    }

    private static final class RecordingMessageDeliveryFacade implements MessageDeliveryFacade {
        private final List<DeliverySearchQuery> queries = new ArrayList<>();

        @Override
        public DeliveryPageView search(DeliverySearchQuery query) {
            queries.add(query);
            return new DeliveryPageView(2, List.of(delivery(1002L, "DEAD", "pm-1")));
        }

        @Override
        public Optional<MessageDeliveryCatalog.Delivery> findById(Long id) {
            if (id.equals(1001L)) {
                return Optional.of(delivery(1001L, "SENT", "pm-sent-1"));
            }
            return Optional.empty();
        }

        @Override
        public List<MessageDeliveryCatalog.Receipt> receipts(Long outboxId) {
            return List.of(
                    receipt(5002L, outboxId, "DELIVERED", "2026-06-12T10:03:00"),
                    receipt(5001L, outboxId, "SENT", "2026-06-12T10:02:00"));
        }

        @Override
        public ReplayResultView replay(Long id) {
            return new ReplayResultView(id, "PENDING", id.equals(1002L));
        }

        @Override
        public ReconcileResultView reconcile() {
            return new ReconcileResultView(1);
        }

        private static MessageDeliveryCatalog.Delivery delivery(Long id, String status, String providerMessageId) {
            return new MessageDeliveryCatalog.Delivery(
                    id,
                    9L,
                    88L,
                    "exec-1",
                    42L,
                    "user-1",
                    "message",
                    "SMS",
                    "twilio",
                    "{\"body\":\"hello\"}",
                    "idem-" + id,
                    status,
                    1,
                    null,
                    null,
                    null,
                    providerMessageId,
                    "{\"ok\":true}",
                    "",
                    LocalDateTime.parse("2026-06-12T10:00:00"),
                    LocalDateTime.parse("2026-06-12T10:04:00"),
                    false);
        }

        private static MessageDeliveryCatalog.Receipt receipt(
                Long id,
                Long outboxId,
                String receiptType,
                String receivedAt) {
            return new MessageDeliveryCatalog.Receipt(
                    id,
                    9L,
                    outboxId,
                    "twilio",
                    "pm-sent-1",
                    receiptType,
                    "{\"type\":\"" + receiptType + "\"}",
                    "receipt-" + id,
                    LocalDateTime.parse(receivedAt),
                    LocalDateTime.parse(receivedAt));
        }
    }
}
