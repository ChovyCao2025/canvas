package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;

/**
 * MarketingMonitorSourcePollingView 承载 domain.monitoring 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param sourceId sourceId 字段。
 * @param sourceKey sourceKey 字段。
 * @param sourceType sourceType 字段。
 * @param pollEnabled pollEnabled 字段。
 * @param pollIntervalMinutes pollIntervalMinutes 字段。
 * @param pollCursor pollCursor 字段。
 * @param lastPolledAt lastPolledAt 字段。
 * @param nextPollAt nextPollAt 字段。
 * @param lastPollStatus lastPollStatus 字段。
 * @param updatedAt updatedAt 字段。
 */
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
