package org.chovy.canvas.cdp.api;

import java.time.LocalDateTime;

public record CdpUserTagView(
        Long id,
        Long tenantId,
        String userId,
        String tagCode,
        String tagValue,
        String valueType,
        String sourceType,
        String status,
        LocalDateTime effectiveAt,
        LocalDateTime expiresAt,
        LocalDateTime updatedAt) {
}
