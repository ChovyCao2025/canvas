package org.chovy.canvas.cdp.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 承载 CdpUserTag 表的持久化字段。
 */
@TableName("cdp_user_tag")
public class CdpUserTagDO {

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
     * 标签值。
     */
    private String tagValue;

    /**
     * 值类型。
     */
    private String valueType;

    /**
     * 来源类型。
     */
    private String sourceType;

    /**
     * 来源引用标识。
     */
    private String sourceRefId;

    /**
     * 状态。
     */
    private String status;

    /**
     * effective At。
     */
    private LocalDateTime effectiveAt;

    /**
     * expires At。
     */
    private LocalDateTime expiresAt;

    /**
     * 创建人。
     */
    private String createdBy;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;

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
     * 返回标签值。
     */
    public String getTagValue() {
        return tagValue;
    }

    /**
     * 设置标签值。
     */
    public void setTagValue(String tagValue) {
        this.tagValue = tagValue;
    }

    /**
     * 返回值类型。
     */
    public String getValueType() {
        return valueType;
    }

    /**
     * 设置值类型。
     */
    public void setValueType(String valueType) {
        this.valueType = valueType;
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
     * 返回状态。
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置状态。
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 返回effective At。
     */
    public LocalDateTime getEffectiveAt() {
        return effectiveAt;
    }

    /**
     * 设置effective At。
     */
    public void setEffectiveAt(LocalDateTime effectiveAt) {
        this.effectiveAt = effectiveAt;
    }

    /**
     * 返回expires At。
     */
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    /**
     * 设置expires At。
     */
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    /**
     * 返回创建人。
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * 设置创建人。
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * 返回创建时间。
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间。
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 返回更新时间。
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置更新时间。
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
