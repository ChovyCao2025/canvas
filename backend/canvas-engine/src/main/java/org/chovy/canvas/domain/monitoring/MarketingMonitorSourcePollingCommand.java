package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;

/**
 * MarketingMonitorSourcePollingCommand 承载 domain.monitoring 场景中的不可变数据快照。
 * @param pollEnabled pollEnabled 字段。
 * @param pollIntervalMinutes pollIntervalMinutes 字段。
 * @param pollCursor pollCursor 字段。
 * @param nextPollAt nextPollAt 字段。
 */
public record MarketingMonitorSourcePollingCommand(
        Boolean pollEnabled,
        Integer pollIntervalMinutes,
        String pollCursor,
        LocalDateTime nextPollAt) {
}
