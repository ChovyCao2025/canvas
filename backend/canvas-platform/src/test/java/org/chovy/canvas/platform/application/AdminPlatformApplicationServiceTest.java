package org.chovy.canvas.platform.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.platform.domain.AdminPlatformCatalog;
import org.junit.jupiter.api.Test;

/**
 * 覆盖管理后台应用服务的租户隔离和基础管理操作。
 */
class AdminPlatformApplicationServiceTest {

    /**
     * 验证用户列表按租户隔离且创建、更新、禁用流程保持可变状态。
     */
    @Test
    void usersAreTenantScopedMutableAndOrdered() {
        AdminPlatformApplicationService service = new AdminPlatformApplicationService(new AdminPlatformCatalog());

        Map<String, Object> created = service.createUser(7L, Map.of(
                "username", "admin-reviewer",
                "password", "secret",
                "displayName", "Admin Reviewer",
                "role", "TENANT_ADMIN"), "operator-1");

        assertThat(created)
                .containsEntry("id", 1002L)
                .containsEntry("tenantId", 7L)
                .containsEntry("username", "admin-reviewer")
                .containsEntry("displayName", "Admin Reviewer")
                .containsEntry("role", "TENANT_ADMIN")
                .containsEntry("enabled", 1)
                .doesNotContainKey("password");
        assertThat(service.users(7L)).extracting(item -> item.get("username"))
                .containsExactly("operator-1", "admin-reviewer");

        service.updateUser(7L, 1002L, Map.of("displayName", "Reviewer Lead", "role", "ADMIN"), "operator-2");
        assertThat(service.users(7L).get(1))
                .containsEntry("displayName", "Reviewer Lead")
                .containsEntry("role", "ADMIN")
                .containsEntry("updatedBy", "operator-2");
        service.disableUser(7L, 1002L, "operator-3");
        assertThat(service.users(7L).get(1)).containsEntry("enabled", 0);
        assertThat(service.users(8L)).extracting(item -> item.get("tenantId")).containsExactly(8L);
    }

    /**
     * 验证项目、成员、画布分页和统计都使用租户内状态。
     */
    @Test
    void projectsMembersCanvasesAndStatsUseTenantScopedState() {
        AdminPlatformApplicationService service = new AdminPlatformApplicationService(new AdminPlatformCatalog());

        Map<String, Object> project = service.createProject(7L, Map.of(
                "projectKey", "retention",
                "projectName", "Retention",
                "description", "Lifecycle retention"), "operator-1");

        assertThat(project)
                .containsEntry("projectId", 2002L)
                .containsEntry("tenantId", 7L)
                .containsEntry("projectKey", "retention")
                .containsEntry("status", "ENABLED")
                .containsEntry("createdBy", "operator-1");
        assertThat(service.projects(7L)).extracting(item -> item.get("projectId")).containsExactly(2001L, 2002L);
        assertThat(service.project(7L, 2002L)).containsEntry("projectName", "Retention");

        Map<String, Object> updated = service.updateProject(7L, 2002L,
                Map.of("projectName", "Retention Growth", "requireReviewBeforePublish", true), "operator-2");
        assertThat(updated)
                .containsEntry("projectName", "Retention Growth")
                .containsEntry("requireReviewBeforePublish", true)
                .containsEntry("updatedBy", "operator-2");

        Map<String, Object> member = service.setProjectMember(7L, 2002L, 1001L,
                Map.of("role", "OWNER"), "operator-2");
        assertThat(member)
                .containsEntry("projectId", 2002L)
                .containsEntry("userId", 1001L)
                .containsEntry("role", "OWNER")
                .containsEntry("updatedBy", "operator-2");
        assertThat(service.projectMembers(7L, 2002L)).extracting(item -> item.get("userId")).containsExactly(1001L);
        service.removeProjectMember(7L, 2002L, 1001L);
        assertThat(service.projectMembers(7L, 2002L)).isEmpty();

        assertThat(service.projectCanvases(7L, 2001L, 1, 20))
                .containsEntry("total", 1L)
                .containsEntry("page", 1)
                .containsEntry("size", 20);
        assertThat((List<?>) service.projectCanvases(7L, 2001L, 1, 20).get("list")).hasSize(1);
        assertThat(service.projectStats(7L, 2001L))
                .containsEntry("projectId", 2001L)
                .containsEntry("canvasCount", 1L)
                .containsEntry("memberCount", 1L);

        assertThat(service.disableProject(7L, 2002L, "operator-3")).containsEntry("status", "DISABLED");
        assertThatThrownBy(() -> service.project(8L, 2002L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("project not found");
    }

    /**
     * 验证系统选项、租户列表和用量统计具备确定性结果。
     */
    @Test
    void systemOptionsTenantsAndUsageAreDeterministic() {
        AdminPlatformApplicationService service = new AdminPlatformApplicationService(new AdminPlatformCatalog());

        assertThat(service.systemOptions(7L, "canvas", null, null, null))
                .extracting(item -> item.get("optionKey"))
                .containsExactly("canvas.publish.review.required");
        Map<String, Object> option = service.updateSystemOption(7L, 3001L,
                Map.of("optionValue", "false", "remark", "temporary"), "operator-1");
        assertThat(option)
                .containsEntry("optionValue", "false")
                .containsEntry("updatedBy", "operator-1");

        Map<String, Object> tenant = service.createTenant(Map.of(
                "name", "Retail Tenant",
                "tenantKey", "retail",
                "planCode", "PRO",
                "quotaJson", "{\"users\":200}"), "operator-1");
        assertThat(tenant)
                .containsEntry("id", 8L)
                .containsEntry("tenantKey", "retail")
                .containsEntry("status", "ACTIVE")
                .containsEntry("createdBy", "operator-1");
        assertThat(service.tenants()).extracting(item -> item.get("id")).containsExactly(7L, 8L);
        assertThat(service.disableTenant(8L, "operator-2"))
                .containsEntry("status", "DISABLED")
                .containsEntry("updatedBy", "operator-2");
        assertThat(service.activateTenant(8L, "operator-3"))
                .containsEntry("status", "ACTIVE")
                .containsEntry("updatedBy", "operator-3");
        assertThat(service.tenantUsage(7L))
                .containsEntry("tenantId", 7L)
                .containsEntry("userCount", 1L)
                .containsEntry("projectCount", 1L)
                .containsEntry("canvasCount", 1L);
    }

    /**
     * 验证创建用户、项目、租户时拒绝空白必填名称。
     */
    @Test
    void requiredNamesRejectBlankInput() {
        AdminPlatformApplicationService service = new AdminPlatformApplicationService(new AdminPlatformCatalog());

        assertThatThrownBy(() -> service.createUser(7L, Map.of("displayName", "Missing username"), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("username is required");
        assertThatThrownBy(() -> service.createProject(7L, Map.of("projectName", "Missing key"), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("projectKey is required");
        assertThatThrownBy(() -> service.createTenant(Map.of("tenantKey", "missing-name"), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name is required");
    }
}
