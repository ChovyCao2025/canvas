package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 表示 CustomerProfile 的业务数据或处理组件。
 */
public final class CustomerProfile {

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
     * 展示名称。
     */
    private final String displayName;

    /**
     * 手机号。
     */
    private final String phone;

    /**
     * 邮箱。
     */
    private final String email;

    /**
     * 状态。
     */
    private final String status;

    /**
     * 扩展属性。
     */
    private final Map<String, Object> properties;

    /**
     * 首次出现时间。
     */
    private final LocalDateTime firstSeenAt;

    /**
     * 最近出现时间。
     */
    private final LocalDateTime lastSeenAt;

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
     * 使用记录字段创建 CustomerProfile。
     */
    public CustomerProfile(
            Long id,
            Long tenantId,
            String userId,
            String displayName,
            String phone,
            String email,
            String status,
            Map<String, Object> properties,
            LocalDateTime firstSeenAt,
            LocalDateTime lastSeenAt,
            String createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.userId = userId;
        this.displayName = displayName;
        this.phone = phone;
        this.email = email;
        this.status = status;
        this.properties = properties;
        this.firstSeenAt = firstSeenAt;
        this.lastSeenAt = lastSeenAt;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

/**
 * 返回替换Id后的副本。
 */
public CustomerProfile withId(Long newId) {
        return new CustomerProfile(newId, tenantId, userId, displayName, phone, email, status, properties,
                firstSeenAt, lastSeenAt, createdBy, createdAt, updatedAt);
    }

    /**
     * 返回替换Seen At后的副本。
     */
    public CustomerProfile withSeenAt(LocalDateTime firstSeen, LocalDateTime lastSeen) {
        return new CustomerProfile(id, tenantId, userId, displayName, phone, email, status, properties,
                firstSeen, lastSeen, createdBy, createdAt, updatedAt);
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
     * 返回展示名称。
     */
    public String displayName() {
        return displayName;
    }

    /**
     * 返回手机号。
     */
    public String phone() {
        return phone;
    }

    /**
     * 返回邮箱。
     */
    public String email() {
        return email;
    }

    /**
     * 返回状态。
     */
    public String status() {
        return status;
    }

    /**
     * 返回扩展属性。
     */
    public Map<String, Object> properties() {
        return properties;
    }

    /**
     * 返回首次出现时间。
     */
    public LocalDateTime firstSeenAt() {
        return firstSeenAt;
    }

    /**
     * 返回最近出现时间。
     */
    public LocalDateTime lastSeenAt() {
        return lastSeenAt;
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
     * 按所有字段比较 CustomerProfile。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CustomerProfile that = (CustomerProfile) o;
        return java.util.Objects.equals(id, that.id)
                && java.util.Objects.equals(tenantId, that.tenantId)
                && java.util.Objects.equals(userId, that.userId)
                && java.util.Objects.equals(displayName, that.displayName)
                && java.util.Objects.equals(phone, that.phone)
                && java.util.Objects.equals(email, that.email)
                && java.util.Objects.equals(status, that.status)
                && java.util.Objects.equals(properties, that.properties)
                && java.util.Objects.equals(firstSeenAt, that.firstSeenAt)
                && java.util.Objects.equals(lastSeenAt, that.lastSeenAt)
                && java.util.Objects.equals(createdBy, that.createdBy)
                && java.util.Objects.equals(createdAt, that.createdAt)
                && java.util.Objects.equals(updatedAt, that.updatedAt);
    }

    /**
     * 根据所有字段计算 CustomerProfile 的哈希值。
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, tenantId, userId, displayName, phone, email, status, properties, firstSeenAt, lastSeenAt, createdBy, createdAt, updatedAt);
    }

    /**
     * 返回与记录结构一致的调试字符串。
     */
    @Override
    public String toString() {
        return "CustomerProfile[" + "id=" + id + ", tenantId=" + tenantId + ", userId=" + userId + ", displayName=" + displayName + ", phone=" + phone + ", email=" + email + ", status=" + status + ", properties=" + properties + ", firstSeenAt=" + firstSeenAt + ", lastSeenAt=" + lastSeenAt + ", createdBy=" + createdBy + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + "]";
    }
}
