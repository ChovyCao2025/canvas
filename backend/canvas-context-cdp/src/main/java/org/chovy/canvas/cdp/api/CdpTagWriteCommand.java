package org.chovy.canvas.cdp.api;

import java.time.LocalDateTime;

/**
 * 表示 CdpTagWriteCommand 的业务数据或处理组件。
 */
public final class CdpTagWriteCommand {

    /**
     * 标签编码。
     */
    private final String tagCode;

    /**
     * 标签值。
     */
    private final String tagValue;

    /**
     * 原因。
     */
    private final String reason;

    /**
     * expires At。
     */
    private final LocalDateTime expiresAt;

    /**
     * 来源类型。
     */
    private final String sourceType;

    /**
     * 来源引用标识。
     */
    private final String sourceRefId;

    /**
     * operator。
     */
    private final String operator;

    /**
     * 幂等键。
     */
    private final String idempotencyKey;

    /**
     * 使用记录字段创建 CdpTagWriteCommand。
     */
    public CdpTagWriteCommand(
            String tagCode,
            String tagValue,
            String reason,
            LocalDateTime expiresAt,
            String sourceType,
            String sourceRefId,
            String operator,
            String idempotencyKey) {
        this.tagCode = tagCode;
        this.tagValue = tagValue;
        this.reason = reason;
        this.expiresAt = expiresAt;
        this.sourceType = sourceType;
        this.sourceRefId = sourceRefId;
        this.operator = operator;
        this.idempotencyKey = idempotencyKey;
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
     * 返回原因。
     */
    public String reason() {
        return reason;
    }

    /**
     * 返回expires At。
     */
    public LocalDateTime expiresAt() {
        return expiresAt;
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
     * 返回operator。
     */
    public String operator() {
        return operator;
    }

    /**
     * 返回幂等键。
     */
    public String idempotencyKey() {
        return idempotencyKey;
    }

    /**
     * 按所有字段比较 CdpTagWriteCommand。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CdpTagWriteCommand that = (CdpTagWriteCommand) o;
        return java.util.Objects.equals(tagCode, that.tagCode)
                && java.util.Objects.equals(tagValue, that.tagValue)
                && java.util.Objects.equals(reason, that.reason)
                && java.util.Objects.equals(expiresAt, that.expiresAt)
                && java.util.Objects.equals(sourceType, that.sourceType)
                && java.util.Objects.equals(sourceRefId, that.sourceRefId)
                && java.util.Objects.equals(operator, that.operator)
                && java.util.Objects.equals(idempotencyKey, that.idempotencyKey);
    }

    /**
     * 根据所有字段计算 CdpTagWriteCommand 的哈希值。
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(tagCode, tagValue, reason, expiresAt, sourceType, sourceRefId, operator, idempotencyKey);
    }

    /**
     * 返回与记录结构一致的调试字符串。
     */
    @Override
    public String toString() {
        return "CdpTagWriteCommand[" + "tagCode=" + tagCode + ", tagValue=" + tagValue + ", reason=" + reason + ", expiresAt=" + expiresAt + ", sourceType=" + sourceType + ", sourceRefId=" + sourceRefId + ", operator=" + operator + ", idempotencyKey=" + idempotencyKey + "]";
    }
}
