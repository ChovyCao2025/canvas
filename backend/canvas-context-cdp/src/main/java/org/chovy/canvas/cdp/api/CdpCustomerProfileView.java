package org.chovy.canvas.cdp.api;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 表示 CdpCustomerProfileView 的业务数据或处理组件。
 */
public final class CdpCustomerProfileView {

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
     * 使用记录字段创建 CdpCustomerProfileView。
     */
    public CdpCustomerProfileView(
            Long id,
            Long tenantId,
            String userId,
            String displayName,
            String phone,
            String email,
            String status,
            Map<String, Object> properties,
            LocalDateTime firstSeenAt,
            LocalDateTime lastSeenAt) {
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
     * 按所有字段比较 CdpCustomerProfileView。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CdpCustomerProfileView that = (CdpCustomerProfileView) o;
        return java.util.Objects.equals(id, that.id)
                && java.util.Objects.equals(tenantId, that.tenantId)
                && java.util.Objects.equals(userId, that.userId)
                && java.util.Objects.equals(displayName, that.displayName)
                && java.util.Objects.equals(phone, that.phone)
                && java.util.Objects.equals(email, that.email)
                && java.util.Objects.equals(status, that.status)
                && java.util.Objects.equals(properties, that.properties)
                && java.util.Objects.equals(firstSeenAt, that.firstSeenAt)
                && java.util.Objects.equals(lastSeenAt, that.lastSeenAt);
    }

    /**
     * 根据所有字段计算 CdpCustomerProfileView 的哈希值。
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, tenantId, userId, displayName, phone, email, status, properties, firstSeenAt, lastSeenAt);
    }

    /**
     * 返回与记录结构一致的调试字符串。
     */
    @Override
    public String toString() {
        return "CdpCustomerProfileView[" + "id=" + id + ", tenantId=" + tenantId + ", userId=" + userId + ", displayName=" + displayName + ", phone=" + phone + ", email=" + email + ", status=" + status + ", properties=" + properties + ", firstSeenAt=" + firstSeenAt + ", lastSeenAt=" + lastSeenAt + "]";
    }
}
