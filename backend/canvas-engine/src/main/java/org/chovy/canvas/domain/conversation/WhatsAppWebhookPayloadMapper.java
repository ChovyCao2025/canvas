package org.chovy.canvas.domain.conversation;

import org.chovy.canvas.engine.delivery.DeliveryReceiptRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class WhatsAppWebhookPayloadMapper {

    public List<WhatsAppConversationReplyPayload> toAdapterPayloads(Map<String, Object> webhookPayload) {
        if (webhookPayload == null) {
            throw new IllegalArgumentException("WhatsApp webhook payload is required");
        }
        List<WhatsAppConversationReplyPayload> payloads = new ArrayList<>();
        for (Map<String, Object> entry : maps(webhookPayload.get("entry"))) {
            String entryId = text(entry.get("id"));
            for (Map<String, Object> change : maps(entry.get("changes"))) {
                Map<String, Object> value = map(change.get("value"));
                if (value.isEmpty()) {
                    continue;
                }
                Map<String, Object> metadata = map(value.get("metadata"));
                Map<String, Object> contactsByWaId = contactsByWaId(value.get("contacts"));
                for (Map<String, Object> message : maps(value.get("messages"))) {
                    WhatsAppConversationReplyPayload payload = toPayload(entryId, change, value, metadata,
                            contactsByWaId, message);
                    if (payload != null) {
                        payloads.add(payload);
                    }
                }
            }
        }
        return payloads;
    }

    public List<DeliveryReceiptRequest> toDeliveryReceipts(Map<String, Object> webhookPayload) {
        if (webhookPayload == null) {
            throw new IllegalArgumentException("WhatsApp webhook payload is required");
        }
        List<DeliveryReceiptRequest> receipts = new ArrayList<>();
        for (Map<String, Object> entry : maps(webhookPayload.get("entry"))) {
            String entryId = text(entry.get("id"));
            for (Map<String, Object> change : maps(entry.get("changes"))) {
                Map<String, Object> value = map(change.get("value"));
                Map<String, Object> metadata = map(value.get("metadata"));
                for (Map<String, Object> status : maps(value.get("statuses"))) {
                    DeliveryReceiptRequest receipt = toReceipt(entryId, metadata, status);
                    if (receipt != null) {
                        receipts.add(receipt);
                    }
                }
            }
        }
        return receipts;
    }

    private WhatsAppConversationReplyPayload toPayload(String entryId,
                                                       Map<String, Object> change,
                                                       Map<String, Object> value,
                                                       Map<String, Object> metadata,
                                                       Map<String, Object> contactsByWaId,
                                                       Map<String, Object> message) {
        String from = text(message.get("from"));
        String messageId = text(message.get("id"));
        if (isBlank(from) || isBlank(messageId)) {
            return null;
        }
        String type = defaultString(text(message.get("type")), "unknown");
        String text = null;
        String interactiveReplyId = null;
        String interactiveReplyTitle = null;
        String interactiveType = null;
        if ("text".equals(type)) {
            text = text(map(message.get("text")).get("body"));
        } else if ("interactive".equals(type)) {
            Map<String, Object> interactive = map(message.get("interactive"));
            interactiveType = text(interactive.get("type"));
            Map<String, Object> reply = map(interactive.get(interactiveType));
            interactiveReplyId = text(reply.get("id"));
            interactiveReplyTitle = text(reply.get("title"));
            text = interactiveReplyTitle;
        } else {
            return null;
        }

        Map<String, Object> contact = map(contactsByWaId.get(from));
        Map<String, Object> profile = map(contact.get("profile"));
        Map<String, Object> attributes = new LinkedHashMap<>();
        putIfPresent(attributes, "entryId", entryId);
        putIfPresent(attributes, "changeField", text(change.get("field")));
        putIfPresent(attributes, "messagingProduct", text(value.get("messaging_product")));
        putIfPresent(attributes, "displayPhoneNumber", text(metadata.get("display_phone_number")));
        putIfPresent(attributes, "phoneNumberId", text(metadata.get("phone_number_id")));
        putIfPresent(attributes, "waId", text(contact.get("wa_id")));
        putIfPresent(attributes, "profileName", text(profile.get("name")));
        putIfPresent(attributes, "messageTimestamp", text(message.get("timestamp")));
        putIfPresent(attributes, "messageType", type);
        putIfPresent(attributes, "interactiveType", interactiveType);

        return new WhatsAppConversationReplyPayload(
                null,
                null,
                null,
                "whatsapp:" + from,
                "CLOUD_API",
                messageId,
                eventId(entryId, messageId),
                text,
                interactiveReplyId,
                interactiveReplyTitle,
                null,
                attributes,
                occurredAt(message.get("timestamp")));
    }

    private DeliveryReceiptRequest toReceipt(String entryId,
                                             Map<String, Object> metadata,
                                             Map<String, Object> status) {
        String providerMessageId = text(status.get("id"));
        String rawStatus = text(status.get("status"));
        String timestamp = text(status.get("timestamp"));
        if (isBlank(providerMessageId) || isBlank(rawStatus)) {
            return null;
        }
        String receiptType = rawStatus.trim().toUpperCase();
        Map<String, Object> rawPayload = new LinkedHashMap<>();
        putIfPresent(rawPayload, "phoneNumberId", text(metadata.get("phone_number_id")));
        putIfPresent(rawPayload, "displayPhoneNumber", text(metadata.get("display_phone_number")));
        putIfPresent(rawPayload, "recipientId", text(status.get("recipient_id")));
        putIfPresent(rawPayload, "status", rawStatus);
        putIfPresent(rawPayload, "timestamp", timestamp);
        putIfPresent(rawPayload, "entryId", entryId);
        return new DeliveryReceiptRequest(
                "CLOUD_API",
                providerMessageId,
                receiptType,
                receiptIdempotencyKey(entryId, providerMessageId, receiptType, timestamp),
                occurredAt(timestamp),
                rawPayload);
    }

    private static Map<String, Object> contactsByWaId(Object rawContacts) {
        Map<String, Object> contacts = new LinkedHashMap<>();
        for (Map<String, Object> contact : maps(rawContacts)) {
            String waId = text(contact.get("wa_id"));
            if (!isBlank(waId)) {
                contacts.put(waId, contact);
            }
        }
        return contacts;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> maps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> maps = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                maps.add((Map<String, Object>) map);
            }
        }
        return maps;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static LocalDateTime occurredAt(Object timestamp) {
        String value = text(timestamp);
        if (isBlank(value)) {
            return null;
        }
        try {
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(value)), ZoneOffset.UTC);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String eventId(String entryId, String messageId) {
        if (isBlank(entryId)) {
            return "whatsapp:" + messageId;
        }
        return "whatsapp:" + entryId + ":" + messageId;
    }

    private static String receiptIdempotencyKey(String entryId,
                                                String providerMessageId,
                                                String receiptType,
                                                String timestamp) {
        String prefix = isBlank(entryId) ? "whatsapp" : "whatsapp:" + entryId;
        return prefix + ":" + providerMessageId + ":" + receiptType + ":" + defaultString(timestamp, "unknown");
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private static String defaultString(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private static void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value instanceof String text) {
            if (!text.isBlank()) {
                target.put(key, text.trim());
            }
        } else if (value != null) {
            target.put(key, value);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
