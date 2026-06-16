package org.chovy.canvas.bi.domain;

import java.util.Locale;
/**
 * BiMetric 指标模型。
 */
public record BiMetric(
        /**
         * 指标键。
         */
        BiResourceKey metricKey,
        /**
         * displayName 字段值。
         */
        String displayName,
        /**
         * expression 字段值。
         */
        String expression,
        /**
         * aggregation 字段值。
         */
        String aggregation,
        /**
         * dataType 字段值。
         */
        String dataType,
        /**
         * 计量单位。
         */
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
