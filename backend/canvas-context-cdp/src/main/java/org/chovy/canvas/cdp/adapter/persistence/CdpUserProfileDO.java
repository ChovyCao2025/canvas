package org.chovy.canvas.cdp.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 承载 CdpUserProfile 表的持久化字段。
 */
@TableName("cdp_user_profile")
public class CdpUserProfileDO {

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
     * 展示名称。
     */
    private String displayName;

    /**
     * 手机号。
     */
    private String phone;

    /**
     * 邮箱。
     */
    private String email;

    /**
     * 状态。
     */
    private String status;

    /**
     * properties Json。
     */
    private String propertiesJson;

    /**
     * first Seen At。
     */
    private LocalDateTime firstSeenAt;

    /**
     * last Seen At。
     */
    private LocalDateTime lastSeenAt;

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
     * 返回展示名称。
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 设置展示名称。
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 返回手机号。
     */
    public String getPhone() {
        return phone;
    }

    /**
     * 设置手机号。
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * 返回邮箱。
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置邮箱。
     */
    public void setEmail(String email) {
        this.email = email;
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
     * 返回properties Json。
     */
    public String getPropertiesJson() {
        return propertiesJson;
    }

    /**
     * 设置properties Json。
     */
    public void setPropertiesJson(String propertiesJson) {
        this.propertiesJson = propertiesJson;
    }

    /**
     * 返回first Seen At。
     */
    public LocalDateTime getFirstSeenAt() {
        return firstSeenAt;
    }

    /**
     * 设置first Seen At。
     */
    public void setFirstSeenAt(LocalDateTime firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }

    /**
     * 返回last Seen At。
     */
    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    /**
     * 设置last Seen At。
     */
    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
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
