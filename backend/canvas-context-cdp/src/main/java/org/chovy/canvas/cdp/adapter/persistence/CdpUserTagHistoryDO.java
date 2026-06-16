package org.chovy.canvas.cdp.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 承载 CdpUserTagHistory 表的持久化字段。
 */
@TableName("cdp_user_tag_history")
public class CdpUserTagHistoryDO {

    /**
     * 唯一标识。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户标识。
     */
    private Long tenantId;

    /**
     * 用户标识。
     */
    private String userId;

    /**
     * 标签编码。
     */
    private String tagCode;

    /**
     * old Value。
     */
    private String oldValue;

    /**
     * new Value。
     */
    private String newValue;

    /**
     * operation。
     */
    private String operation;

    /**
     * 来源类型。
     */
    private String sourceType;

    /**
     * 来源引用标识。
     */
    private String sourceRefId;

    /**
     * 幂等键。
     */
    private String idempotencyKey;

    /**
     * 原因。
     */
    private String reason;

    /**
     * 操作人。
     */
    private String operator;

    /**
     * operated At。
     */
    private LocalDateTime operatedAt;

    /**
     * 返回唯一标识。
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置唯一标识。
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 返回租户标识。
     */
    public Long getTenantId() {
        return tenantId;
    }

    /**
     * 设置租户标识。
     */
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * 返回用户标识。
     */
    public String getUserId() {
        return userId;
    }

    /**
     * 设置用户标识。
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * 返回标签编码。
     */
    public String getTagCode() {
        return tagCode;
    }

    /**
     * 设置标签编码。
     */
    public void setTagCode(String tagCode) {
        this.tagCode = tagCode;
    }

    /**
     * 返回old Value。
     */
    public String getOldValue() {
        return oldValue;
    }

    /**
     * 设置old Value。
     */
    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    /**
     * 返回new Value。
     */
    public String getNewValue() {
        return newValue;
    }

    /**
     * 设置new Value。
     */
    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    /**
     * 返回operation。
     */
    public String getOperation() {
        return operation;
    }

    /**
     * 设置operation。
     */
    public void setOperation(String operation) {
        this.operation = operation;
    }

    /**
     * 返回来源类型。
     */
    public String getSourceType() {
        return sourceType;
    }

    /**
     * 设置来源类型。
     */
    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    /**
     * 返回来源引用标识。
     */
    public String getSourceRefId() {
        return sourceRefId;
    }

    /**
     * 设置来源引用标识。
     */
    public void setSourceRefId(String sourceRefId) {
        this.sourceRefId = sourceRefId;
    }

    /**
     * 返回幂等键。
     */
    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    /**
     * 设置幂等键。
     */
    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    /**
     * 返回原因。
     */
    public String getReason() {
        return reason;
    }

    /**
     * 设置原因。
     */
    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * 返回操作人。
     */
    public String getOperator() {
        return operator;
    }

    /**
     * 设置操作人。
     */
    public void setOperator(String operator) {
        this.operator = operator;
    }

    /**
     * 返回operated At。
     */
    public LocalDateTime getOperatedAt() {
        return operatedAt;
    }

    /**
     * 设置operated At。
     */
    public void setOperatedAt(LocalDateTime operatedAt) {
        this.operatedAt = operatedAt;
    }
}
