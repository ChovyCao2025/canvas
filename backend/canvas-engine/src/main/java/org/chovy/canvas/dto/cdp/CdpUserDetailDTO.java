package org.chovy.canvas.dto.cdp;

import java.time.LocalDateTime;

public record CdpUserDetailDTO(
        String userId,
        String displayName,
        String phone,
        String email,
        String status,
        String propertiesJson,
        LocalDateTime firstSeenAt,
        LocalDateTime lastSeenAt
) {}
