package org.chovy.canvas.common.tenant;

public record TenantContext(Long tenantId, String role, String username) {

    public boolean isSuperAdmin() {
        return RoleNames.SUPER_ADMIN.equals(role) || RoleNames.ADMIN.equals(role);
    }

    public boolean isTenantAdmin() {
        return RoleNames.TENANT_ADMIN.equals(role);
    }
}
