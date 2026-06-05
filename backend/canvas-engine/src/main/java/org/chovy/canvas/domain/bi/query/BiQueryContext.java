package org.chovy.canvas.domain.bi.query;

import org.chovy.canvas.common.tenant.RoleNames;

public record BiQueryContext(
        Long tenantId,
        String username,
        String role
) {
    public BiQueryContext(Long tenantId, String username) {
        this(tenantId, username, RoleNames.OPERATOR);
    }

    public BiQueryContext {
        tenantId = tenantId == null ? 0L : tenantId;
        username = username == null || username.isBlank() ? "system" : username;
        role = role == null || role.isBlank() ? RoleNames.OPERATOR : role;
    }
}
