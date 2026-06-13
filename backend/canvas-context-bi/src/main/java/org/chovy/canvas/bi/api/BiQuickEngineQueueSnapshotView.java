package org.chovy.canvas.bi.api;

import java.util.List;

public record BiQuickEngineQueueSnapshotView(
        Long tenantId,
        String poolKey,
        Long queued,
        Long claimed,
        Long completed,
        Long blocked,
        Long total,
        List<BiQuickEngineQueueItemView> jobs) {
}
