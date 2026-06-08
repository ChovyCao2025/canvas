package org.chovy.canvas.domain.bi.datasource;

import org.chovy.canvas.domain.bi.query.BiQueryColumn;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BiDatasourceApiPreview 承载 domain.bi.datasource 场景中的不可变数据快照。
 * @param id id 字段。
 * @param sourceKey sourceKey 字段。
 * @param name name 字段。
 * @param connectorType connectorType 字段。
 * @param columns columns 字段。
 * @param rows rows 字段。
 * @param rowCount rowCount 字段。
 * @param truncated truncated 字段。
 * @param durationMs durationMs 字段。
 * @param checkedAt checkedAt 字段。
 */
public record BiDatasourceApiPreview(
        Long id,
        String sourceKey,
        String name,
        String connectorType,
        List<BiQueryColumn> columns,
        List<Map<String, Object>> rows,
        int rowCount,
        boolean truncated,
        long durationMs,
        LocalDateTime checkedAt
) {

    public BiDatasourceApiPreview {
        columns = columns == null ? List.of() : List.copyOf(columns);
        rows = rows == null ? List.of() : rows.stream()
                .map(row -> Collections.unmodifiableMap(new LinkedHashMap<>(row)))
                .toList();
    }
}
