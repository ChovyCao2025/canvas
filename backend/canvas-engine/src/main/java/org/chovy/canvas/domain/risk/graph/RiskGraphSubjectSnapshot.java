package org.chovy.canvas.domain.risk.graph;

import java.time.Instant;
import java.util.Map;

/**
 * 风控图谱主体快照。
 *
 * @param tenantId 租户编号
 * @param subjectId 主体编号
 * @param decisionRunId 关联决策运行编号
 * @param observedAt 观测时间
 * @param identifiers 标准化主体标识集合
 */
public record RiskGraphSubjectSnapshot(
        Long tenantId,
        String subjectId,
        String decisionRunId,
        Instant observedAt,
        Map<String, String> identifiers
) {
}
