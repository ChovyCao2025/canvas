package org.chovy.canvas.cdp.api;

import java.time.LocalDateTime;

public record CdpUserTagHistoryView(
        Long tenantId,
        String userId,
        String tagCode,
        String oldValue,
        String newValue,
        String operation,
        String sourceType,
        String sourceRefId,
        String reason,
        String operator,
        LocalDateTime operatedAt) {
}
