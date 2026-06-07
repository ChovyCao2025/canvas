package org.chovy.canvas.domain.conversation;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationReplyAdapterSupportTest {

    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 6, 6, 15, 30);

    @Test
    void normalizesProviderToUppercaseDefaultingBlankValues() {
        assertThat(ConversationReplyAdapterSupport.normalizeProvider(" cloud_api ")).isEqualTo("CLOUD_API");
        assertThat(ConversationReplyAdapterSupport.normalizeProvider("")).isEqualTo("DEFAULT");
        assertThat(ConversationReplyAdapterSupport.normalizeProvider(null)).isEqualTo("DEFAULT");
    }

    @Test
    void choosesTrimmedPrimaryTextBeforeFallbackText() {
        assertThat(ConversationReplyAdapterSupport.firstText(" hello ", "fallback")).isEqualTo("hello");
        assertThat(ConversationReplyAdapterSupport.firstText(" ", " fallback ")).isEqualTo("fallback");
        assertThat(ConversationReplyAdapterSupport.firstText(null, " ")).isNull();
    }

    @Test
    void buildsAdapterAttributesWithTrimmedOptionalStringFields() {
        Map<String, Object> attributes = ConversationReplyAdapterSupport.adapterAttributes(
                "WEB_CHAT",
                Map.of("locale", "en-US"),
                Map.entry("webChatSessionId", " session-1 "),
                Map.entry("actionId", " "),
                Map.entry("actionLabel", "Book a demo"));

        assertThat(attributes).containsExactlyInAnyOrderEntriesOf(Map.of(
                "adapter", "WEB_CHAT",
                "locale", "en-US",
                "webChatSessionId", "session-1",
                "actionLabel", "Book a demo"));
    }

    @Test
    void buildsProviderIngressRequestFromCommonPayloadFields() {
        ProviderConversationReplyPayload payload = new TestProviderPayload(
                10L,
                20L,
                "exec-1",
                "user-1",
                " cloud_api ",
                "external-1",
                "event-1",
                " ",
                "BOOK_DEMO",
                Map.of("adapter", "TEST"),
                OCCURRED_AT);

        ConversationIngressReq req = ConversationReplyAdapterSupport.providerIngress(
                payload,
                "TEST",
                true,
                "Book a demo",
                Map.of("adapter", "TEST", "replyId", "book-demo"));

        assertThat(req.canvasId()).isEqualTo(10L);
        assertThat(req.versionId()).isEqualTo(20L);
        assertThat(req.executionId()).isEqualTo("exec-1");
        assertThat(req.userId()).isEqualTo("user-1");
        assertThat(req.channel()).isEqualTo("TEST");
        assertThat(req.provider()).isEqualTo("CLOUD_API");
        assertThat(req.externalMessageId()).isEqualTo("external-1");
        assertThat(req.eventId()).isEqualTo("event-1");
        assertThat(req.messageType()).isEqualTo("INTERACTIVE");
        assertThat(req.text()).isEqualTo("Book a demo");
        assertThat(req.intent()).isEqualTo("BOOK_DEMO");
        assertThat(req.attributes()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "adapter", "TEST",
                "replyId", "book-demo"));
        assertThat(req.occurredAt()).isEqualTo(OCCURRED_AT);
    }

    @Test
    void buildsProviderIngressRequestFromInteractiveMarkers() {
        ProviderConversationReplyPayload payload = new TestProviderPayload(
                10L,
                20L,
                "exec-1",
                "user-1",
                "cloud_api",
                "external-1",
                "event-1",
                " ",
                "BOOK_DEMO",
                Map.of(),
                OCCURRED_AT);

        ConversationIngressReq req = ConversationReplyAdapterSupport.providerIngress(
                payload,
                "TEST",
                "Book a demo",
                Map.of("adapter", "TEST"),
                " ",
                "Book a demo");

        assertThat(req.messageType()).isEqualTo("INTERACTIVE");
        assertThat(req.text()).isEqualTo("Book a demo");
    }

    private record TestProviderPayload(
            Long canvasId,
            Long versionId,
            String executionId,
            String userId,
            String provider,
            String externalMessageId,
            String eventId,
            String text,
            String intent,
            Map<String, Object> attributes,
            LocalDateTime occurredAt) implements ProviderConversationReplyPayload {
    }
}
