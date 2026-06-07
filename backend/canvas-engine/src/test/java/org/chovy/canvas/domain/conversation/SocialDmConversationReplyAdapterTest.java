package org.chovy.canvas.domain.conversation;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

class SocialDmConversationReplyAdapterTest {

    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 6, 6, 12, 15);

    @Test
    void mapsTextReplyToSocialDmIngressRequest() {
        SocialDmConversationReplyAdapter adapter = new SocialDmConversationReplyAdapter();

        ConversationIngressReq req = adapter.toIngress(new SocialDmConversationReplyPayload(
                        10L,
                        20L,
                        "exec-1",
                        "instagram:user-1",
                        "instagram",
                        "meta",
                        "page-1",
                        "thread-1",
                        "dm-msg-1",
                        "dm-event-1",
                        "I want the coupon",
                        null,
                        null,
                        "COUPON",
                        Map.of("locale", "en-US"),
                        OCCURRED_AT),
                new ConversationAdapterContext(8L, "system"));

        assertThat(req.canvasId()).isEqualTo(10L);
        assertThat(req.versionId()).isEqualTo(20L);
        assertThat(req.executionId()).isEqualTo("exec-1");
        assertThat(req.userId()).isEqualTo("instagram:user-1");
        assertThat(req.channel()).isEqualTo("SOCIAL_DM");
        assertThat(req.provider()).isEqualTo("META");
        assertThat(req.externalMessageId()).isEqualTo("dm-msg-1");
        assertThat(req.eventId()).isEqualTo("dm-event-1");
        assertThat(req.messageType()).isEqualTo("TEXT");
        assertThat(req.text()).isEqualTo("I want the coupon");
        assertThat(req.intent()).isEqualTo("COUPON");
        assertThat(req.attributes()).containsEntry("adapter", "SOCIAL_DM")
                .containsEntry("platform", "instagram")
                .containsEntry("pageId", "page-1")
                .containsEntry("threadId", "thread-1")
                .containsEntry("locale", "en-US");
        assertThat(req.occurredAt()).isEqualTo(OCCURRED_AT);
    }

    @Test
    void mapsQuickReplyToInteractiveMessageTypeAndAttributes() {
        SocialDmConversationReplyAdapter adapter = new SocialDmConversationReplyAdapter();

        ConversationIngressReq req = adapter.toIngress(new SocialDmConversationReplyPayload(
                        10L,
                        20L,
                        "exec-1",
                        "facebook:user-1",
                        "facebook",
                        null,
                        "page-1",
                        "thread-1",
                        "dm-msg-2",
                        "dm-event-2",
                        null,
                        "product-a",
                        "Product A",
                        "PRODUCT_A",
                        Map.of(),
                        OCCURRED_AT),
                new ConversationAdapterContext(8L, "system"));

        assertThat(req.channel()).isEqualTo("SOCIAL_DM");
        assertThat(req.provider()).isEqualTo("DEFAULT");
        assertThat(req.messageType()).isEqualTo("INTERACTIVE");
        assertThat(req.text()).isEqualTo("Product A");
        assertThat(req.intent()).isEqualTo("PRODUCT_A");
        assertThat(req.attributes()).containsEntry("adapter", "SOCIAL_DM")
                .containsEntry("platform", "facebook")
                .containsEntry("pageId", "page-1")
                .containsEntry("threadId", "thread-1")
                .containsEntry("quickReplyPayload", "product-a")
                .containsEntry("quickReplyTitle", "Product A");
    }

    @Test
    void contractSupportConvertsRawSocialDmPayloadUsingDeclaredPayloadType() {
        ConversationIngressReq req = ConversationAdapterContractSupport.captureRawIngress(
                new SocialDmConversationReplyAdapter(),
                "social_dm",
                Map.ofEntries(
                        entry("canvasId", 10L),
                        entry("versionId", 20L),
                        entry("executionId", "exec-1"),
                        entry("userId", "instagram:user-1"),
                        entry("platform", "instagram"),
                        entry("provider", "meta"),
                        entry("pageId", "page-1"),
                        entry("threadId", "thread-1"),
                        entry("externalMessageId", "dm-msg-1"),
                        entry("eventId", "dm-event-1"),
                        entry("text", "hello"),
                        entry("intent", "GREETING"),
                        entry("attributes", Map.of("locale", "en-US"))),
                "operator-1");

        assertThat(req.channel()).isEqualTo("SOCIAL_DM");
        assertThat(req.attributes()).containsEntry("platform", "instagram")
                .containsEntry("pageId", "page-1")
                .containsEntry("threadId", "thread-1")
                .containsEntry("locale", "en-US");
    }

    @Test
    void rejectsMissingPayload() {
        ConversationAdapterContractSupport.assertRejectsMissingPayload(
                new SocialDmConversationReplyAdapter(),
                "social DM conversation reply payload is required");
    }
}
