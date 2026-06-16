package org.chovy.canvas.cdp.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 承载 CdpUserIdentity 表的持久化字段。
 */
@TableName("cdp_user_identity")
public class CdpUserIdentityDO {

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
     * identity Type。
     */
    private String identityType;

    /**
     * identity Value。
     */
    private String identityValue;

    /**
     * 来源类型。
     */
    private String sourceType;

    /**
     * 来源引用标识。
     */
    private String sourceRefId;

    /**
     * verified。
     */
    private Integer verified;

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
     * 返回identity Type。
     */
    public String getIdentityType() {
        return identityType;
    }

    /**
     * 设置identity Type。
     */
    public void setIdentityType(String identityType) {
        this.identityType = identityType;
    }

    /**
     * 返回identity Value。
     */
    public String getIdentityValue() {
        return identityValue;
    }

    /**
     * 设置identity Value。
     */
    public void setIdentityValue(String identityValue) {
        this.identityValue = identityValue;
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
     * 返回verified。
     */
    public Integer getVerified() {
        return verified;
    }

    /**
     * 设置verified。
     */
    public void setVerified(Integer verified) {
        this.verified = verified;
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
