package org.chovy.canvas.domain.bi.dataset;

import java.util.List;

/**
 * BiMetricResource 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param metricKey metricKey 字段。
 * @param displayName displayName 字段。
 * @param expression expression 字段。
 * @param aggregation aggregation 字段。
 * @param dataType dataType 字段。
 * @param unit unit 字段。
 * @param formatPattern formatPattern 字段。
 * @param allowedDimensions allowedDimensions 字段。
 * @param owner owner 字段。
 * @param description description 字段。
 * @param status status 字段。
 */
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
