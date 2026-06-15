package org.chovy.canvas.web.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.chovy.canvas.execution.api.MessageSendRecordFacade;
import org.chovy.canvas.execution.api.MessageSendRecordFacade.MessageSendRecordPageView;
import org.chovy.canvas.execution.api.MessageSendRecordFacade.MessageSendRecordQuery;
import org.chovy.canvas.execution.domain.MessageSendRecordCatalog.MessageSendRecord;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class MessageSendRecordControllerCompatibilityTest {

    @Test
    void listRoutePreservesLegacyFiltersPagingEnvelopeAndDateBinding() {
        RecordingMessageSendRecordFacade facade = new RecordingMessageSendRecordFacade();

        webClient(facade).get()
                .uri(builder -> builder.path("/canvas/message-send-records")
                        .queryParam("canvasId", 42)
                        .queryParam("executionId", "exec-1")
                        .queryParam("userId", "user-1")
                        .queryParam("channel", " sms ")
                        .queryParam("status", " sent ")
                        .queryParam("startAt", "2026-06-12T09:00:00")
                        .queryParam("endAt", "2026-06-12T11:00:00")
                        .queryParam("page", 0)
                        .queryParam("size", 500)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.total").isEqualTo(2)
                .jsonPath("$.data.list[0].id").isEqualTo(502)
                .jsonPath("$.data.list[0].requestPayload").isEqualTo("{\"body\":\"new\"}")
                .jsonPath("$.data.list[0].externalMessageId").isEqualTo("pm-502");

        assertThat(facade.queries).containsExactly(new MessageSendRecordQuery(
                42L,
                "exec-1",
                "user-1",
                " sms ",
                " sent ",
                LocalDateTime.parse("2026-06-12T09:00:00"),
                LocalDateTime.parse("2026-06-12T11:00:00"),
                1,
                100));
    }

    @Test
    void detailRouteReturnsRecordShapeAndLegacyMissingFailure() {
        RecordingMessageSendRecordFacade facade = new RecordingMessageSendRecordFacade();
        WebTestClient client = webClient(facade);

        client.get().uri("/canvas/message-send-records/501")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.id").isEqualTo(501)
                .jsonPath("$.data.tenantId").isEqualTo(7)
                .jsonPath("$.data.executionId").isEqualTo("exec-1")
                .jsonPath("$.data.canvasId").isEqualTo(42)
                .jsonPath("$.data.userId").isEqualTo("user-1")
                .jsonPath("$.data.nodeId").isEqualTo("node-message")
                .jsonPath("$.data.channel").isEqualTo("SMS")
                .jsonPath("$.data.templateId").isEqualTo("tpl-1")
                .jsonPath("$.data.idempotencyKey").isEqualTo("idem-501")
                .jsonPath("$.data.status").isEqualTo("SENT")
                .jsonPath("$.data.externalMessageId").isEqualTo("pm-501")
                .jsonPath("$.data.createdAt").isEqualTo("2026-06-12T10:00:00");

        client.get().uri("/canvas/message-send-records/9999")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(-1)
                .jsonPath("$.message").isEqualTo("发送记录不存在: 9999")
                .jsonPath("$.data").doesNotExist();
    }

    private static WebTestClient webClient(MessageSendRecordFacade facade) {
        return WebTestClient.bindToController(new MessageSendRecordController(facade)).build();
    }

    private static final class RecordingMessageSendRecordFacade implements MessageSendRecordFacade {
        private final List<MessageSendRecordQuery> queries = new ArrayList<>();

        @Override
        public MessageSendRecordPageView search(MessageSendRecordQuery query) {
            queries.add(query);
            return new MessageSendRecordPageView(2, List.of(record(502L, "new"), record(501L, "old")));
        }

        @Override
        public Optional<MessageSendRecord> findById(Long id) {
            if (id.equals(501L)) {
                return Optional.of(record(501L, "old"));
            }
            return Optional.empty();
        }

        private static MessageSendRecord record(Long id, String body) {
            return new MessageSendRecord(
                    id,
                    7L,
                    "exec-1",
                    42L,
                    "user-1",
                    "node-message",
                    "SMS",
                    "tpl-1",
                    "idem-" + id,
                    "{\"body\":\"" + body + "\"}",
                    "SENT",
                    "pm-" + id,
                    null,
                    LocalDateTime.parse("2026-06-12T10:00:00"),
                    LocalDateTime.parse("2026-06-12T10:04:00"));
        }
    }
}
