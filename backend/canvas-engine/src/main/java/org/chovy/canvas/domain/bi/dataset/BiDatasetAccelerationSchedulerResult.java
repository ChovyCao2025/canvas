package org.chovy.canvas.domain.bi.dataset;

import java.util.List;

public record BiDatasetAccelerationSchedulerResult(
        int policiesChecked,
        int refreshed,
        int skipped,
        int failed,
        List<BiDatasetAccelerationSchedulerItem> items) {

    public BiDatasetAccelerationSchedulerResult(int policiesChecked, int refreshed, int skipped, int failed) {
        this(policiesChecked, refreshed, skipped, failed, List.of());
    }

    public BiDatasetAccelerationSchedulerResult {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
