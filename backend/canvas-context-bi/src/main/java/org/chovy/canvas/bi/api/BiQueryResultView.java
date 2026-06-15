package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;

public record BiQueryResultView(
        String datasetKey,
        List<Map<String, Object>> columns,
        List<Map<String, Object>> rows,
        int rowCount,
        long durationMs,
        String sqlHash,
        boolean cached) {

    public BiQueryResultView {
        columns = columns == null ? List.of() : List.copyOf(columns);
        rows = rows == null ? List.of() : List.copyOf(rows);
    }
}
