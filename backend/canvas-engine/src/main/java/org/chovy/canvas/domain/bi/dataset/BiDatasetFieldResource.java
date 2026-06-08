package org.chovy.canvas.domain.bi.dataset;

/**
 * BiDatasetFieldResource 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param fieldKey fieldKey 字段。
 * @param displayName displayName 字段。
 * @param columnExpression columnExpression 字段。
 * @param role role 字段。
 * @param dataType dataType 字段。
 * @param semanticType semanticType 字段。
 * @param defaultAggregation defaultAggregation 字段。
 * @param formatPattern formatPattern 字段。
 * @param unit unit 字段。
 * @param visible visible 字段。
 * @param sensitiveLevel sensitiveLevel 字段。
 * @param sortOrder sortOrder 字段。
 */
public record BiDatasetFieldResource(
        String fieldKey,
        String displayName,
        String columnExpression,
        String role,
        String dataType,
        String semanticType,
        String defaultAggregation,
        String formatPattern,
        String unit,
        boolean visible,
        String sensitiveLevel,
        int sortOrder
) {
}
