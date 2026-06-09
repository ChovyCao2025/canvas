package org.chovy.canvas.domain.risk.graph;

/**
 * 风控图谱中的名单命中。
 *
 * @param listKey 名单业务键
 * @param subjectType 主体类型
 * @param subjectMasked 主体脱敏展示值
 * @param reason 命中原因
 */
public record RiskGraphListHit(
        String listKey,
        String subjectType,
        String subjectMasked,
        String reason
) {
}
