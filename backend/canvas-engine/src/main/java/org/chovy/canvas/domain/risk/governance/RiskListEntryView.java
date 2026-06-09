package org.chovy.canvas.domain.risk.governance;

import java.time.Instant;

/**
 * 风控名单条目视图。
 *
 * @param id 条目编号
 * @param tenantId 租户编号
 * @param listKey 名单业务键
 * @param subjectHash 主体哈希
 * @param subjectMasked 主体脱敏展示值
 * @param reason 入名单原因
 * @param source 条目来源
 * @param effectiveFrom 生效时间
 * @param expiresAt 过期时间
 * @param createdBy 创建人
 */
public record RiskListEntryView(
        long id,
        Long tenantId,
        String listKey,
        String subjectHash,
        String subjectMasked,
        String reason,
        String source,
        Instant effectiveFrom,
        Instant expiresAt,
        String createdBy
) {

    /**
     * 返回不暴露原始主体的调试字符串。
     */
    @Override
    public String toString() {
        return "RiskListEntryView[id=" + id
                + ", tenantId=" + tenantId
                + ", listKey=" + listKey
                + ", subjectHashPresent=" + (subjectHash != null)
                + ", subjectMasked=" + subjectMasked
                + ", reason=" + reason
                + ", source=" + source
                + ", effectiveFrom=" + effectiveFrom
                + ", expiresAt=" + expiresAt
                + ", createdBy=" + createdBy + "]";
    }
}
