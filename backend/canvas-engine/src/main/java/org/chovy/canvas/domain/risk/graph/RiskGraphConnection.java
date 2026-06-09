package org.chovy.canvas.domain.risk.graph;

import java.util.Map;

/**
 * 风控图谱中的主体连接。
 *
 * @param subjectId 相邻主体编号
 * @param decisionRunId 关联决策运行编号
 * @param sharedIdentifiers 共享标识映射
 */
public record RiskGraphConnection(
        String subjectId,
        String decisionRunId,
        Map<String, String> sharedIdentifiers
) {
}
