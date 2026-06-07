package org.chovy.canvas.domain.bi.datasource;

import org.chovy.canvas.domain.bi.query.BiQueryColumn;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
