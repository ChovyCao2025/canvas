package org.chovy.canvas.dto.cdp;

public record CdpWriteKeyCreateResp(
        Long id,
        String name,
        String writeKey,
        String keyPrefix,
        String platform,
        Integer rateLimitQps,
        Long dailyQuota
) {
}
