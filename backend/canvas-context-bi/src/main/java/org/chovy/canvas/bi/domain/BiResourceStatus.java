package org.chovy.canvas.bi.domain;

import java.util.Locale;

public enum BiResourceStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED;

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

    public boolean published() {
        return this == PUBLISHED;
    }
}
