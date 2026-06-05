package org.chovy.canvas.domain.bi.resource;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.domain.bi.permission.BiPermissionService;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.springframework.stereotype.Service;

@Service
public class BiResourcePermissionGuard {

    private final BiPermissionService permissionService;

    public BiResourcePermissionGuard(BiPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void require(Long tenantId,
                        Long workspaceId,
                        String resourceType,
                        Long resourceId,
                        String username,
                        String role,
                        String actionKey) {
        if (resourceId == null) {
            return;
        }
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        permissionService.enforceResourceAccess(
                scopedTenantId,
                workspaceId,
                resourceType,
                resourceId,
                new BiQueryContext(scopedTenantId, defaultUser(username), defaultRole(role)),
                actionKey);
    }

    private String defaultUser(String username) {
        return username == null || username.isBlank() ? "system" : username;
    }

    private String defaultRole(String role) {
        return role == null || role.isBlank() ? RoleNames.OPERATOR : role;
    }
}
