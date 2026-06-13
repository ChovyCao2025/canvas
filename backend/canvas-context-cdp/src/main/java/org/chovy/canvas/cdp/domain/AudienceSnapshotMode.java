package org.chovy.canvas.cdp.domain;

import java.util.Locale;

public enum AudienceSnapshotMode {
    STATIC_LOCKED,
    DYNAMIC_REFRESH;

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
