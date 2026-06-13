package org.chovy.canvas.bi.api;

public record BiMetricView(
        String metricKey,
        String displayName,
        String expression,
        String aggregation,
        String dataType,
        String unit
) {
}
