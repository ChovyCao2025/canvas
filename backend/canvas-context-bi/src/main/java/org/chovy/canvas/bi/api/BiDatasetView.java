package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
/**
 * BiDatasetView 视图。
 */
public record BiDatasetView(
        /**
         * 唯一标识。
         */
        Long id,
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * 工作空间标识。
         */
        Long workspaceId,
        /**
         * 数据集键。
         */
        String datasetKey,
        /**
         * 展示名称。
         */
        String name,
        /**
         * datasetType 字段值。
         */
        String datasetType,
        /**
         * sourceRefId 对应的标识。
         */
        Long sourceRefId,
        /**
         * tableExpression 字段值。
         */
        String tableExpression,
        /**
         * tenantColumn 字段值。
         */
        String tenantColumn,
        /**
         * model 字段值。
         */
        Map<String, Object> model,
        /**
         * 字段列表。
         */
        List<BiDatasetFieldView> fields,
        /**
         * 指标列表。
         */
        List<BiMetricView> metrics,
        /**
         * 状态值。
         */
        String status,
        /**
         * 创建人。
         */
        String createdBy,
        /**
         * 创建时间。
         */
        LocalDateTime createdAt,
        /**
         * 更新时间。
         */
        LocalDateTime updatedAt
) {
    public BiDatasetView {
        model = model == null ? Map.of() : Map.copyOf(model);
        fields = fields == null ? List.of() : List.copyOf(fields);
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
    }
}
