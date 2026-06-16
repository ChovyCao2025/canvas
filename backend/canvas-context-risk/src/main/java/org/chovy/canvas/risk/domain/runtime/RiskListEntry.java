package org.chovy.canvas.risk.domain.runtime;

import java.time.Instant;
import java.util.Objects;

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
public final class RiskListEntry {

    /**
     * RiskListEntry 的 tenantId 字段。
     */
    private final Long tenantId;


    /**
     * RiskListEntry 的 listKey 字段。
     */
    private final String listKey;


    /**
     * RiskListEntry 的 subjectHash 字段。
     */
    private final String subjectHash;


    /**
     * RiskListEntry 的 subjectMasked 字段。
     */
    private final String subjectMasked;


    /**
     * RiskListEntry 的 listType 字段。
     */
    private final RiskListType listType;


    /**
     * RiskListEntry 的 reason 字段。
     */
    private final String reason;


    /**
     * RiskListEntry 的 effectiveFrom 字段。
     */
    private final Instant effectiveFrom;


    /**
     * RiskListEntry 的 expiresAt 字段。
     */
    private final Instant expiresAt;


    /**
     * 创建 RiskListEntry。
     *
     * @param tenantId RiskListEntry 的 tenantId 字段
     * @param listKey RiskListEntry 的 listKey 字段
     * @param subjectHash RiskListEntry 的 subjectHash 字段
     * @param subjectMasked RiskListEntry 的 subjectMasked 字段
     * @param listType RiskListEntry 的 listType 字段
     * @param reason RiskListEntry 的 reason 字段
     * @param effectiveFrom RiskListEntry 的 effectiveFrom 字段
     * @param expiresAt RiskListEntry 的 expiresAt 字段
     */
    public RiskListEntry(Long tenantId, String listKey, String subjectHash, String subjectMasked, RiskListType listType, String reason, Instant effectiveFrom, Instant expiresAt) {
        this.tenantId = tenantId;
        this.listKey = listKey;
        this.subjectHash = subjectHash;
        this.subjectMasked = subjectMasked;
        this.listType = listType;
        this.reason = reason;
        this.effectiveFrom = effectiveFrom;
        this.expiresAt = expiresAt;
    }

    /**
     * 返回 RiskListEntry 的 tenantId 字段。
     *
     * @return tenantId 字段值
     */
    public Long tenantId() {
        return tenantId;
    }

    /**
     * 返回 RiskListEntry 的 listKey 字段。
     *
     * @return listKey 字段值
     */
    public String listKey() {
        return listKey;
    }

    /**
     * 返回 RiskListEntry 的 subjectHash 字段。
     *
     * @return subjectHash 字段值
     */
    public String subjectHash() {
        return subjectHash;
    }

    /**
     * 返回 RiskListEntry 的 subjectMasked 字段。
     *
     * @return subjectMasked 字段值
     */
    public String subjectMasked() {
        return subjectMasked;
    }

    /**
     * 返回 RiskListEntry 的 listType 字段。
     *
     * @return listType 字段值
     */
    public RiskListType listType() {
        return listType;
    }

    /**
     * 返回 RiskListEntry 的 reason 字段。
     *
     * @return reason 字段值
     */
    public String reason() {
        return reason;
    }

    /**
     * 返回 RiskListEntry 的 effectiveFrom 字段。
     *
     * @return effectiveFrom 字段值
     */
    public Instant effectiveFrom() {
        return effectiveFrom;
    }

    /**
     * 返回 RiskListEntry 的 expiresAt 字段。
     *
     * @return expiresAt 字段值
     */
    public Instant expiresAt() {
        return expiresAt;
    }

    /**
     * 比较当前 RiskListEntry 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskListEntry other)) {
            return false;
        }
        return Objects.equals(tenantId, other.tenantId)
                && Objects.equals(listKey, other.listKey)
                && Objects.equals(subjectHash, other.subjectHash)
                && Objects.equals(subjectMasked, other.subjectMasked)
                && Objects.equals(listType, other.listType)
                && Objects.equals(reason, other.reason)
                && Objects.equals(effectiveFrom, other.effectiveFrom)
                && Objects.equals(expiresAt, other.expiresAt);
    }

    /**
     * 计算 RiskListEntry 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(tenantId, listKey, subjectHash, subjectMasked, listType, reason, effectiveFrom, expiresAt);
    }

    /**
     * 返回 RiskListEntry 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskListEntry[tenantId=" + tenantId + ", listKey=" + listKey + ", subjectHash=" + subjectHash + ", subjectMasked=" + subjectMasked + ", listType=" + listType + ", reason=" + reason + ", effectiveFrom=" + effectiveFrom + ", expiresAt=" + expiresAt + "]";
    }

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
