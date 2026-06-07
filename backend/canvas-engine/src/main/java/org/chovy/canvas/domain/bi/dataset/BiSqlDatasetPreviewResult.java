package org.chovy.canvas.domain.bi.dataset;

import org.chovy.canvas.domain.bi.query.BiQueryColumn;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record BiSqlDatasetPreviewResult(
        String datasetKey,
        String normalizedSqlTemplate,
        String compiledSql,
        int parameterCount,
        List<BiQueryColumn> columns,
        List<Map<String, Object>> rows,
        int rowCount,
        int sampleLimit,
        boolean sampleExecuted,
        String executionError,
        BiSqlDatasetLineageView lineage,
        BiSqlDatasetImpactView impact
) {
    public BiSqlDatasetPreviewResult {
        columns = columns == null ? List.of() : List.copyOf(columns);
        rows = rows == null
                ? List.of()
                : rows.stream()
                .map(row -> Collections.unmodifiableMap(new LinkedHashMap<>(row)))
                .toList();
    }
}
