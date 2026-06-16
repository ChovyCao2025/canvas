package org.chovy.canvas.cdp.api;

import java.time.LocalDateTime;

/**
 * 表示 CdpUserTagHistoryView 的业务数据或处理组件。
 */
public final class CdpUserTagHistoryView {

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
     * old Value。
     */
    private final String oldValue;

    /**
     * new Value。
     */
    private final String newValue;

    /**
     * 操作类型。
     */
    private final String operation;

    /**
     * 来源类型。
     */
    private final String sourceType;

    /**
     * 来源引用标识。
     */
    private final String sourceRefId;

    /**
     * 原因。
     */
    private final String reason;

    /**
     * operator。
     */
    private final String operator;

    /**
     * operated At。
     */
    private final LocalDateTime operatedAt;

    /**
     * 使用记录字段创建 CdpUserTagHistoryView。
     */
    public CdpUserTagHistoryView(
            Long tenantId,
            String userId,
            String tagCode,
            String oldValue,
            String newValue,
            String operation,
            String sourceType,
            String sourceRefId,
            String reason,
            String operator,
            LocalDateTime operatedAt) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.tagCode = tagCode;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.operation = operation;
        this.sourceType = sourceType;
        this.sourceRefId = sourceRefId;
        this.reason = reason;
        this.operator = operator;
        this.operatedAt = operatedAt;
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
     * 返回old Value。
     */
    public String oldValue() {
        return oldValue;
    }

    /**
     * 返回new Value。
     */
    public String newValue() {
        return newValue;
    }

    /**
     * 返回操作类型。
     */
    public String operation() {
        return operation;
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
     * 返回原因。
     */
    public String reason() {
        return reason;
    }

    /**
     * 返回operator。
     */
    public String operator() {
        return operator;
    }

    /**
     * 返回operated At。
     */
    public LocalDateTime operatedAt() {
        return operatedAt;
    }

    /**
     * 按所有字段比较 CdpUserTagHistoryView。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CdpUserTagHistoryView that = (CdpUserTagHistoryView) o;
        return java.util.Objects.equals(tenantId, that.tenantId)
                && java.util.Objects.equals(userId, that.userId)
                && java.util.Objects.equals(tagCode, that.tagCode)
                && java.util.Objects.equals(oldValue, that.oldValue)
                && java.util.Objects.equals(newValue, that.newValue)
                && java.util.Objects.equals(operation, that.operation)
                && java.util.Objects.equals(sourceType, that.sourceType)
                && java.util.Objects.equals(sourceRefId, that.sourceRefId)
                && java.util.Objects.equals(reason, that.reason)
                && java.util.Objects.equals(operator, that.operator)
                && java.util.Objects.equals(operatedAt, that.operatedAt);
    }

    /**
     * 根据所有字段计算 CdpUserTagHistoryView 的哈希值。
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(tenantId, userId, tagCode, oldValue, newValue, operation, sourceType, sourceRefId, reason, operator, operatedAt);
    }

    /**
     * 返回与记录结构一致的调试字符串。
     */
    @Override
    public String toString() {
        return "CdpUserTagHistoryView[" + "tenantId=" + tenantId + ", userId=" + userId + ", tagCode=" + tagCode + ", oldValue=" + oldValue + ", newValue=" + newValue + ", operation=" + operation + ", sourceType=" + sourceType + ", sourceRefId=" + sourceRefId + ", reason=" + reason + ", operator=" + operator + ", operatedAt=" + operatedAt + "]";
    }
}
