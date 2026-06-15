package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;

public record BiQueryCommand(
        String datasetKey,
        String dashboardKey,
        List<String> dimensions,
        List<String> metrics,
        List<Map<String, Object>> filters,
        List<Map<String, Object>> sorts,
        int limit,
        int offset,
        Map<String, String> sqlParameters) {

    public BiQueryCommand {
        dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
        filters = filters == null ? List.of() : List.copyOf(filters);
        sorts = sorts == null ? List.of() : List.copyOf(sorts);
        sqlParameters = sqlParameters == null ? Map.of() : Map.copyOf(sqlParameters);
        offset = Math.max(0, offset);
    }
}
