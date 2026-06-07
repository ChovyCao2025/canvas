package org.chovy.canvas.domain.bi.dataset;

import java.util.List;

public record BiQuickEngineQueueSnapshotView(
        Long tenantId,
        String poolKey,
        long queued,
        long claimed,
        long completed,
        long blocked,
        long total,
        List<BiQuickEngineQueueJobView> jobs) {
}
