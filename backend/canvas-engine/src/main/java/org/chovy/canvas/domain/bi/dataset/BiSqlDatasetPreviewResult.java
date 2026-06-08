package org.chovy.canvas.domain.bi.dataset;

import org.chovy.canvas.domain.bi.query.BiQueryColumn;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BiSqlDatasetPreviewResult 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param datasetKey datasetKey 字段。
 * @param normalizedSqlTemplate normalizedSqlTemplate 字段。
 * @param compiledSql compiledSql 字段。
 * @param parameterCount parameterCount 字段。
 * @param columns columns 字段。
 * @param rows rows 字段。
 * @param rowCount rowCount 字段。
 * @param sampleLimit sampleLimit 字段。
 * @param sampleExecuted sampleExecuted 字段。
 * @param executionError executionError 字段。
 * @param lineage lineage 字段。
 * @param impact impact 字段。
 */
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
