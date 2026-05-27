package org.chovy.canvas.config;

import org.chovy.canvas.common.tenant.RoleNames;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigRoleTest {

    @Test
    void tenantAdministrationRoutesRemainSuperAdminOnlyDuringRollout() {
        assertThat(SecurityConfig.SUPER_ADMIN_ROUTE_ROLES)
                .containsExactly(RoleNames.ADMIN, RoleNames.SUPER_ADMIN);
    }

    @Test
    void userAdministrationRoutesAllowTenantAdmins() {
        assertThat(SecurityConfig.TENANT_ADMIN_ROUTE_ROLES)
                .containsExactly(RoleNames.ADMIN, RoleNames.SUPER_ADMIN, RoleNames.TENANT_ADMIN);
    }
}
