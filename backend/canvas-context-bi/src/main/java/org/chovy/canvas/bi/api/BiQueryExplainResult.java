package org.chovy.canvas.bi.api;

import java.util.List;

public record BiQueryExplainResult(
        String datasetKey,
        String sqlHash,
        int parametersCount,
        List<String> steps) {

    public BiQueryExplainResult {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
