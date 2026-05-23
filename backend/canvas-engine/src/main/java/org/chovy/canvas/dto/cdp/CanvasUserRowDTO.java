package org.chovy.canvas.dto.cdp;

import java.time.LocalDateTime;
import java.util.List;

public record CanvasUserRowDTO(
        String userId,
        String displayName,
        long executionCount,
        long successCount,
        long failedCount,
        String latestStatus,
        LocalDateTime firstEnteredAt,
        LocalDateTime lastEnteredAt,
        List<CdpUserTagDTO> tags
) {}
