package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;

public record BiDatasetCommand(
        Long workspaceId,
        String datasetKey,
        String name,
        String datasetType,
        Long sourceRefId,
        String tableExpression,
        String tenantColumn,
        Map<String, Object> model,
        List<BiDatasetFieldCommand> fields,
        List<BiMetricCommand> metrics,
        String status
) {
    public BiDatasetCommand {
        model = model == null ? Map.of() : Map.copyOf(model);
        fields = fields == null ? List.of() : List.copyOf(fields);
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
    }
}
