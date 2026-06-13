package org.chovy.canvas.bi.domain;

import java.util.Locale;
import java.util.Objects;

public record BiResourceKey(String value) {

    public BiResourceKey {
        value = normalize(value, "resourceKey");
    }

    public static BiResourceKey of(String value, String field) {
        return new BiResourceKey(normalize(value, field));
    }

    private static String normalize(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String normalized = value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return normalized;
    }

    @Override
    public String toString() {
        return Objects.toString(value);
    }
}
