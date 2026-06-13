package org.chovy.canvas.conversation.domain;

import java.util.List;
import java.util.Locale;

public final class ConversationText {

    private ConversationText() {
    }

    public static String required(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    public static String upperRequired(String value, String message) {
        return required(value, message).toUpperCase(Locale.ROOT);
    }

    public static String upperOptional(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    public static String optionalKey(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public static List<String> normalizeKeys(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(ConversationText::optionalKey)
                .filter(value -> value != null && !value.isEmpty())
                .distinct()
                .toList();
    }

    public static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
