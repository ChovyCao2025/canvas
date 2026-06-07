package org.chovy.canvas.domain.conversation;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsAppConversationReplyAdapterTest {

    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 6, 6, 10, 30);

    @Test
    void mapsTextReplyToWhatsAppIngressRequest() {
        WhatsAppConversationReplyAdapter adapter = new WhatsAppConversationReplyAdapter();

        ConversationIngressReq req = adapter.toIngress(new WhatsAppConversationReplyPayload(
                        10L,
                        20L,
                        "exec-1",
                        "whatsapp:+15551234567",
                        "twilio",
                        "wamid.1",
                        "webhook-1",
                        "I need details",
                        null,
                        null,
                        "DETAILS",
                        Map.of("profileName", "Mia"),
                        OCCURRED_AT),
                new ConversationAdapterContext(8L, "system"));

        assertThat(req.canvasId()).isEqualTo(10L);
        assertThat(req.versionId()).isEqualTo(20L);
        assertThat(req.executionId()).isEqualTo("exec-1");
        assertThat(req.userId()).isEqualTo("whatsapp:+15551234567");
        assertThat(req.channel()).isEqualTo("WHATSAPP");
        assertThat(req.provider()).isEqualTo("TWILIO");
        assertThat(req.externalMessageId()).isEqualTo("wamid.1");
        assertThat(req.eventId()).isEqualTo("webhook-1");
        assertThat(req.messageType()).isEqualTo("TEXT");
        assertThat(req.text()).isEqualTo("I need details");
        assertThat(req.intent()).isEqualTo("DETAILS");
        assertThat(req.attributes()).containsEntry("adapter", "WHATSAPP")
                .containsEntry("profileName", "Mia");
        assertThat(req.occurredAt()).isEqualTo(OCCURRED_AT);
    }

    @Test
    void mapsInteractiveReplyToInteractiveMessageTypeAndAttributes() {
        WhatsAppConversationReplyAdapter adapter = new WhatsAppConversationReplyAdapter();

        ConversationIngressReq req = adapter.toIngress(new WhatsAppConversationReplyPayload(
                        10L,
                        20L,
                        "exec-1",
                        "whatsapp:+15551234567",
                        "cloud_api",
                        "wamid.2",
                        "webhook-2",
                        null,
                        "product-a",
                        "Product A",
                        "PRODUCT_A",
                        Map.of(),
                        OCCURRED_AT),
                new ConversationAdapterContext(8L, "system"));

        assertThat(req.channel()).isEqualTo("WHATSAPP");
        assertThat(req.provider()).isEqualTo("CLOUD_API");
        assertThat(req.messageType()).isEqualTo("INTERACTIVE");
        assertThat(req.text()).isEqualTo("Product A");
        assertThat(req.intent()).isEqualTo("PRODUCT_A");
        assertThat(req.attributes()).containsEntry("adapter", "WHATSAPP")
                .containsEntry("interactiveReplyId", "product-a")
                .containsEntry("interactiveReplyTitle", "Product A");
    }

    @Test
    void contractSupportConvertsRawWhatsAppPayloadUsingDeclaredPayloadType() {
        ConversationIngressReq req = ConversationAdapterContractSupport.captureRawIngress(
                new WhatsAppConversationReplyAdapter(),
                "whatsapp",
                Map.of(
                        "canvasId", 10L,
                        "versionId", 20L,
                        "executionId", "exec-1",
                        "userId", "whatsapp:+15551234567",
                        "provider", "twilio",
                        "externalMessageId", "wamid.1",
                        "eventId", "webhook-1",
                        "text", "hello",
                        "intent", "GREETING",
                        "attributes", Map.of("profileName", "Mia")),
                "operator-1");

        assertThat(req.channel()).isEqualTo("WHATSAPP");
        assertThat(req.provider()).isEqualTo("TWILIO");
        assertThat(req.attributes()).containsEntry("profileName", "Mia");
    }

    @Test
    void rejectsMissingPayload() {
        ConversationAdapterContractSupport.assertRejectsMissingPayload(
                new WhatsAppConversationReplyAdapter(),
                "whatsapp conversation reply payload is required");
    }
}
