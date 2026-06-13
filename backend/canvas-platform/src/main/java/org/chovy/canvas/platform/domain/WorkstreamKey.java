package org.chovy.canvas.platform.domain;

import java.util.Locale;
import java.util.Objects;

public record WorkstreamKey(String value) {

    public WorkstreamKey {
        value = normalize(value);
    }

    private static String normalize(String value) {
        String normalized = Objects.requireNonNull(value, "workstreamKey").trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9][a-z0-9-]{0,127}")) {
            throw new IllegalArgumentException("invalid workstream key: " + value);
        }
        return normalized;
    }
}
