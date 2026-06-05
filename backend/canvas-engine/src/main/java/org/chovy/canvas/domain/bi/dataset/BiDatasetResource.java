package org.chovy.canvas.domain.bi.dataset;

import java.util.List;
import java.util.Map;

public record BiDatasetResource(
        String datasetKey,
        String name,
        String datasetType,
        String tableExpression,
        String tenantColumn,
        Map<String, Object> model,
        List<BiDatasetFieldResource> fields,
        List<BiMetricResource> metrics,
        String status,
        String source
) {
    public BiDatasetResource {
        model = model == null ? Map.of() : Map.copyOf(model);
        fields = fields == null ? List.of() : List.copyOf(fields);
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
    }
}
