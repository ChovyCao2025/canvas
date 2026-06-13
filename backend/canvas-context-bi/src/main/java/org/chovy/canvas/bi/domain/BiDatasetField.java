package org.chovy.canvas.bi.domain;

public record BiDatasetField(
        BiResourceKey fieldKey,
        String displayName,
        String columnExpression,
        String roleKey,
        String dataType,
        String defaultAggregation,
        boolean visible,
        int sortOrder
) {
    public BiDatasetField {
        if (fieldKey == null) {
            throw new IllegalArgumentException("fieldKey is required");
        }
        displayName = displayName == null || displayName.isBlank() ? fieldKey.value() : displayName.trim();
        columnExpression = columnExpression == null || columnExpression.isBlank()
                ? fieldKey.value()
                : columnExpression.trim();
        roleKey = upperOrDefault(roleKey, "DIMENSION");
        dataType = upperOrDefault(dataType, "STRING");
        defaultAggregation = upperOrDefault(defaultAggregation, "NONE");
    }

    private static String upperOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
