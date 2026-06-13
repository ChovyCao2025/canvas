package org.chovy.canvas.bi.api;

public record BiDatasetFieldView(
        String fieldKey,
        String displayName,
        String columnExpression,
        String roleKey,
        String dataType,
        String defaultAggregation,
        boolean visible,
        int sortOrder
) {
}
