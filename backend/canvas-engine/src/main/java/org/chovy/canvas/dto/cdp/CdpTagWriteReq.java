package org.chovy.canvas.dto.cdp;

import java.time.LocalDateTime;

public record CdpTagWriteReq(
        String tagCode,
        String tagValue,
        String reason,
        LocalDateTime expiresAt,
        String sourceType,
        String sourceRefId,
        String operator,
        String idempotencyKey
) {}
