package org.chovy.canvas.platform.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.platform.domain.OpsCatalog;
import org.junit.jupiter.api.Test;

class OpsApplicationServiceTest {

    @Test
    void exposesRuntimeCacheRecoveryAndAuditOperations() {
        OpsApplicationService service = new OpsApplicationService(new OpsCatalog());

        assertThat(service.runtimeStatus(7L, "TENANT_ADMIN", "operator-1"))
                .containsEntry("status", "UP")
                .containsEntry("tenantId", 7L)
                .containsEntry("role", "TENANT_ADMIN")
                .containsEntry("username", "operator-1");

        assertThat(service.invalidateCache(7L, 101L, "operator-1"))
                .containsEntry("tenantId", 7L)
                .containsEntry("canvasId", 101L)
                .containsEntry("invalidated", true);

        assertThat(service.rebuildRuntimeState(7L, "operator-1"))
                .containsEntry("tenantId", 7L)
                .containsEntry("rebuilt", true)
                .containsEntry("routeCount", 3);

        List<Map<String, Object>> events = service.auditEvents(7L, 10);
        assertThat(events).hasSize(2);
        assertThat(events.getFirst())
                .containsEntry("tenantId", 7L)
                .containsEntry("action", "CACHE_INVALIDATE");
    }

    @Test
    void emergencyActionsRequireReasonAndRecordAudits() {
        OpsApplicationService service = new OpsApplicationService(new OpsCatalog());

        Map<String, Object> pause = service.emergencyAction(7L, 101L, "PAUSE",
                Map.of("reason", "campaign incident"), "TENANT_ADMIN", "operator-1");
        assertThat(pause)
                .containsEntry("tenantId", 7L)
                .containsEntry("canvasId", 101L)
                .containsEntry("action", "PAUSE")
                .containsEntry("status", "PAUSED")
                .containsEntry("operator", "operator-1");

        Map<String, Object> kill = service.emergencyAction(7L, 101L, "KILL",
                Map.of("reason", "bad deploy", "mode", "FORCE"), "TENANT_ADMIN", "operator-2");
        assertThat(kill)
                .containsEntry("action", "KILL")
                .containsEntry("mode", "FORCE")
                .containsEntry("status", "KILLED");

        assertThat(service.auditEvents(7L, 10))
                .extracting(item -> item.get("action"))
                .contains("PAUSE", "KILL");
    }

    @Test
    void defaultsAndValidationMatchWebCompatibilityContract() {
        OpsApplicationService service = new OpsApplicationService(new OpsCatalog());

        assertThat(service.runtimeStatus(null, null, " "))
                .containsEntry("tenantId", 7L)
                .containsEntry("role", "OPERATOR")
                .containsEntry("username", "operator-1");

        assertThatThrownBy(() -> service.emergencyAction(7L, 101L, "PAUSE", Map.of(),
                "TENANT_ADMIN", "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("reason is required");

        assertThatThrownBy(() -> service.emergencyAction(7L, 101L, "PAUSE",
                Map.of("reason", "incident"), "OPERATOR", "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("operator is not allowed to execute ops emergency action");
    }
}
