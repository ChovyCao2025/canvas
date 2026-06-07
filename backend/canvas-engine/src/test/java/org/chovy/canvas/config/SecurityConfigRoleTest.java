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
    void opsRoutesSeparateReadAndWriteRoles() {
        assertThat(SecurityConfig.OPS_READ_ROUTE_ROLES)
                .containsExactly(
                        RoleNames.ADMIN,
                        RoleNames.SUPER_ADMIN,
                        RoleNames.TENANT_ADMIN,
                        RoleNames.OPERATOR);
        assertThat(SecurityConfig.OPS_WRITE_ROUTE_ROLES)
                .containsExactly(RoleNames.ADMIN, RoleNames.SUPER_ADMIN, RoleNames.TENANT_ADMIN);
        assertThat(SecurityConfig.OPS_ROUTE_ROLES)
                .containsExactly(RoleNames.ADMIN, RoleNames.SUPER_ADMIN, RoleNames.TENANT_ADMIN);
    }

    @Test
    void contentMarketingRoutesSeparateDraftAndPublishRoles() {
        assertThat(SecurityConfig.CONTENT_EDITOR_ROUTE_ROLES)
                .containsExactly(
                        RoleNames.ADMIN,
                        RoleNames.SUPER_ADMIN,
                        RoleNames.TENANT_ADMIN,
                        RoleNames.OPERATOR);
        assertThat(SecurityConfig.CONTENT_PUBLISH_ROUTE_ROLES)
                .containsExactly(RoleNames.ADMIN, RoleNames.SUPER_ADMIN, RoleNames.TENANT_ADMIN);
    }

    @Test
    void internalOpenApiRoutesStayExplicitlyEnumerated() {
        assertThat(SecurityConfig.INTERNAL_OPEN_API_ROUTES)
                .containsExactly(
                        "/canvas/events/report",
                        "/canvas/execute/direct/*",
                        "/canvas/trigger/behavior",
                        "/warehouse/realtime/pipelines/checkpoints");
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
