package org.chovy.canvas.domain.conversation;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

final class ConversationReplyAdapterSupport {

    private ConversationReplyAdapterSupport() {
    }

    static String normalizeProvider(String provider) {
        if (isBlank(provider)) {
            return "DEFAULT";
        }
        return provider.trim().toUpperCase(Locale.ROOT);
    }

    static String firstText(String text, String fallback) {
        if (!isBlank(text)) {
            return text.trim();
        }
        return isBlank(fallback) ? null : fallback.trim();
    }

    @SafeVarargs
    static Map<String, Object> adapterAttributes(
            String adapterKey,
            Map<String, Object> sourceAttributes,
            Map.Entry<String, String>... optionalTextAttributes) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        if (sourceAttributes != null) {
            attributes.putAll(sourceAttributes);
        }
        attributes.put("adapter", adapterKey);
        for (Map.Entry<String, String> attribute : optionalTextAttributes) {
            if (attribute != null && !isBlank(attribute.getValue())) {
                attributes.put(attribute.getKey(), attribute.getValue().trim());
            }
        }
        return attributes;
    }

    static Map.Entry<String, String> textAttribute(String key, String value) {
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }

    static boolean hasAnyText(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return true;
            }
        }
        return false;
    }

    static ConversationIngressReq providerIngress(ProviderConversationReplyPayload payload,
                                                  String channel,
                                                  boolean interactive,
                                                  String fallbackText,
                                                  Map<String, ?> attributes) {
        return new ConversationIngressReq(
                payload.canvasId(),
                payload.versionId(),
                payload.executionId(),
                payload.userId(),
                channel,
                normalizeProvider(payload.provider()),
                payload.externalMessageId(),
                payload.eventId(),
                interactive ? "INTERACTIVE" : "TEXT",
                firstText(payload.text(), fallbackText),
                payload.intent(),
                attributes == null ? Map.of() : new LinkedHashMap<>(attributes),
                payload.occurredAt());
    }

    static ConversationIngressReq providerIngress(ProviderConversationReplyPayload payload,
                                                  String channel,
                                                  String fallbackText,
                                                  Map<String, ?> attributes,
                                                  String... interactiveMarkers) {
        return providerIngress(
                payload,
                channel,
                hasAnyText(interactiveMarkers),
                fallbackText,
                attributes);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
