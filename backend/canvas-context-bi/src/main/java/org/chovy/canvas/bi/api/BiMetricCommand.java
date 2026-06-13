package org.chovy.canvas.bi.api;

public record BiMetricCommand(
        String metricKey,
        String displayName,
        String expression,
        String aggregation,
        String dataType,
        String unit
) {
}
