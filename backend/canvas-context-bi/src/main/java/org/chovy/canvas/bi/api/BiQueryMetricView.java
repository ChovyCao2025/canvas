package org.chovy.canvas.bi.api;
/**
 * BiQueryMetricView 视图。
 */
public record BiQueryMetricView(
        /**
         * 指标键。
         */
        String metricKey,
        String dataType) {
}
