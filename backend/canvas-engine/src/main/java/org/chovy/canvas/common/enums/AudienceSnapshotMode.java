package org.chovy.canvas.common.enums;

public enum AudienceSnapshotMode {
    STATIC_LOCKED,
    DYNAMIC_REFRESH;

    public static AudienceSnapshotMode normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return STATIC_LOCKED;
        }
        for (AudienceSnapshotMode mode : values()) {
            if (mode.name().equalsIgnoreCase(raw.trim())) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unsupported audienceSnapshotMode: " + raw);
    }
}
