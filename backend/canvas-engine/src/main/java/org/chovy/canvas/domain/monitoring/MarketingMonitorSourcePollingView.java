package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;

public record MarketingMonitorSourcePollingView(
        Long tenantId,
        Long sourceId,
        String sourceKey,
        String sourceType,
        boolean pollEnabled,
        int pollIntervalMinutes,
        String pollCursor,
        LocalDateTime lastPolledAt,
        LocalDateTime nextPollAt,
        String lastPollStatus,
        LocalDateTime updatedAt) {
}
