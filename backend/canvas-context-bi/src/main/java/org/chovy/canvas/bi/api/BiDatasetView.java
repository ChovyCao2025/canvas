package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record BiDatasetView(
        Long id,
        Long tenantId,
        Long workspaceId,
        String datasetKey,
        String name,
        String datasetType,
        Long sourceRefId,
        String tableExpression,
        String tenantColumn,
        Map<String, Object> model,
        List<BiDatasetFieldView> fields,
        List<BiMetricView> metrics,
        String status,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public BiDatasetView {
        model = model == null ? Map.of() : Map.copyOf(model);
        fields = fields == null ? List.of() : List.copyOf(fields);
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
    }
}
