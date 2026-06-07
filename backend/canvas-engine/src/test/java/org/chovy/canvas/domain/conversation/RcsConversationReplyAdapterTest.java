package org.chovy.canvas.domain.conversation;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RcsConversationReplyAdapterTest {

    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 6, 6, 12, 15);

    @Test
    void mapsTextReplyToRcsIngressRequest() {
        RcsConversationReplyAdapter adapter = new RcsConversationReplyAdapter();

        ConversationIngressReq req = adapter.toIngress(new RcsConversationReplyPayload(
                        10L,
                        20L,
                        "exec-1",
                        "rcs:+15551234567",
                        "google_rcs",
                        "agent-1",
                        "conversation-1",
                        "rcs-msg-1",
                        "rcs-event-1",
                        "I want pricing",
                        null,
                        null,
                        "PRICING",
                        Map.of("locale", "en-US"),
                        OCCURRED_AT),
                new ConversationAdapterContext(8L, "system"));

        assertThat(req.canvasId()).isEqualTo(10L);
        assertThat(req.versionId()).isEqualTo(20L);
        assertThat(req.executionId()).isEqualTo("exec-1");
        assertThat(req.userId()).isEqualTo("rcs:+15551234567");
        assertThat(req.channel()).isEqualTo("RCS");
        assertThat(req.provider()).isEqualTo("GOOGLE_RCS");
        assertThat(req.externalMessageId()).isEqualTo("rcs-msg-1");
        assertThat(req.eventId()).isEqualTo("rcs-event-1");
        assertThat(req.messageType()).isEqualTo("TEXT");
        assertThat(req.text()).isEqualTo("I want pricing");
        assertThat(req.intent()).isEqualTo("PRICING");
        assertThat(req.attributes()).containsEntry("adapter", "RCS")
                .containsEntry("agentId", "agent-1")
                .containsEntry("conversationId", "conversation-1")
                .containsEntry("locale", "en-US");
        assertThat(req.occurredAt()).isEqualTo(OCCURRED_AT);
    }

    @Test
    void mapsSuggestionReplyToInteractiveMessageTypeAndAttributes() {
        RcsConversationReplyAdapter adapter = new RcsConversationReplyAdapter();

        ConversationIngressReq req = adapter.toIngress(new RcsConversationReplyPayload(
                        10L,
                        20L,
                        "exec-1",
                        "rcs:+15551234567",
                        "rbm",
                        "agent-1",
                        "conversation-2",
                        "rcs-msg-2",
                        "rcs-event-2",
                        null,
                        "book-demo",
                        "Book a demo",
                        "BOOK_DEMO",
                        Map.of(),
                        OCCURRED_AT),
                new ConversationAdapterContext(8L, "system"));

        assertThat(req.channel()).isEqualTo("RCS");
        assertThat(req.provider()).isEqualTo("RBM");
        assertThat(req.messageType()).isEqualTo("INTERACTIVE");
        assertThat(req.text()).isEqualTo("Book a demo");
        assertThat(req.intent()).isEqualTo("BOOK_DEMO");
        assertThat(req.attributes()).containsEntry("adapter", "RCS")
                .containsEntry("agentId", "agent-1")
                .containsEntry("conversationId", "conversation-2")
                .containsEntry("suggestionReplyId", "book-demo")
                .containsEntry("suggestionText", "Book a demo");
    }

    @Test
    void contractSupportConvertsRawRcsPayloadUsingDeclaredPayloadType() {
        ConversationIngressReq req = ConversationAdapterContractSupport.captureRawIngress(
                new RcsConversationReplyAdapter(),
                "rcs",
                Map.ofEntries(
                        Map.entry("canvasId", 10L),
                        Map.entry("versionId", 20L),
                        Map.entry("executionId", "exec-1"),
                        Map.entry("userId", "rcs:+15551234567"),
                        Map.entry("provider", "google_rcs"),
                        Map.entry("agentId", "agent-1"),
                        Map.entry("conversationId", "conversation-1"),
                        Map.entry("externalMessageId", "rcs-msg-1"),
                        Map.entry("eventId", "rcs-event-1"),
                        Map.entry("text", "hello"),
                        Map.entry("intent", "GREETING"),
                        Map.entry("attributes", Map.of("locale", "en-US"))),
                "operator-1");

        assertThat(req.channel()).isEqualTo("RCS");
        assertThat(req.provider()).isEqualTo("GOOGLE_RCS");
        assertThat(req.attributes()).containsEntry("agentId", "agent-1")
                .containsEntry("conversationId", "conversation-1")
                .containsEntry("locale", "en-US");
    }

    @Test
    void rejectsMissingPayload() {
        ConversationAdapterContractSupport.assertRejectsMissingPayload(
                new RcsConversationReplyAdapter(),
                "RCS conversation reply payload is required");
    }
}
