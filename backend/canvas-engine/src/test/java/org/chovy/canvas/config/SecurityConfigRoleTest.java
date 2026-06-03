package org.chovy.canvas.config;

import org.chovy.canvas.common.tenant.RoleNames;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

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

    @Test
    void opsRoutesRequireAdminOrOperatorRoles() {
        assertThat(SecurityConfig.OPS_ROUTE_ROLES)
                .containsExactly(RoleNames.ADMIN, RoleNames.SUPER_ADMIN, RoleNames.TENANT_ADMIN, RoleNames.OPERATOR);
    }

    @Test
    void documentationRoutesArePublicOnlyOutsideProductionLikeProfiles() {
        assertThat(SecurityConfig.publicDocumentationEnabled(new MockEnvironment()
                .withProperty("spring.profiles.active", "local"))).isTrue();
        assertThat(SecurityConfig.publicDocumentationEnabled(new MockEnvironment()
                .withProperty("spring.profiles.active", "prod"))).isFalse();
        assertThat(SecurityConfig.DOCUMENTATION_ROUTES)
                .contains("/swagger-ui/**", "/v3/api-docs/**");
    }
}
