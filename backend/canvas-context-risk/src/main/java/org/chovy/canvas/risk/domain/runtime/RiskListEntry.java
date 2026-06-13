package org.chovy.canvas.risk.domain.runtime;

import java.time.Instant;

/**
 * 风控名单条目。
 *
 * @param tenantId 租户编号
 * @param listKey 名单业务键
 * @param subjectHash 主体哈希
 * @param subjectMasked 主体脱敏展示值
 * @param listType 名单类型
 * @param reason 入名单原因
 * @param effectiveFrom 生效时间
 * @param expiresAt 过期时间
 */
public record RiskListEntry(
        Long tenantId,
        String listKey,
        String subjectHash,
        String subjectMasked,
        RiskListType listType,
        String reason,
        Instant effectiveFrom,
        Instant expiresAt
) {

    /**
     * 返回替换生效窗口后的名单条目副本。
     */
    public RiskListEntry withEffectiveWindow(Instant newEffectiveFrom, Instant newExpiresAt) {
        return new RiskListEntry(
                tenantId,
                listKey,
                subjectHash,
                subjectMasked,
                listType,
                reason,
                newEffectiveFrom,
                newExpiresAt);
    }

    /**
     * 返回替换脱敏展示值后的名单条目副本。
     */
    public RiskListEntry withSubjectMasked(String newSubjectMasked) {
        return new RiskListEntry(
                tenantId,
                listKey,
                subjectHash,
                newSubjectMasked,
                listType,
                reason,
                effectiveFrom,
                expiresAt);
    }
}
