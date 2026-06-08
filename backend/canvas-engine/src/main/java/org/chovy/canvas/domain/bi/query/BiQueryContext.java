package org.chovy.canvas.domain.bi.query;

import org.chovy.canvas.common.tenant.RoleNames;

/**
 * BiQueryContext 承载 domain.bi.query 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param username username 字段。
 * @param role role 字段。
 */
public record BiQueryContext(
        Long tenantId,
        String username,
        String role
) {
    /**
     * 创建 BiQueryContext 实例并注入 domain.bi.query 场景依赖。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     */
    public BiQueryContext(Long tenantId, String username) {
        this(tenantId, username, RoleNames.OPERATOR);
    }

    public BiQueryContext {
        tenantId = tenantId == null ? 0L : tenantId;
        username = username == null || username.isBlank() ? "system" : username;
        role = role == null || role.isBlank() ? RoleNames.OPERATOR : role;
    }
}
