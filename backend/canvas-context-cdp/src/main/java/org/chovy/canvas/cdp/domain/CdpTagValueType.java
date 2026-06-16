package org.chovy.canvas.cdp.domain;

import java.util.Locale;

/**
 * 枚举 CdpTagValueType 支持的取值。
 */
public enum CdpTagValueType {
    /**
     * STRING 枚举值。
     */
    STRING,
    /**
     * NUMBER 枚举值。
     */
    NUMBER,
    /**
     * BOOLEAN 枚举值。
     */
    BOOLEAN,
    /**
     * JSON 枚举值。
     */
    JSON;

    /**
     * 执行 from 对应的 CDP 业务操作。
     */
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

    /**
     * 归一化normalize。
     */
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

    /**
     * 归一化Boolean。
     */
    private static String normalizeBoolean(String value) {
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            throw new IllegalArgumentException("BOOLEAN tag value must be true or false");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    /**
     * 归一化Number。
     */
    private static String normalizeNumber(String value) {
        try {
            Double.parseDouble(value);
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("NUMBER tag value must be numeric", ex);
        }
    }
}
