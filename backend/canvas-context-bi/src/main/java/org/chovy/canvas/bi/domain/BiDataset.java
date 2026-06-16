package org.chovy.canvas.bi.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
/**
 * BiDataset 数据集模型。
 */
public record BiDataset(
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
        BiResourceKey datasetKey,
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
        List<BiDatasetField> fields,
        /**
         * 指标列表。
         */
        List<BiMetric> metrics,
        /**
         * 状态值。
         */
        BiResourceStatus status,
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
    public BiDataset {
        tenantId = tenantId == null ? 0L : tenantId;
        if (workspaceId == null || workspaceId <= 0) {
            throw new IllegalArgumentException("workspaceId is required");
        }
        if (datasetKey == null) {
            throw new IllegalArgumentException("datasetKey is required");
        }
        name = name == null || name.isBlank() ? datasetKey.value() : name.trim();
        datasetType = datasetType == null || datasetType.isBlank()
                ? "SQL"
                : datasetType.trim().toUpperCase(java.util.Locale.ROOT);
        if (tableExpression == null || tableExpression.isBlank()) {
            throw new IllegalArgumentException("tableExpression is required");
        }
        tableExpression = tableExpression.trim();
        tenantColumn = tenantColumn == null || tenantColumn.isBlank() ? null : tenantColumn.trim();
        model = model == null ? Map.of() : Map.copyOf(model);
        fields = fields == null ? List.of() : List.copyOf(fields);
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
        status = status == null ? BiResourceStatus.DRAFT : status;
    }
    /**
     * 返回带有指定变更的新对象。
     */
    public BiDataset withId(Long newId) {
        return new BiDataset(newId, tenantId, workspaceId, datasetKey, name, datasetType, sourceRefId,
                tableExpression, tenantColumn, model, fields, metrics, status, createdBy, createdAt, updatedAt);
    }
}
