package org.chovy.canvas.domain.risk.governance;

/**
 * 风控名单命中视图。
 *
 * @param tenantId 租户编号
 * @param listKey 名单业务键
 * @param subjectHash 主体哈希
 * @param subjectMasked 主体脱敏展示值
 * @param decisionRunId 关联决策运行编号
 */
public record RiskListHitView(
        Long tenantId,
        String listKey,
        String subjectHash,
        String subjectMasked,
        String decisionRunId
) {

    /**
     * 返回不暴露原始主体的调试字符串。
     */
    @Override
    public String toString() {
        return "RiskListHitView[tenantId=" + tenantId
                + ", listKey=" + listKey
                + ", subjectHashPresent=" + (subjectHash != null)
                + ", subjectMasked=" + subjectMasked
                + ", decisionRunId=" + decisionRunId + "]";
    }
}
