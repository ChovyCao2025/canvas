package org.chovy.canvas.bi.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record BiDataset(
        Long id,
        Long tenantId,
        Long workspaceId,
        BiResourceKey datasetKey,
        String name,
        String datasetType,
        Long sourceRefId,
        String tableExpression,
        String tenantColumn,
        Map<String, Object> model,
        List<BiDatasetField> fields,
        List<BiMetric> metrics,
        BiResourceStatus status,
        String createdBy,
        LocalDateTime createdAt,
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

    public BiDataset withId(Long newId) {
        return new BiDataset(newId, tenantId, workspaceId, datasetKey, name, datasetType, sourceRefId,
                tableExpression, tenantColumn, model, fields, metrics, status, createdBy, createdAt, updatedAt);
    }
}
