package org.chovy.canvas.domain.marketing;

import java.math.BigDecimal;
import java.util.Map;

public record GrowthTaskProgressCommand(
        Long taskId,
        Long participantId,
        BigDecimal deltaValue,
        String eventKey,
        Map<String, Object> evidence) {
}
