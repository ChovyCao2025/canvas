package org.chovy.canvas.domain.bi.query;

import java.util.List;

/**
 * BiMetricSpec 承载 domain.bi.query 场景中的不可变数据快照。
 * @param metricKey metricKey 字段。
 * @param expression expression 字段。
 * @param valueType valueType 字段。
 * @param allowedDimensions allowedDimensions 字段。
 */
public record BiMetricSpec(
        String metricKey,
        String expression,
        String valueType,
        List<String> allowedDimensions
) {
    /**
     * 创建 BiMetricSpec 实例并注入 domain.bi.query 场景依赖。
     * @param metricKey 业务键，用于在同一租户下定位资源。
     * @param expression expression 参数，用于 BiMetricSpec 流程中的校验、计算或对象转换。
     * @param valueType 类型标识，用于选择对应处理分支。
     */
    public BiMetricSpec(String metricKey, String expression, String valueType) {
        this(metricKey, expression, valueType, List.of());
    }

    public BiMetricSpec {
        allowedDimensions = allowedDimensions == null ? List.of() : List.copyOf(allowedDimensions);
    }
}
