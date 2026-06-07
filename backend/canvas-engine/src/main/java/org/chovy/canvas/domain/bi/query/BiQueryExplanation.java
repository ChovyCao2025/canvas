package org.chovy.canvas.domain.bi.query;

import java.util.List;

public record BiQueryExplanation(
        String datasetKey,
        String sqlHash,
        int parametersCount,
        List<String> steps
) {
    public BiQueryExplanation {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
