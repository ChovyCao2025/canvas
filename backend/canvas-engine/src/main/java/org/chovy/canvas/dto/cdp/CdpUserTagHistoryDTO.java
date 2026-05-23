package org.chovy.canvas.dto.cdp;

import java.time.LocalDateTime;

public record CdpUserTagHistoryDTO(
        String tagCode,
        String oldValue,
        String newValue,
        String operation,
        String sourceType,
        String sourceRefId,
        String reason,
        String operator,
        LocalDateTime operatedAt
) {}
