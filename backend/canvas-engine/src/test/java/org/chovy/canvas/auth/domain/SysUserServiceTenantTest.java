package org.chovy.canvas.auth.domain;

import io.jsonwebtoken.Claims;
import org.chovy.canvas.auth.util.JwtUtil;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.dal.dataobject.SysUserDO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SysUserServiceTenantTest {

    @Test
    void jwtIncludesTenantAndRoleClaims() {
        SysUserDO user = new SysUserDO();
        user.setId(7L);
        user.setTenantId(3L);
        user.setUsername("tenant_admin");
        user.setDisplayName("Tenant Admin");
        user.setRole(RoleNames.TENANT_ADMIN);
        JwtUtil jwtUtil = new JwtUtil("canvas-engine-jwt-secret-key-must-be-at-least-256-bits", 24);

        Claims claims = jwtUtil.parse(jwtUtil.generate(user));

        assertThat(claims.getSubject()).isEqualTo("7");
        assertThat(claims.get("tenantId", Number.class).longValue()).isEqualTo(3L);
        assertThat(claims.get("role", String.class)).isEqualTo(RoleNames.TENANT_ADMIN);
    }

    @Test
    void legacyAdminRoleIsSuperAdmin() {
        TenantContext context = new TenantContext(3L, RoleNames.ADMIN, "admin");

        assertThat(context.isSuperAdmin()).isTrue();
    }
}
