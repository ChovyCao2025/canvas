package org.chovy.canvas.bi.api;

import java.util.List;

public record BiQueryDatasetView(
        String datasetKey,
        List<BiQueryFieldView> fields,
        List<BiQueryMetricView> metrics) {
}
