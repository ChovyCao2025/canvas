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

/**
 * WhatsAppWebhookPayloadMapper 编排 domain.conversation 场景的领域业务规则。
 */
@Component
public class WhatsAppWebhookPayloadMapper {

    /**
     * 转换为接口返回或领域视图。
     *
     * @param String string 参数，用于 toAdapterPayloads 流程中的校验、计算或对象转换。
     * @param webhookPayload webhook payload 参数，用于 toAdapterPayloads 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    public List<WhatsAppConversationReplyPayload> toAdapterPayloads(Map<String, Object> webhookPayload) {
        if (webhookPayload == null) {
            throw new IllegalArgumentException("WhatsApp webhook payload is required");
        }
        List<WhatsAppConversationReplyPayload> payloads = new ArrayList<>();
        // WhatsApp can batch entries, changes, and messages; flatten them into one adapter payload per inbound message.
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

    /**
     * toDeliveryReceipts 校验或转换 domain.conversation 场景的数据。
     * @param webhookPayload webhook payload 参数，用于 toDeliveryReceipts 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    public List<DeliveryReceiptRequest> toDeliveryReceipts(Map<String, Object> webhookPayload) {
        if (webhookPayload == null) {
            throw new IllegalArgumentException("WhatsApp webhook payload is required");
        }
        List<DeliveryReceiptRequest> receipts = new ArrayList<>();
        // Status callbacks share the same envelope as messages but map to delivery receipts instead of conversations.
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

    /**
     * 转换为接口返回或领域视图。
     *
     * @param entryId 业务对象 ID，用于定位具体记录。
     * @param change change 参数，用于 toPayload 流程中的校验、计算或对象转换。
     * @param value 待处理值，用于规则计算或转换。
     * @param metadata metadata 参数，用于 toPayload 流程中的校验、计算或对象转换。
     * @param contactsByWaId 业务对象 ID，用于定位具体记录。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @return 返回组装或转换后的结果对象。
     */
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
        // 根据前序判断结果进入后续条件分支。
        } else if ("interactive".equals(type)) {
            Map<String, Object> interactive = map(message.get("interactive"));
            interactiveType = text(interactive.get("type"));
            // Interactive replies store the selected option under a field named by the interactive subtype.
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

    /**
     * 转换为接口返回或领域视图。
     *
     * @param entryId 业务对象 ID，用于定位具体记录。
     * @param metadata metadata 参数，用于 toReceipt 流程中的校验、计算或对象转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回组装或转换后的结果对象。
     */
    private DeliveryReceiptRequest toReceipt(String entryId,
                                             Map<String, Object> metadata,
                                             Map<String, Object> status) {
        // 准备本次处理所需的上下文和中间变量。
        String providerMessageId = text(status.get("id"));
        String rawStatus = text(status.get("status"));
        String timestamp = text(status.get("timestamp"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new DeliveryReceiptRequest(
                "CLOUD_API",
                providerMessageId,
                receiptType,
                receiptIdempotencyKey(entryId, providerMessageId, receiptType, timestamp),
                occurredAt(timestamp),
                rawPayload);
    }

    /**
     * 执行 contactsByWaId 流程，围绕 contacts by wa id 完成校验、计算或结果组装。
     *
     * @param rawContacts raw contacts 参数，用于 contactsByWaId 流程中的校验、计算或对象转换。
     * @return 返回 contactsByWaId 流程生成的业务结果。
     */
    private static Map<String, Object> contactsByWaId(Object rawContacts) {
        Map<String, Object> contacts = new LinkedHashMap<>();
        for (Map<String, Object> contact : maps(rawContacts)) {
            String waId = text(contact.get("wa_id"));
            if (!isBlank(waId)) {
                // Contact metadata is referenced by message.from, so index once per webhook envelope.
                contacts.put(waId, contact);
            }
        }
        return contacts;
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回组装或转换后的结果对象。
     */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> maps(Object value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> maps = new ArrayList<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                maps.add((Map<String, Object>) map);
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return maps;
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回组装或转换后的结果对象。
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    /**
     * 执行 occurredAt 流程，围绕 occurred at 完成校验、计算或结果组装。
     *
     * @param timestamp 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 occurredAt 流程生成的业务结果。
     */
    private static LocalDateTime occurredAt(Object timestamp) {
        String value = text(timestamp);
        if (isBlank(value)) {
            return null;
        }
        try {
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(value)), ZoneOffset.UTC);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * 执行 eventId 流程，围绕 event id 完成校验、计算或结果组装。
     *
     * @param entryId 业务对象 ID，用于定位具体记录。
     * @param messageId 业务对象 ID，用于定位具体记录。
     * @return 返回 event id 生成的文本或业务键。
     */
    private static String eventId(String entryId, String messageId) {
        if (isBlank(entryId)) {
            return "whatsapp:" + messageId;
        }
        return "whatsapp:" + entryId + ":" + messageId;
    }

    /**
     * 执行 receiptIdempotencyKey 流程，围绕 receipt idempotency key 完成校验、计算或结果组装。
     *
     * @param entryId 业务对象 ID，用于定位具体记录。
     * @param providerMessageId 业务对象 ID，用于定位具体记录。
     * @param receiptType 类型标识，用于选择对应处理分支。
     * @param timestamp 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 receipt idempotency key 生成的文本或业务键。
     */
    private static String receiptIdempotencyKey(String entryId,
                                                String providerMessageId,
                                                String receiptType,
                                                String timestamp) {
        String prefix = isBlank(entryId) ? "whatsapp" : "whatsapp:" + entryId;
        return prefix + ":" + providerMessageId + ":" + receiptType + ":" + defaultString(timestamp, "unknown");
    }

    /**
     * 执行 text 流程，围绕 text 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 text 生成的文本或业务键。
     */
    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private static String defaultString(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    /**
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @param String string 参数，用于 putIfPresent 流程中的校验、计算或对象转换。
     * @param target target 参数，用于 putIfPresent 流程中的校验、计算或对象转换。
     * @param key 业务键，用于在同一租户下定位资源。
     * @param value 待处理值，用于规则计算或转换。
     */
    private static void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value instanceof String text) {
            if (!text.isBlank()) {
                target.put(key, text.trim());
            }
        // 根据前序判断结果进入后续条件分支。
        } else if (value != null) {
            target.put(key, value);
        }
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
