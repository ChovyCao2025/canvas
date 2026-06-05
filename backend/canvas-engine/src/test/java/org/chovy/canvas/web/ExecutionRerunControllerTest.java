package org.chovy.canvas.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.ExecutionRerunAuditDO;
import org.chovy.canvas.dal.dataobject.TestUserDO;
import org.chovy.canvas.dal.dataobject.TestUserSetDO;
import org.chovy.canvas.dal.mapper.ExecutionRerunAuditMapper;
import org.chovy.canvas.dal.mapper.TestUserMapper;
import org.chovy.canvas.dal.mapper.TestUserSetMapper;
import org.chovy.canvas.domain.canvas.TestUserRerunService;
import org.chovy.canvas.domain.canvas.TestUserRerunService.RerunRequest;
import org.chovy.canvas.domain.canvas.TestUserRerunService.TestUserCreateReq;
import org.chovy.canvas.domain.canvas.TestUserRerunService.TestUserSetCreateReq;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExecutionRerunControllerTest {

    private final TestUserSetMapper setMapper = mock(TestUserSetMapper.class);
    private final TestUserMapper userMapper = mock(TestUserMapper.class);
    private final ExecutionRerunAuditMapper auditMapper = mock(ExecutionRerunAuditMapper.class);
    private final CanvasExecutionService executionService = mock(CanvasExecutionService.class);
    private final TenantContextResolver tenantResolver = mock(TenantContextResolver.class);
    private final TestUserRerunService service = new TestUserRerunService(
            setMapper, userMapper, auditMapper, executionService, new ObjectMapper());

    @Test
    void createsSeedUserAndPreviewContext() {
        when(tenantResolver.current()).thenReturn(Mono.just(context(RoleNames.TENANT_ADMIN)));
        when(setMapper.insert(any(TestUserSetDO.class))).thenAnswer(invocation -> {
            invocation.<TestUserSetDO>getArgument(0).setId(11L);
            return 1;
        });
        when(userMapper.insert(any(TestUserDO.class))).thenAnswer(invocation -> {
            invocation.<TestUserDO>getArgument(0).setId(22L);
            return 1;
        });
        TestUserController controller = new TestUserController(service, tenantResolver);

        TestUserSetDO set = controller.createSet(new TestUserSetCreateReq("VIP seeds", "demo")).block().getData();
        TestUserDO created = controller.createUser(set.getId(), new TestUserCreateReq(
                "user-1",
                "Alice",
                Map.of("tier", "gold"),
                Map.of("amount", 150))).block().getData();
        created.setTenantId(7L);
        when(userMapper.selectById(22L)).thenReturn(created);

        var preview = controller.preview(22L).block().getData();

        assertThat(set.getId()).isEqualTo(11L);
        assertThat(preview.context()).containsEntry("userId", "user-1");
        assertThat((Map<String, Object>) preview.context().get("profile")).containsEntry("tier", "gold");
        assertThat(preview.inputParams()).containsEntry("amount", 150);
    }

    @Test
    void rerunRequiresReason() {
        ExecutionRerunController controller = controller(RoleNames.TENANT_ADMIN);

        assertThatThrownBy(() -> controller.rerun(10L, new RerunRequest(
                "user-1", null, null, null, "too short", Map.of(), null)).block())
                .hasMessageContaining("reason");
    }

    @Test
    void rerunDefaultsToDryRunAndWritesAudit() {
        ExecutionRerunController controller = controller(RoleNames.TENANT_ADMIN);
        stubAuditInsert(44L);
        when(executionService.triggerDryRun(eq(10L), eq("user-1"), any(), eq(null)))
                .thenReturn(Mono.just(Map.of("executionId", "dry-1")));

        var result = controller.rerun(10L, new RerunRequest(
                "user-1", null, "exec-old", null,
                "rerun because operator is validating", Map.of("amount", 150), null)).block().getData();

        assertThat(result.auditId()).isEqualTo(44L);
        assertThat(result.mode()).isEqualTo(TestUserRerunService.MODE_DRY_RUN);
        ArgumentCaptor<ExecutionRerunAuditDO> audit = ArgumentCaptor.forClass(ExecutionRerunAuditDO.class);
        verify(auditMapper).insert(audit.capture());
        assertThat(audit.getValue().getOriginalExecutionId()).isEqualTo("exec-old");
        assertThat(audit.getValue().getMode()).isEqualTo(TestUserRerunService.MODE_DRY_RUN);
        verify(executionService).triggerDryRun(eq(10L), eq("user-1"), any(), eq(null));
        verify(auditMapper).updateById(any(ExecutionRerunAuditDO.class));
    }

    @Test
    void sideEffectSkipModeUsesDryRunWithSkipMarker() {
        ExecutionRerunController controller = controller(RoleNames.TENANT_ADMIN);
        stubAuditInsert(45L);
        when(executionService.triggerDryRun(eq(10L), eq("user-1"), any(), eq(null)))
                .thenReturn(Mono.just(Map.of("executionId", "dry-2")));

        controller.rerun(10L, new RerunRequest(
                "user-1", null, null, TestUserRerunService.MODE_SKIP_SIDE_EFFECTS,
                "rerun without downstream side effects", Map.of("amount", 150), null)).block();

        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(executionService).triggerDryRun(eq(10L), eq("user-1"), payload.capture(), eq(null));
        assertThat(payload.getValue()).containsEntry("__skipSideEffects", true);
        verify(executionService, never()).trigger(any(), any(), any(), any(), any(), any(), any(), eq(false));
    }

    @Test
    void adminReplayRequiresAdminRole() {
        ExecutionRerunController controller = controller(RoleNames.OPERATOR);

        assertThatThrownBy(() -> controller.rerun(10L, new RerunRequest(
                "user-1", null, null, TestUserRerunService.MODE_ADMIN_REPLAY,
                "admin replay for incident validation", Map.of(), null)).block())
                .hasMessageContaining("ADMIN_REPLAY requires admin role");
    }

    @Test
    void adminReplayTriggersRealExecutionWithAuditBoundary() {
        ExecutionRerunController controller = controller(RoleNames.TENANT_ADMIN);
        stubAuditInsert(46L);
        when(executionService.trigger(eq(10L), eq("user-1"), any(), any(), eq(null), any(), eq("rerun-46"), eq(false)))
                .thenReturn(Mono.just(Map.of("executionId", "real-1")));

        var result = controller.rerun(10L, new RerunRequest(
                "user-1", null, "exec-old", TestUserRerunService.MODE_ADMIN_REPLAY,
                "admin replay for incident validation", Map.of("amount", 150), null)).block().getData();

        assertThat(result.status()).isEqualTo(TestUserRerunService.STATUS_SUCCESS);
        verify(executionService).trigger(eq(10L), eq("user-1"), any(), any(), eq(null), any(), eq("rerun-46"), eq(false));
        verify(executionService, never()).triggerDryRun(any(), any(), any(), any());
    }

    private ExecutionRerunController controller(String role) {
        when(tenantResolver.current()).thenReturn(Mono.just(context(role)));
        return new ExecutionRerunController(service, tenantResolver);
    }

    private TenantContext context(String role) {
        return new TenantContext(7L, role, "operator-1");
    }

    private void stubAuditInsert(Long id) {
        when(auditMapper.insert(any(ExecutionRerunAuditDO.class))).thenAnswer(invocation -> {
            invocation.<ExecutionRerunAuditDO>getArgument(0).setId(id);
            return 1;
        });
    }
}
