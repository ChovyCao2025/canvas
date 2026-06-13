package org.chovy.canvas.cdp.api;

import java.time.LocalDateTime;
import java.util.Map;

public record CdpCustomerProfileView(
        Long id,
        Long tenantId,
        String userId,
        String displayName,
        String phone,
        String email,
        String status,
        Map<String, Object> properties,
        LocalDateTime firstSeenAt,
        LocalDateTime lastSeenAt) {
}
