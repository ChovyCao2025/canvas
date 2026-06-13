package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;

public record WarehouseSyncRun(
        String status,
        LocalDateTime finishedAt,
        LocalDateTime startedAt,
        LocalDateTime windowEnd,
        LocalDateTime windowStart) {
}
