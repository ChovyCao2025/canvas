package org.chovy.canvas.bi.api;

public record BiDatasetFieldCommand(
        String fieldKey,
        String displayName,
        String columnExpression,
        String roleKey,
        String dataType,
        String defaultAggregation,
        Boolean visible,
        Integer sortOrder
) {
}
