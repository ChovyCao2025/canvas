package org.chovy.canvas.cdp.api;

import java.time.LocalDateTime;

public record CdpWriteKeyView(
        Long writeKeyId,
        Long tenantId,
        String writeKey,
        String platform,
        Integer rateLimitPerMinute,
        LocalDateTime expiresAt) {
}
