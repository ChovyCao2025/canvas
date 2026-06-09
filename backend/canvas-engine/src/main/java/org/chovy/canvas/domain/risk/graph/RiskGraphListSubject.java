package org.chovy.canvas.domain.risk.graph;

/**
 * 风控图谱使用的名单主体索引。
 *
 * @param tenantId 租户编号
 * @param listKey 名单业务键
 * @param subjectType 主体类型
 * @param subjectValue 主体标准化值
 * @param subjectMasked 主体脱敏展示值
 * @param reason 入名单原因
 */
public record RiskGraphListSubject(
        Long tenantId,
        String listKey,
        String subjectType,
        String subjectValue,
        String subjectMasked,
        String reason
) {
}
