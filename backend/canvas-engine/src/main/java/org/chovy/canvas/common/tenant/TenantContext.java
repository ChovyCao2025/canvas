package org.chovy.canvas.common.tenant;

/**
 * TenantContext 承载 common.tenant 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param role role 字段。
 * @param username username 字段。
 */
public record TenantContext(Long tenantId, String role, String username) {

    /**
     * isSuperAdmin 校验或转换 common.tenant 场景的数据。
     * @return 返回布尔判断结果。
     */
    public boolean isSuperAdmin() {
        return RoleNames.SUPER_ADMIN.equals(role) || RoleNames.ADMIN.equals(role);
    }

    /**
     * isTenantAdmin 校验或转换 common.tenant 场景的数据。
     * @return 返回布尔判断结果。
     */
    public boolean isTenantAdmin() {
        return RoleNames.TENANT_ADMIN.equals(role);
    }
}
