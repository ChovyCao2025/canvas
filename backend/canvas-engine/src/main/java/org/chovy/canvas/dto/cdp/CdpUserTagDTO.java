package org.chovy.canvas.dto.cdp;

import java.time.LocalDateTime;

public record CdpUserTagDTO(
        String tagCode,
        String tagName,
        String tagValue,
        String valueType,
        String sourceType,
        String status,
        LocalDateTime effectiveAt,
        LocalDateTime expiresAt,
        LocalDateTime updatedAt
) {}
