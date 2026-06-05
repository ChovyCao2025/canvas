package org.chovy.canvas.domain.bi.query;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record BiQueryResult(
        String datasetKey,
        List<BiQueryColumn> columns,
        List<Map<String, Object>> rows,
        int rowCount,
        long durationMs,
        String sqlHash,
        boolean cached
) {
    public BiQueryResult {
        columns = List.copyOf(columns);
        rows = rows.stream()
                .map(row -> Collections.unmodifiableMap(new LinkedHashMap<>(row)))
                .toList();
    }

    public BiQueryResult(
            String datasetKey,
            List<BiQueryColumn> columns,
            List<Map<String, Object>> rows,
            int rowCount,
            long durationMs,
            String sqlHash) {
        this(datasetKey, columns, rows, rowCount, durationMs, sqlHash, false);
    }

    public BiQueryResult asCached(long durationMs) {
        return new BiQueryResult(datasetKey, columns, rows, rowCount, durationMs, sqlHash, true);
    }
}
