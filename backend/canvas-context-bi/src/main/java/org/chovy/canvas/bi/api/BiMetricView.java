package org.chovy.canvas.bi.api;
/**
 * BiMetricView 视图。
 */
public record BiMetricView(
        /**
         * 指标键。
         */
        String metricKey,
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
}
