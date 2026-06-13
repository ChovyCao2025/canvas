package org.chovy.canvas.cdp.api;

import java.time.LocalDateTime;

public record CdpTagWriteCommand(
        String tagCode,
        String tagValue,
        String reason,
        LocalDateTime expiresAt,
        String sourceType,
        String sourceRefId,
        String operator,
        String idempotencyKey) {
}
