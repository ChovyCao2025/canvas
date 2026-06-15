package org.chovy.canvas.bi.api;

import java.util.List;

public record BiSelfServiceExportQueueResult(
        int checked,
        int processed,
        int skipped,
        List<BiSelfServiceExportJobView> jobs) {

    public BiSelfServiceExportQueueResult {
        jobs = jobs == null ? List.of() : List.copyOf(jobs);
    }
}
