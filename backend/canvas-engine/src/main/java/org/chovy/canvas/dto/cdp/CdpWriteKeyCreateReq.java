package org.chovy.canvas.dto.cdp;

public record CdpWriteKeyCreateReq(
        String name,
        String platform,
        Integer rateLimitQps,
        Long dailyQuota,
        String description
) {
}
