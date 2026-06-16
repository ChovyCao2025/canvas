package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;

/**
 * 表示 CdpUserTag 的业务数据或处理组件。
 */
public final class CdpUserTag {

    /**
     * 唯一标识。
     */
    private final Long id;

    /**
     * 租户标识。
     */
    private final Long tenantId;

    /**
     * 用户标识。
     */
    private final String userId;

    /**
     * 标签编码。
     */
    private final String tagCode;

    /**
     * 标签值。
     */
    private final String tagValue;

    /**
     * 值类型。
     */
    private final String valueType;

    /**
     * 来源类型。
     */
    private final String sourceType;

    /**
     * 来源引用标识。
     */
    private final String sourceRefId;

    /**
     * 状态。
     */
    private final String status;

    /**
     * effective At。
     */
    private final LocalDateTime effectiveAt;

    /**
     * expires At。
     */
    private final LocalDateTime expiresAt;

    /**
     * 创建人。
     */
    private final String createdBy;

    /**
     * 创建时间。
     */
    private final LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private final LocalDateTime updatedAt;

    /**
     * 使用记录字段创建 CdpUserTag。
     */
    public CdpUserTag(
            Long id,
            Long tenantId,
            String userId,
            String tagCode,
            String tagValue,
            String valueType,
            String sourceType,
            String sourceRefId,
            String status,
            LocalDateTime effectiveAt,
            LocalDateTime expiresAt,
            String createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.userId = userId;
        this.tagCode = tagCode;
        this.tagValue = tagValue;
        this.valueType = valueType;
        this.sourceType = sourceType;
        this.sourceRefId = sourceRefId;
        this.status = status;
        this.effectiveAt = effectiveAt;
        this.expiresAt = expiresAt;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

/**
 * 返回替换Id后的副本。
 */
public CdpUserTag withId(Long newId) {
        return new CdpUserTag(newId, tenantId, userId, tagCode, tagValue, valueType, sourceType, sourceRefId, status,
                effectiveAt, expiresAt, createdBy, createdAt, updatedAt);
    }

    /**
     * 返回替换Status后的副本。
     */
    public CdpUserTag withStatus(String newStatus) {
        return new CdpUserTag(id, tenantId, userId, tagCode, tagValue, valueType, sourceType, sourceRefId, newStatus,
                effectiveAt, expiresAt, createdBy, createdAt, updatedAt);
    }

    /**
     * 返回唯一标识。
     */
    public Long id() {
        return id;
    }

    /**
     * 返回租户标识。
     */
    public Long tenantId() {
        return tenantId;
    }

    /**
     * 返回用户标识。
     */
    public String userId() {
        return userId;
    }

    /**
     * 返回标签编码。
     */
    public String tagCode() {
        return tagCode;
    }

    /**
     * 返回标签值。
     */
    public String tagValue() {
        return tagValue;
    }

    /**
     * 返回值类型。
     */
    public String valueType() {
        return valueType;
    }

    /**
     * 返回来源类型。
     */
    public String sourceType() {
        return sourceType;
    }

    /**
     * 返回来源引用标识。
     */
    public String sourceRefId() {
        return sourceRefId;
    }

    /**
     * 返回状态。
     */
    public String status() {
        return status;
    }

    /**
     * 返回effective At。
     */
    public LocalDateTime effectiveAt() {
        return effectiveAt;
    }

    /**
     * 返回expires At。
     */
    public LocalDateTime expiresAt() {
        return expiresAt;
    }

    /**
     * 返回创建人。
     */
    public String createdBy() {
        return createdBy;
    }

    /**
     * 返回创建时间。
     */
    public LocalDateTime createdAt() {
        return createdAt;
    }

    /**
     * 返回更新时间。
     */
    public LocalDateTime updatedAt() {
        return updatedAt;
    }

    /**
     * 按所有字段比较 CdpUserTag。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CdpUserTag that = (CdpUserTag) o;
        return java.util.Objects.equals(id, that.id)
                && java.util.Objects.equals(tenantId, that.tenantId)
                && java.util.Objects.equals(userId, that.userId)
                && java.util.Objects.equals(tagCode, that.tagCode)
                && java.util.Objects.equals(tagValue, that.tagValue)
                && java.util.Objects.equals(valueType, that.valueType)
                && java.util.Objects.equals(sourceType, that.sourceType)
                && java.util.Objects.equals(sourceRefId, that.sourceRefId)
                && java.util.Objects.equals(status, that.status)
                && java.util.Objects.equals(effectiveAt, that.effectiveAt)
                && java.util.Objects.equals(expiresAt, that.expiresAt)
                && java.util.Objects.equals(createdBy, that.createdBy)
                && java.util.Objects.equals(createdAt, that.createdAt)
                && java.util.Objects.equals(updatedAt, that.updatedAt);
    }

    /**
     * 根据所有字段计算 CdpUserTag 的哈希值。
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, tenantId, userId, tagCode, tagValue, valueType, sourceType, sourceRefId, status, effectiveAt, expiresAt, createdBy, createdAt, updatedAt);
    }

    /**
     * 返回与记录结构一致的调试字符串。
     */
    @Override
    public String toString() {
        return "CdpUserTag[" + "id=" + id + ", tenantId=" + tenantId + ", userId=" + userId + ", tagCode=" + tagCode + ", tagValue=" + tagValue + ", valueType=" + valueType + ", sourceType=" + sourceType + ", sourceRefId=" + sourceRefId + ", status=" + status + ", effectiveAt=" + effectiveAt + ", expiresAt=" + expiresAt + ", createdBy=" + createdBy + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + "]";
    }
}
