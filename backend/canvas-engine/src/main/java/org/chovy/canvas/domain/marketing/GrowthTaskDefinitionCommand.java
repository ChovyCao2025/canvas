package org.chovy.canvas.domain.marketing;

import java.math.BigDecimal;
import java.util.Map;

public record GrowthTaskDefinitionCommand(
        String taskKey,
        String taskType,
        String completionPolicy,
        String resetPolicy,
        Long rewardPoolId,
        BigDecimal targetValue,
        String status,
        Map<String, Object> rule) {
}
