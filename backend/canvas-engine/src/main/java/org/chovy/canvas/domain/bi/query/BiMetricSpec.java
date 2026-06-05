package org.chovy.canvas.domain.bi.query;

import java.util.List;

public record BiMetricSpec(
        String metricKey,
        String expression,
        String valueType,
        List<String> allowedDimensions
) {
    public BiMetricSpec(String metricKey, String expression, String valueType) {
        this(metricKey, expression, valueType, List.of());
    }

    public BiMetricSpec {
        allowedDimensions = allowedDimensions == null ? List.of() : List.copyOf(allowedDimensions);
    }
}
