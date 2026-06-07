package org.chovy.canvas.domain.marketing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record GrowthTaskProgressView(
        Long id,
        Long tenantId,
        Long activityId,
        Long participantId,
        Long taskId,
        BigDecimal progressValue,
        BigDecimal targetValue,
        String status,
        String lastEventKey,
        Map<String, Object> evidence,
        Long rewardGrantId,
        String updatedBy,
        LocalDateTime completedAt,
        LocalDateTime updatedAt) {
}
