package org.chovy.canvas.domain.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * AiDecisionRecomputeCommand 承载 domain.ai 场景中的不可变数据快照。
 * @param runDate runDate 字段。
 * @param decisionScope decisionScope 字段。
 * @param userIds userIds 字段。
 * @param force force 字段。
 * @param budgetCap budgetCap 字段。
 * @param metadata metadata 字段。
 */
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
