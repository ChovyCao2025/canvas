package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;

public record MarketingMonitorSourcePollingCommand(
        Boolean pollEnabled,
        Integer pollIntervalMinutes,
        String pollCursor,
        LocalDateTime nextPollAt) {
}
