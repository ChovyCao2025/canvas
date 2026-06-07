package org.chovy.canvas.domain.conversation;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WebChatConversationReplyAdapterTest {

    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 6, 6, 11, 45);

    @Test
    void mapsTextReplyToWebChatIngressRequest() {
        WebChatConversationReplyAdapter adapter = new WebChatConversationReplyAdapter();

        ConversationIngressReq req = adapter.toIngress(new WebChatConversationReplyPayload(
                        10L,
                        20L,
                        "exec-1",
                        "visitor-1",
                        "chat-session-1",
                        "widget",
                        "web-msg-1",
                        "web-event-1",
                        "Can I talk to sales?",
                        null,
                        null,
                        "SALES_HANDOFF",
                        Map.of("pageUrl", "https://example.test/pricing"),
                        OCCURRED_AT),
                new ConversationAdapterContext(8L, "system"));

        assertThat(req.canvasId()).isEqualTo(10L);
        assertThat(req.versionId()).isEqualTo(20L);
        assertThat(req.executionId()).isEqualTo("exec-1");
        assertThat(req.userId()).isEqualTo("visitor-1");
        assertThat(req.channel()).isEqualTo("WEB_CHAT");
        assertThat(req.provider()).isEqualTo("WIDGET");
        assertThat(req.externalMessageId()).isEqualTo("web-msg-1");
        assertThat(req.eventId()).isEqualTo("web-event-1");
        assertThat(req.messageType()).isEqualTo("TEXT");
        assertThat(req.text()).isEqualTo("Can I talk to sales?");
        assertThat(req.intent()).isEqualTo("SALES_HANDOFF");
        assertThat(req.attributes()).containsEntry("adapter", "WEB_CHAT")
                .containsEntry("webChatSessionId", "chat-session-1")
                .containsEntry("pageUrl", "https://example.test/pricing");
        assertThat(req.occurredAt()).isEqualTo(OCCURRED_AT);
    }

    @Test
    void mapsActionReplyToInteractiveMessageTypeAndAttributes() {
        WebChatConversationReplyAdapter adapter = new WebChatConversationReplyAdapter();

        ConversationIngressReq req = adapter.toIngress(new WebChatConversationReplyPayload(
                        10L,
                        20L,
                        "exec-1",
                        "visitor-1",
                        "chat-session-1",
                        null,
                        "web-msg-2",
                        "web-event-2",
                        null,
                        "book-demo",
                        "Book a demo",
                        "BOOK_DEMO",
                        Map.of(),
                        OCCURRED_AT),
                new ConversationAdapterContext(8L, "system"));

        assertThat(req.channel()).isEqualTo("WEB_CHAT");
        assertThat(req.provider()).isEqualTo("DEFAULT");
        assertThat(req.messageType()).isEqualTo("INTERACTIVE");
        assertThat(req.text()).isEqualTo("Book a demo");
        assertThat(req.intent()).isEqualTo("BOOK_DEMO");
        assertThat(req.attributes()).containsEntry("adapter", "WEB_CHAT")
                .containsEntry("webChatSessionId", "chat-session-1")
                .containsEntry("actionId", "book-demo")
                .containsEntry("actionLabel", "Book a demo");
    }

    @Test
    void contractSupportConvertsRawWebChatPayloadUsingDeclaredPayloadType() {
        ConversationIngressReq req = ConversationAdapterContractSupport.captureRawIngress(
                new WebChatConversationReplyAdapter(),
                "web_chat",
                Map.of(
                        "canvasId", 10L,
                        "versionId", 20L,
                        "executionId", "exec-1",
                        "userId", "visitor-1",
                        "webChatSessionId", "chat-session-1",
                        "externalMessageId", "web-msg-1",
                        "eventId", "web-event-1",
                        "text", "hello",
                        "intent", "GREETING",
                        "attributes", Map.of("locale", "en-US")),
                "operator-1");

        assertThat(req.channel()).isEqualTo("WEB_CHAT");
        assertThat(req.attributes()).containsEntry("webChatSessionId", "chat-session-1")
                .containsEntry("locale", "en-US");
    }

    @Test
    void rejectsMissingPayload() {
        ConversationAdapterContractSupport.assertRejectsMissingPayload(
                new WebChatConversationReplyAdapter(),
                "web chat conversation reply payload is required");
    }
}
