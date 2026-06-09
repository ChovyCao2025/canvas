package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;

/**
 * MarketingMonitorPollCommand 承载 domain.monitoring 场景中的不可变数据快照。
 * @param requestedFrom requestedFrom 字段。
 * @param requestedUntil requestedUntil 字段。
 * @param cursorOverride cursorOverride 字段。
 * @param maxItems maxItems 字段。
 * @param force force 字段。
 */
public record MarketingMonitorPollCommand(
        LocalDateTime requestedFrom,
        LocalDateTime requestedUntil,
        String cursorOverride,
        int maxItems,
        boolean force) {
}
