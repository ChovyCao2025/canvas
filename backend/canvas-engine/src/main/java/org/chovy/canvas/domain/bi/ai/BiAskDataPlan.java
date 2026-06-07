package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.query.BiFilter;
import org.chovy.canvas.domain.bi.query.BiSort;

import java.util.List;

public record BiAskDataPlan(
        String datasetKey,
        List<String> dimensions,
        List<String> metrics,
        List<BiFilter> filters,
        List<BiSort> sorts,
        int limit,
        String explanation
) {
    public BiAskDataPlan {
        dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
        filters = filters == null ? List.of() : List.copyOf(filters);
        sorts = sorts == null ? List.of() : List.copyOf(sorts);
    }
}
