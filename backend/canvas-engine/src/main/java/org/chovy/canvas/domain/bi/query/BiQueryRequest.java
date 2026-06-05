package org.chovy.canvas.domain.bi.query;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record BiQueryRequest(
        String datasetKey,
        List<String> dimensions,
        List<String> metrics,
        List<BiFilter> filters,
        @JsonAlias("sort")
        List<BiSort> sorts,
        int limit
) {
    public BiQueryRequest {
        dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
        filters = filters == null ? List.of() : List.copyOf(filters);
        sorts = sorts == null ? List.of() : List.copyOf(sorts);
    }
}
