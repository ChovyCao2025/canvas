package org.chovy.canvas.platform.api;

import java.util.Objects;

/**
 * 表示调用平台能力的租户内操作者。
 */
public final class PlatformActor {

    /**
     * 操作者所属租户标识。
     */
    private final Long tenantId;

    /**
     * 操作者用户名。
     */
    private final String username;

    /**
     * 创建调用平台能力的操作者。
     *
     * @param tenantId 操作者所属租户标识
     * @param username 操作者用户名
     */
    public PlatformActor(Long tenantId, String username) {
        this.tenantId = tenantId;
        this.username = username;
    }

    /**
     * 返回操作者所属租户标识。
     *
     * @return 操作者所属租户标识
     */
    public Long tenantId() {
        return tenantId;
    }

    /**
     * 返回操作者用户名。
     *
     * @return 操作者用户名
     */
    public String username() {
        return username;
    }

    /**
     * 判断两个操作者值对象是否相同。
     *
     * @param object 待比较对象
     * @return 所有字段相同时返回 true
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof PlatformActor that)) {
            return false;
        }
        return Objects.equals(tenantId, that.tenantId)
                && Objects.equals(username, that.username);
    }

    /**
     * 计算操作者值对象哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(tenantId, username);
    }

    /**
     * 返回与原 record 形态一致的字符串。
     *
     * @return 字符串表示
     */
    @Override
    public String toString() {
        return "PlatformActor[tenantId=" + tenantId + ", username=" + username + "]";
    }
}
