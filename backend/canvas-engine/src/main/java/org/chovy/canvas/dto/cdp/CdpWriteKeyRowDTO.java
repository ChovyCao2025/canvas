package org.chovy.canvas.dto.cdp;

import java.time.LocalDateTime;

public record CdpWriteKeyRowDTO(
        Long id,
        String name,
        String keyPrefix,
        String platform,
        String status,
        Integer rateLimitQps,
        Long dailyQuota,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
