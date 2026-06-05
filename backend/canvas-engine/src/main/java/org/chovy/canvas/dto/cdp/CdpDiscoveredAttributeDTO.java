package org.chovy.canvas.dto.cdp;

import java.time.LocalDateTime;

public record CdpDiscoveredAttributeDTO(
        Long id,
        String eventCode,
        String attrName,
        String attrType,
        String status,
        String sampleValue,
        LocalDateTime firstSeenAt,
        LocalDateTime lastSeenAt
) {
}
