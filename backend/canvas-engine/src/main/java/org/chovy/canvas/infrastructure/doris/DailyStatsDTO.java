package org.chovy.canvas.infrastructure.doris;

import java.time.LocalDate;

/**
 * Doris DWS 层每日画布执行统计行。
 */
public record DailyStatsDTO(
        LocalDate statDate,
        Long canvasId,
        String canvasName,
        String triggerType,
        Long totalExecutions,
        Long successCount,
        Long failCount,
        Long runningCount,
        Long uniqueUsers,
        Long avgDurationMs
) {
}
