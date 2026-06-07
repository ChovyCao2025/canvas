package org.chovy.canvas.domain.bi.query;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;
import java.util.Map;

public record BiQueryRequest(
        String datasetKey,
        String dashboardKey,
        List<String> dimensions,
        List<String> metrics,
        List<BiFilter> filters,
        @JsonAlias("sort")
        List<BiSort> sorts,
        int limit,
        int offset,
        Map<String, String> sqlParameters
) {
    public BiQueryRequest {
        dashboardKey = dashboardKey == null || dashboardKey.isBlank() ? null : dashboardKey.trim();
        dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
        filters = filters == null ? List.of() : List.copyOf(filters);
        sorts = sorts == null ? List.of() : List.copyOf(sorts);
        offset = Math.max(0, offset);
        sqlParameters = sqlParameters == null ? Map.of() : Map.copyOf(sqlParameters);
    }

    public BiQueryRequest(String datasetKey,
                          List<String> dimensions,
                          List<String> metrics,
                          List<BiFilter> filters,
                          List<BiSort> sorts,
                          int limit) {
        this(datasetKey, null, dimensions, metrics, filters, sorts, limit, 0, Map.of());
    }

    public BiQueryRequest(String datasetKey,
                          List<String> dimensions,
                          List<String> metrics,
                          List<BiFilter> filters,
                          List<BiSort> sorts,
                          int limit,
                          int offset) {
        this(datasetKey, null, dimensions, metrics, filters, sorts, limit, offset, Map.of());
    }

    public BiQueryRequest(String datasetKey,
                          String dashboardKey,
                          List<String> dimensions,
                          List<String> metrics,
                          List<BiFilter> filters,
                          List<BiSort> sorts,
                          int limit) {
        this(datasetKey, dashboardKey, dimensions, metrics, filters, sorts, limit, 0, Map.of());
    }

    public BiQueryRequest(String datasetKey,
                          String dashboardKey,
                          List<String> dimensions,
                          List<String> metrics,
                          List<BiFilter> filters,
                          List<BiSort> sorts,
                          int limit,
                          int offset) {
        this(datasetKey, dashboardKey, dimensions, metrics, filters, sorts, limit, offset, Map.of());
    }
}
