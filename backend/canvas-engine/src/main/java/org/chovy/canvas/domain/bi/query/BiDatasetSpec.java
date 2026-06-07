package org.chovy.canvas.domain.bi.query;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record BiDatasetSpec(
        String datasetKey,
        String tableExpression,
        String tenantColumn,
        Map<String, BiFieldSpec> fields,
        Map<String, BiMetricSpec> metrics,
        List<BiSqlParameterSpec> sqlParameters,
        Map<String, Object> model
) {
    public BiDatasetSpec {
        fields = Map.copyOf(fields);
        metrics = Map.copyOf(metrics);
        sqlParameters = sqlParameters == null ? List.of() : List.copyOf(sqlParameters);
        model = model == null || model.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(model));
    }

    public BiDatasetSpec(String datasetKey,
                         String tableExpression,
                         String tenantColumn,
                         Map<String, BiFieldSpec> fields,
                         Map<String, BiMetricSpec> metrics,
                         List<BiSqlParameterSpec> sqlParameters) {
        this(datasetKey, tableExpression, tenantColumn, fields, metrics, sqlParameters, Map.of());
    }

    public BiDatasetSpec(String datasetKey,
                         String tableExpression,
                         String tenantColumn,
                         Map<String, BiFieldSpec> fields,
                         Map<String, BiMetricSpec> metrics) {
        this(datasetKey, tableExpression, tenantColumn, fields, metrics, List.of());
    }
}
