package org.chovy.canvas.platform.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.chovy.canvas.platform.domain.ApprovalCatalog;
import org.junit.jupiter.api.Test;

class ApprovalApplicationServiceTest {

    @Test
    void listsTasksAndInstancesThenApprovesAssignedTask() {
        ApprovalApplicationService service = new ApprovalApplicationService(new ApprovalCatalog());

        assertThat(service.tasks(7L, "operator-1", "OPERATOR", "PENDING"))
                .anySatisfy(task -> assertThat(task)
                        .containsEntry("id", 7001L)
                        .containsEntry("instanceId", 9001L)
                        .containsEntry("status", "PENDING"));
        assertThat(service.instances(7L, "canvas", "canvas-101", "PENDING"))
                .singleElement()
                .satisfies(instance -> assertThat(instance)
                        .containsEntry("id", 9001L)
                        .containsEntry("targetType", "CANVAS")
                        .containsEntry("targetId", "canvas-101"));

        Map<String, Object> approved = service.approve(7L, 7001L, Map.of("comment", "looks good"),
                "operator-1", "OPERATOR");

        assertThat(approved)
                .containsEntry("status", "APPROVED")
                .containsEntry("updatedBy", "operator-1")
                .containsEntry("comment", "looks good");
        assertThat(service.tasks(7L, "operator-1", "OPERATOR", "PENDING"))
                .hasSize(2)
                .noneSatisfy(task -> assertThat(task).containsEntry("id", 7001L));
        assertThat(service.instances(7L, "CANVAS", "canvas-101", "APPROVED")).hasSize(1);
    }

    @Test
    void rejectsBadTaskInputsAndUnauthorizedDecisions() {
        ApprovalApplicationService service = new ApprovalApplicationService(new ApprovalCatalog());

        assertThatThrownBy(() -> service.approve(7L, null, Map.of(), "operator-1", "OPERATOR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("taskId is required");
        assertThatThrownBy(() -> service.reject(7L, 7001L, Map.of("comment", "no"), "other", "VIEWER"))
                .isInstanceOf(SecurityException.class)
                .hasMessage("approval task is not assigned to actor: other");
        assertThatThrownBy(() -> service.reject(7L, 7001L, Map.of("comment", "no"), "other", "OPERATOR"))
                .isInstanceOf(SecurityException.class)
                .hasMessage("approval task is not assigned to actor: other");
    }

    @Test
    void adminOnlyLarkSyncUsesLimitAndUpdatesInstanceSnapshot() {
        ApprovalApplicationService service = new ApprovalApplicationService(new ApprovalCatalog());

        assertThatThrownBy(() -> service.syncLarkApprovals(7L, 100, "operator-1", "OPERATOR"))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Lark approval sync requires admin role");

        Map<String, Object> summary = service.syncLarkApprovals(7L, 1, "admin-1", "TENANT_ADMIN");
        Map<String, Object> instance = service.syncLarkApprovalInstance(7L, 9001L, "admin-1", "ADMIN");

        assertThat(summary)
                .containsEntry("tenantId", 7L)
                .containsEntry("synced", 1)
                .containsEntry("provider", "LARK")
                .containsEntry("operator", "admin-1");
        assertThat(instance)
                .containsEntry("id", 9001L)
                .containsEntry("externalProvider", "LARK")
                .containsEntry("externalSyncedBy", "admin-1");
        assertThat(service.syncLarkApprovalInstance(8L, 9001L, "admin-1", "ADMIN"))
                .containsEntry("tenantId", 8L)
                .containsEntry("targetId", "canvas-tenant-8");
    }
}
