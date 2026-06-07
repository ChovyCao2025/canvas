package org.chovy.canvas.domain.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record AiDecisionRecomputeCommand(
        LocalDate runDate,
        String decisionScope,
        List<String> userIds,
        boolean force,
        BigDecimal budgetCap,
        Map<String, Object> metadata) {

    public AiDecisionRecomputeCommand {
        userIds = userIds == null ? List.of() : List.copyOf(userIds);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
