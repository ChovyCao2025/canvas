package org.chovy.canvas.domain.bi.dataset;

import java.util.List;

public record BiQuickEngineQueueClaimResult(
        int expired,
        int claimed,
        List<BiQuickEngineQueueJobView> jobs
) {
    public BiQuickEngineQueueClaimResult {
        jobs = jobs == null ? List.of() : List.copyOf(jobs);
    }
}
