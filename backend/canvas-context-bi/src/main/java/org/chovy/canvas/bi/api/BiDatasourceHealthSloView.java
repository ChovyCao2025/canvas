package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;

public record BiDatasourceHealthSloView(
        int totalChecks,
        int availableChecks,
        int unavailableChecks,
        double availabilityRate,
        List<Map<String, Object>> sources) {

    public BiDatasourceHealthSloView {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
