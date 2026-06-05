package org.chovy.canvas.domain.bi.dataset;

import java.util.List;

public record BiMetricResource(
        String metricKey,
        String displayName,
        String expression,
        String aggregation,
        String dataType,
        String unit,
        String formatPattern,
        List<String> allowedDimensions,
        String owner,
        String description,
        String status
) {
    public BiMetricResource {
        allowedDimensions = allowedDimensions == null ? List.of() : List.copyOf(allowedDimensions);
    }
}
