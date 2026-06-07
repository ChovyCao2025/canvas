package org.chovy.canvas.domain.marketing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record GrowthTaskDefinitionView(
        Long id,
        Long tenantId,
        Long activityId,
        String taskKey,
        String taskType,
        String completionPolicy,
        String resetPolicy,
        Long rewardPoolId,
        BigDecimal targetValue,
        String status,
        Map<String, Object> rule,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
