package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;

public record MarketingMonitorPollCommand(
        LocalDateTime requestedFrom,
        LocalDateTime requestedUntil,
        String cursorOverride,
        int maxItems,
        boolean force) {
}
