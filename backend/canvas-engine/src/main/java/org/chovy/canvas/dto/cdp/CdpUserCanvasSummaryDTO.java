package org.chovy.canvas.dto.cdp;

import java.time.LocalDateTime;

public record CdpUserCanvasSummaryDTO(
        Long canvasId,
        String canvasName,
        long executionCount,
        long successCount,
        long failedCount,
        String latestStatus,
        LocalDateTime firstEnteredAt,
        LocalDateTime lastEnteredAt
) {}
