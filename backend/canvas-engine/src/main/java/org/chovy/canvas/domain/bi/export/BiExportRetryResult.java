package org.chovy.canvas.domain.bi.export;

import java.util.List;

public record BiExportRetryResult(
        int checked,
        int retried,
        int completed,
        int failed,
        List<BiExportJobView> jobs
) {
    public BiExportRetryResult {
        jobs = jobs == null ? List.of() : List.copyOf(jobs);
    }
}
