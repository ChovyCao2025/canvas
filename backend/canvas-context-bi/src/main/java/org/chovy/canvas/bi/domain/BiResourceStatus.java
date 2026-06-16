package org.chovy.canvas.bi.domain;

import java.util.Locale;
/**
 * BiResourceStatus 枚举值集合。
 */
public enum BiResourceStatus {
    /**
     * DRAFT 枚举值。
     */
    DRAFT,

    /**
     * PUBLISHED 枚举值。
     */
    PUBLISHED,

    /**
     * ARCHIVED 枚举值。
     */
    ARCHIVED;

    /**
     * 从输入值构建目标对象。
     */
    public static BiResourceStatus from(String value) {
        if (value == null || value.isBlank()) {
            return DRAFT;
        }
        try {
            return BiResourceStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("unsupported BI resource status: " + value, ex);
        }
    }
    /**
     * 发布业务资源。
     */
    public boolean published() {
        return this == PUBLISHED;
    }
}
