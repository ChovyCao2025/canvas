package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;

public record CdpUserTagHistory(
        Long tenantId,
        String userId,
        String tagCode,
        String oldValue,
        String newValue,
        String operation,
        String sourceType,
        String sourceRefId,
        String idempotencyKey,
        String reason,
        String operator,
        LocalDateTime operatedAt) {
}
