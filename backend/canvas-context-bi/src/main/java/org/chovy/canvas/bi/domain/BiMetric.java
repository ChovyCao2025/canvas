package org.chovy.canvas.bi.domain;

import java.util.Locale;

public record BiMetric(
        BiResourceKey metricKey,
        String displayName,
        String expression,
        String aggregation,
        String dataType,
        String unit
) {
    public BiMetric {
        if (metricKey == null) {
            throw new IllegalArgumentException("metricKey is required");
        }
        displayName = displayName == null || displayName.isBlank() ? metricKey.value() : displayName.trim();
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("metric expression is required");
        }
        expression = expression.trim();
        aggregation = aggregation == null || aggregation.isBlank() ? "SUM" : aggregation.trim().toUpperCase(Locale.ROOT);
        dataType = dataType == null || dataType.isBlank() ? "DECIMAL" : dataType.trim().toUpperCase(Locale.ROOT);
        unit = unit == null || unit.isBlank() ? null : unit.trim().toUpperCase(Locale.ROOT);
    }
}
