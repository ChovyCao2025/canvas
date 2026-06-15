package org.chovy.canvas.bi.api;

import java.util.Map;

public record BiSelfServiceExportJobDetailView(
        BiSelfServiceExportJobView job,
        Map<String, Object> partition,
        Map<String, Object> audit) {

    public BiSelfServiceExportJobDetailView {
        partition = partition == null ? Map.of() : Map.copyOf(partition);
        audit = audit == null ? Map.of() : Map.copyOf(audit);
    }
}
