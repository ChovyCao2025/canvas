package org.chovy.canvas.cdp.domain;

import java.util.Locale;

/**
 * 枚举 AudienceSnapshotMode 支持的取值。
 */
public enum AudienceSnapshotMode {
    /**
     * STATIC_LOCKED 枚举值。
     */
    STATIC_LOCKED,
    /**
     * DYNAMIC_REFRESH 枚举值。
     */
    DYNAMIC_REFRESH;

    /**
     * 归一化normalize。
     */
    public static AudienceSnapshotMode normalize(String value) {
        if (value == null || value.isBlank()) {
            return STATIC_LOCKED;
        }
        try {
            return AudienceSnapshotMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return STATIC_LOCKED;
        }
    }
}
