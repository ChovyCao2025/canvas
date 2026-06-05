package org.chovy.canvas.domain.bi.query;

import java.util.Map;

public record BiDatasetSpec(
        String datasetKey,
        String tableExpression,
        String tenantColumn,
        Map<String, BiFieldSpec> fields,
        Map<String, BiMetricSpec> metrics
) {
    public BiDatasetSpec {
        fields = Map.copyOf(fields);
        metrics = Map.copyOf(metrics);
    }
}
