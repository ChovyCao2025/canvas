package org.chovy.canvas.bi.api;

import java.util.List;

public record BiSelfServiceExportRetryResult(
        int checked,
        int retried,
        List<BiSelfServiceExportJobView> jobs) {

    public BiSelfServiceExportRetryResult {
        jobs = jobs == null ? List.of() : List.copyOf(jobs);
    }
}
