package org.chovy.canvas.domain.conversation;

import org.junit.jupiter.api.Test;
import org.chovy.canvas.engine.delivery.DeliveryReceiptRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WhatsAppWebhookPayloadMapperTest {

    @Test
    void mapsCloudApiTextMessageToAdapterPayload() {
        WhatsAppWebhookPayloadMapper mapper = new WhatsAppWebhookPayloadMapper();

        List<WhatsAppConversationReplyPayload> payloads = mapper.toAdapterPayloads(webhookMessage(Map.of(
                "from", "15551234567",
                "id", "wamid.1",
                "timestamp", "1780372800",
                "type", "text",
                "text", Map.of("body", "I want pricing"))));

        assertThat(payloads).hasSize(1);
        WhatsAppConversationReplyPayload payload = payloads.get(0);
        assertThat(payload.userId()).isEqualTo("whatsapp:15551234567");
        assertThat(payload.provider()).isEqualTo("CLOUD_API");
        assertThat(payload.externalMessageId()).isEqualTo("wamid.1");
        assertThat(payload.eventId()).isEqualTo("whatsapp:entry-1:wamid.1");
        assertThat(payload.text()).isEqualTo("I want pricing");
        assertThat(payload.interactiveReplyId()).isNull();
        assertThat(payload.attributes()).containsEntry("phoneNumberId", "phone-number-id-1")
                .containsEntry("displayPhoneNumber", "+15550000000")
                .containsEntry("profileName", "Mia")
                .containsEntry("waId", "15551234567")
                .containsEntry("messageType", "text");
        assertThat(payload.occurredAt()).isEqualTo(LocalDateTime.of(2026, 6, 2, 4, 0));
    }

    @Test
    void mapsCloudApiInteractiveButtonReplyToAdapterPayload() {
        WhatsAppWebhookPayloadMapper mapper = new WhatsAppWebhookPayloadMapper();

        List<WhatsAppConversationReplyPayload> payloads = mapper.toAdapterPayloads(webhookMessage(Map.of(
                "from", "15551234567",
                "id", "wamid.2",
                "timestamp", "1780372801",
                "type", "interactive",
                "interactive", Map.of(
                        "type", "button_reply",
                        "button_reply", Map.of("id", "book-demo", "title", "Book a demo")))));

        assertThat(payloads).hasSize(1);
        WhatsAppConversationReplyPayload payload = payloads.get(0);
        assertThat(payload.text()).isEqualTo("Book a demo");
        assertThat(payload.interactiveReplyId()).isEqualTo("book-demo");
        assertThat(payload.interactiveReplyTitle()).isEqualTo("Book a demo");
        assertThat(payload.attributes()).containsEntry("interactiveType", "button_reply")
                .containsEntry("messageType", "interactive");
    }

    @Test
    void skipsStatusOnlyWebhookPayloads() {
        WhatsAppWebhookPayloadMapper mapper = new WhatsAppWebhookPayloadMapper();

        List<WhatsAppConversationReplyPayload> payloads = mapper.toAdapterPayloads(statusWebhook());

        assertThat(payloads).isEmpty();
    }

    @Test
    void mapsCloudApiStatusesToDeliveryReceipts() {
        WhatsAppWebhookPayloadMapper mapper = new WhatsAppWebhookPayloadMapper();

        List<DeliveryReceiptRequest> receipts = mapper.toDeliveryReceipts(statusWebhook());

        assertThat(receipts).hasSize(1);
        DeliveryReceiptRequest receipt = receipts.get(0);
        assertThat(receipt.provider()).isEqualTo("CLOUD_API");
        assertThat(receipt.providerMessageId()).isEqualTo("wamid.1");
        assertThat(receipt.receiptType()).isEqualTo("DELIVERED");
        assertThat(receipt.idempotencyKey()).isEqualTo("whatsapp:entry-1:wamid.1:DELIVERED:1780372800");
        assertThat(receipt.receivedAt()).isEqualTo(LocalDateTime.of(2026, 6, 2, 4, 0));
        assertThat(receipt.rawPayload()).containsEntry("phoneNumberId", "phone-number-id-1")
                .containsEntry("recipientId", "15551234567")
                .containsEntry("status", "delivered");
    }

    @Test
    void rejectsMissingWebhookPayload() {
        WhatsAppWebhookPayloadMapper mapper = new WhatsAppWebhookPayloadMapper();

        assertThatThrownBy(() -> mapper.toAdapterPayloads(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("WhatsApp webhook payload is required");
    }

    private Map<String, Object> webhookMessage(Map<String, Object> message) {
        return Map.of(
                "object", "whatsapp_business_account",
                "entry", List.of(Map.of(
                        "id", "entry-1",
                        "changes", List.of(Map.of(
                                "field", "messages",
                                "value", Map.of(
                                        "messaging_product", "whatsapp",
                                        "metadata", Map.of(
                                                "display_phone_number", "+15550000000",
                                                "phone_number_id", "phone-number-id-1"),
                                        "contacts", List.of(Map.of(
                                                "wa_id", "15551234567",
                                                "profile", Map.of("name", "Mia"))),
                                        "messages", List.of(message)))))));
    }

    private Map<String, Object> statusWebhook() {
        return Map.of(
                "object", "whatsapp_business_account",
                "entry", List.of(Map.of(
                        "id", "entry-1",
                        "changes", List.of(Map.of(
                                "field", "messages",
                                "value", Map.of(
                                        "messaging_product", "whatsapp",
                                        "metadata", Map.of(
                                                "display_phone_number", "+15550000000",
                                                "phone_number_id", "phone-number-id-1"),
                                        "statuses", List.of(Map.of(
                                                "id", "wamid.1",
                                                "status", "delivered",
                                                "timestamp", "1780372800",
                                                "recipient_id", "15551234567"))))))));
    }
}
