package org.chovy.canvas.cdp.domain;

import java.util.Locale;

public enum CdpTagValueType {
    STRING,
    NUMBER,
    BOOLEAN,
    JSON;

    public static CdpTagValueType from(String valueType) {
        String normalized = valueType == null || valueType.isBlank()
                ? STRING.name()
                : valueType.trim().toUpperCase(Locale.ROOT);
        try {
            return CdpTagValueType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("unsupported tag value type: " + normalized, ex);
        }
    }

    public String normalize(String value) {
        if (value == null) {
            return null;
        }
        return switch (this) {
            case BOOLEAN -> normalizeBoolean(value);
            case NUMBER -> normalizeNumber(value);
            case JSON, STRING -> value;
        };
    }

    private static String normalizeBoolean(String value) {
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            throw new IllegalArgumentException("BOOLEAN tag value must be true or false");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private static String normalizeNumber(String value) {
        try {
            Double.parseDouble(value);
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("NUMBER tag value must be numeric", ex);
        }
    }
}
