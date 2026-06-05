package org.chovy.canvas.domain.bi.dataset;

public record BiDatasetFieldResource(
        String fieldKey,
        String displayName,
        String columnExpression,
        String role,
        String dataType,
        String semanticType,
        String defaultAggregation,
        String formatPattern,
        String unit,
        boolean visible,
        String sensitiveLevel,
        int sortOrder
) {
}
