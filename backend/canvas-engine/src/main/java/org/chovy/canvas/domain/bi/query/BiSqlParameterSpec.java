package org.chovy.canvas.domain.bi.query;

import java.util.List;

public record BiSqlParameterSpec(
        String key,
        String type,
        boolean required,
        String defaultValue,
        List<String> allowedValues
) {
    public BiSqlParameterSpec {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("SQL parameter key is required");
        }
        key = key.trim();
        type = type == null || type.isBlank() ? "STRING" : type.trim();
        defaultValue = defaultValue == null || defaultValue.isBlank() ? null : defaultValue.trim();
        allowedValues = allowedValues == null ? List.of() : List.copyOf(allowedValues);
    }
}
