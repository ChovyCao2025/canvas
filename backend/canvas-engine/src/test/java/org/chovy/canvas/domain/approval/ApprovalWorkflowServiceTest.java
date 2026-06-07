package org.chovy.canvas.domain.approval;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.dal.dataobject.ApprovalAuditEventDO;
import org.chovy.canvas.dal.dataobject.ApprovalDefinitionDO;
import org.chovy.canvas.dal.dataobject.ApprovalInstanceDO;
import org.chovy.canvas.dal.dataobject.ApprovalTaskDO;
import org.chovy.canvas.dal.mapper.ApprovalAuditEventMapper;
import org.chovy.canvas.dal.mapper.ApprovalDefinitionMapper;
import org.chovy.canvas.dal.mapper.ApprovalInstanceMapper;
import org.chovy.canvas.dal.mapper.ApprovalTaskMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalWorkflowServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-06T06:00:00Z"), ZoneOffset.UTC);

    @Test
    void enterpriseApprovalMigrationCreatesDefinitionsInstancesTasksAndAuditEvents() throws Exception {
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V350__enterprise_approval_workflow.sql"));

        assertThat(migration).contains("CREATE TABLE IF NOT EXISTS approval_definition");
        assertThat(migration).contains("CREATE TABLE IF NOT EXISTS approval_instance");
        assertThat(migration).contains("CREATE TABLE IF NOT EXISTS approval_task");
        assertThat(migration).contains("CREATE TABLE IF NOT EXISTS approval_audit_event");
        assertThat(migration).contains("CANVAS_PUBLISH_DEFAULT");
        assertThat(migration).contains("BI_PUBLISH_DEFAULT");
        assertThat(migration).contains("RUNTIME_MANUAL_DEFAULT");
    }

    @Test
    void submitCreatesPendingInstanceTasksAndAuditEvent() {
        ApprovalDefinitionMapper definitionMapper = mock(ApprovalDefinitionMapper.class);
        ApprovalInstanceMapper instanceMapper = mock(ApprovalInstanceMapper.class);
        ApprovalTaskMapper taskMapper = mock(ApprovalTaskMapper.class);
        ApprovalAuditEventMapper auditMapper = mock(ApprovalAuditEventMapper.class);
        when(definitionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(definition());
        doAnswer(invocation -> {
            ApprovalInstanceDO row = invocation.getArgument(0);
            row.setId(101L);
            return 1;
        }).when(instanceMapper).insert(any(ApprovalInstanceDO.class));
        doAnswer(invocation -> {
            ApprovalTaskDO row = invocation.getArgument(0);
            row.setId(row.getApprover().equals("bob") ? 201L : 202L);
            return 1;
        }).when(taskMapper).insert(any(ApprovalTaskDO.class));
        ApprovalWorkflowService service = service(definitionMapper, instanceMapper, taskMapper, auditMapper, List.of());

        ApprovalInstanceView view = service.submit(new ApprovalSubmitCommand(
                7L,
                "CANVAS_PUBLISH_DEFAULT",
                "CANVAS",
                "CANVAS",
                "62",
                91L,
                "alice",
                "准备发布活动画布",
                "HIGH",
                "[\"PROJECT_REQUIRES_REVIEW\"]",
                "{\"canvasId\":62}",
                List.of("bob", "tenant_admin"),
                null,
                "PUBLISH_CANVAS"));

        ArgumentCaptor<ApprovalInstanceDO> instance = ArgumentCaptor.forClass(ApprovalInstanceDO.class);
        verify(instanceMapper).insert(instance.capture());
        assertThat(instance.getValue().getTenantId()).isEqualTo(7L);
        assertThat(instance.getValue().getDefinitionKey()).isEqualTo("CANVAS_PUBLISH_DEFAULT");
        assertThat(instance.getValue().getTargetType()).isEqualTo("CANVAS");
        assertThat(instance.getValue().getTargetId()).isEqualTo("62");
        assertThat(instance.getValue().getTargetVersionId()).isEqualTo(91L);
        assertThat(instance.getValue().getStatus()).isEqualTo("PENDING");
        assertThat(instance.getValue().getSubmitter()).isEqualTo("alice");
        assertThat(instance.getValue().getRiskLevel()).isEqualTo("HIGH");
        assertThat(instance.getValue().getAutoAction()).isEqualTo("PUBLISH_CANVAS");

        ArgumentCaptor<ApprovalTaskDO> task = ArgumentCaptor.forClass(ApprovalTaskDO.class);
        verify(taskMapper, org.mockito.Mockito.times(2)).insert(task.capture());
        assertThat(task.getAllValues())
                .extracting(ApprovalTaskDO::getApprover)
                .containsExactly("bob", "tenant_admin");
        assertThat(task.getAllValues())
                .allSatisfy(row -> {
                    assertThat(row.getInstanceId()).isEqualTo(101L);
                    assertThat(row.getStatus()).isEqualTo("PENDING");
                    assertThat(row.getDueAt()).isEqualTo(LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC).plusHours(24));
                });

        ArgumentCaptor<ApprovalAuditEventDO> audit = ArgumentCaptor.forClass(ApprovalAuditEventDO.class);
        verify(auditMapper).insert(audit.capture());
        assertThat(audit.getValue().getEventType()).isEqualTo("SUBMITTED");
        assertThat(audit.getValue().getActor()).isEqualTo("alice");
        assertThat(audit.getValue().getNewStatus()).isEqualTo("PENDING");

        assertThat(view.id()).isEqualTo(101L);
        assertThat(view.status()).isEqualTo("PENDING");
        assertThat(view.pendingTasks()).hasSize(2);
    }

    @Test
    void approvePendingTaskCompletesInstanceAndRunsAutoAction() {
        ApprovalDefinitionMapper definitionMapper = mock(ApprovalDefinitionMapper.class);
        ApprovalInstanceMapper instanceMapper = mock(ApprovalInstanceMapper.class);
        ApprovalTaskMapper taskMapper = mock(ApprovalTaskMapper.class);
        ApprovalAuditEventMapper auditMapper = mock(ApprovalAuditEventMapper.class);
        RecordingAutoActionHandler handler = new RecordingAutoActionHandler();
        ApprovalTaskDO task = task(201L, 101L, "bob", "PENDING");
        ApprovalInstanceDO instance = instance("PENDING");
        when(taskMapper.selectById(201L)).thenReturn(task);
        when(instanceMapper.selectById(101L)).thenReturn(instance);
        when(definitionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(definition());
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));
        ApprovalWorkflowService service = service(definitionMapper, instanceMapper, taskMapper, auditMapper, List.of(handler));

        ApprovalInstanceView view = service.approveTask(new ApprovalDecisionCommand(
                7L,
                201L,
                "bob",
                RoleNames.OPERATOR,
                "检查通过"));

        assertThat(task.getStatus()).isEqualTo("APPROVED");
        assertThat(task.getActedAt()).isEqualTo(LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC));
        verify(taskMapper).updateById(task);
        assertThat(instance.getStatus()).isEqualTo("APPROVED");
        assertThat(instance.getCompletedBy()).isEqualTo("bob");
        assertThat(instance.getAutoActionStatus()).isEqualTo("SUCCESS");
        verify(instanceMapper, org.mockito.Mockito.atLeastOnce()).updateById(instance);
        assertThat(handler.executions).containsExactly("101:bob");
        assertThat(view.status()).isEqualTo("APPROVED");
    }

    @Test
    void rejectPendingTaskRejectsInstanceAndSkipsAutoAction() {
        ApprovalDefinitionMapper definitionMapper = mock(ApprovalDefinitionMapper.class);
        ApprovalInstanceMapper instanceMapper = mock(ApprovalInstanceMapper.class);
        ApprovalTaskMapper taskMapper = mock(ApprovalTaskMapper.class);
        ApprovalAuditEventMapper auditMapper = mock(ApprovalAuditEventMapper.class);
        RecordingAutoActionHandler handler = new RecordingAutoActionHandler();
        ApprovalTaskDO task = task(201L, 101L, "bob", "PENDING");
        ApprovalInstanceDO instance = instance("PENDING");
        when(taskMapper.selectById(201L)).thenReturn(task);
        when(instanceMapper.selectById(101L)).thenReturn(instance);
        ApprovalWorkflowService service = service(definitionMapper, instanceMapper, taskMapper, auditMapper, List.of(handler));

        ApprovalInstanceView view = service.rejectTask(new ApprovalDecisionCommand(
                7L,
                201L,
                "bob",
                RoleNames.OPERATOR,
                "风险未说明"));

        assertThat(task.getStatus()).isEqualTo("REJECTED");
        assertThat(instance.getStatus()).isEqualTo("REJECTED");
        assertThat(instance.getResultComment()).isEqualTo("风险未说明");
        assertThat(handler.executions).isEmpty();
        assertThat(view.status()).isEqualTo("REJECTED");
    }

    @Test
    void decisionRejectsUsersWhoAreNotAssignedApproverOrTenantAdmin() {
        ApprovalDefinitionMapper definitionMapper = mock(ApprovalDefinitionMapper.class);
        ApprovalInstanceMapper instanceMapper = mock(ApprovalInstanceMapper.class);
        ApprovalTaskMapper taskMapper = mock(ApprovalTaskMapper.class);
        ApprovalAuditEventMapper auditMapper = mock(ApprovalAuditEventMapper.class);
        ApprovalTaskDO task = task(201L, 101L, "bob", "PENDING");
        when(taskMapper.selectById(201L)).thenReturn(task);
        when(instanceMapper.selectById(101L)).thenReturn(instance("PENDING"));
        ApprovalWorkflowService service = service(definitionMapper, instanceMapper, taskMapper, auditMapper, List.of());

        assertThatThrownBy(() -> service.approveTask(new ApprovalDecisionCommand(
                7L,
                201L,
                "mallory",
                RoleNames.OPERATOR,
                "越权审批")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not assigned");

        verify(taskMapper, never()).updateById(any(ApprovalTaskDO.class));
    }

    @Test
    void larkProviderSubmitStoresExternalInstanceAndTaskIds() {
        ApprovalDefinitionMapper definitionMapper = mock(ApprovalDefinitionMapper.class);
        ApprovalInstanceMapper instanceMapper = mock(ApprovalInstanceMapper.class);
        ApprovalTaskMapper taskMapper = mock(ApprovalTaskMapper.class);
        ApprovalAuditEventMapper auditMapper = mock(ApprovalAuditEventMapper.class);
        ApprovalDefinitionDO definition = definition();
        definition.setExternalProvider("LARK");
        definition.setExternalDefinitionCode("lark-definition-code");
        when(definitionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(definition);
        doAnswer(invocation -> {
            ApprovalInstanceDO row = invocation.getArgument(0);
            row.setId(101L);
            return 1;
        }).when(instanceMapper).insert(any(ApprovalInstanceDO.class));
        doAnswer(invocation -> {
            ApprovalTaskDO row = invocation.getArgument(0);
            row.setId(row.getApprover().equals("bob") ? 201L : 202L);
            return 1;
        }).when(taskMapper).insert(any(ApprovalTaskDO.class));
        RecordingExternalApprovalProvider provider = new RecordingExternalApprovalProvider();
        ApprovalWorkflowService service = service(definitionMapper, instanceMapper, taskMapper, auditMapper,
                List.of(), List.of(provider));

        ApprovalInstanceView view = service.submit(new ApprovalSubmitCommand(
                7L,
                "CANVAS_PUBLISH_DEFAULT",
                "CANVAS",
                "CANVAS",
                "62",
                91L,
                "alice",
                "准备发布活动画布",
                "HIGH",
                "[\"PROJECT_REQUIRES_REVIEW\"]",
                "{\"form\":[{\"id\":\"reason\",\"type\":\"textarea\",\"value\":\"准备发布活动画布\"}]}",
                List.of("bob", "tenant_admin"),
                null,
                "PUBLISH_CANVAS"));

        assertThat(provider.submissions).containsExactly("CANVAS_PUBLISH_DEFAULT:lark-definition-code:101");
        assertThat(view.externalInstanceId()).isEqualTo("lark-instance-101");
        ArgumentCaptor<ApprovalInstanceDO> instanceUpdate = ArgumentCaptor.forClass(ApprovalInstanceDO.class);
        verify(instanceMapper, org.mockito.Mockito.atLeastOnce()).updateById(instanceUpdate.capture());
        assertThat(instanceUpdate.getAllValues())
                .anySatisfy(row -> assertThat(row.getExternalInstanceId()).isEqualTo("lark-instance-101"));
        ArgumentCaptor<ApprovalTaskDO> taskUpdate = ArgumentCaptor.forClass(ApprovalTaskDO.class);
        verify(taskMapper, org.mockito.Mockito.atLeastOnce()).updateById(taskUpdate.capture());
        assertThat(taskUpdate.getAllValues())
                .extracting(ApprovalTaskDO::getExternalTaskId)
                .contains("lark-task-201", "lark-task-202");
    }

    @Test
    void larkProviderDecisionRunsExternalTaskActionBeforeLocalApproval() {
        ApprovalDefinitionMapper definitionMapper = mock(ApprovalDefinitionMapper.class);
        ApprovalInstanceMapper instanceMapper = mock(ApprovalInstanceMapper.class);
        ApprovalTaskMapper taskMapper = mock(ApprovalTaskMapper.class);
        ApprovalAuditEventMapper auditMapper = mock(ApprovalAuditEventMapper.class);
        ApprovalDefinitionDO definition = definition();
        definition.setExternalProvider("LARK");
        definition.setExternalDefinitionCode("lark-definition-code");
        ApprovalTaskDO task = task(201L, 101L, "bob", "PENDING");
        task.setExternalTaskId("lark-task-201");
        ApprovalInstanceDO instance = instance("PENDING");
        instance.setExternalInstanceId("lark-instance-101");
        when(taskMapper.selectById(201L)).thenReturn(task);
        when(instanceMapper.selectById(101L)).thenReturn(instance);
        when(definitionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(definition);
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));
        RecordingExternalApprovalProvider provider = new RecordingExternalApprovalProvider();
        ApprovalWorkflowService service = service(definitionMapper, instanceMapper, taskMapper, auditMapper,
                List.of(), List.of(provider));

        ApprovalInstanceView view = service.approveTask(new ApprovalDecisionCommand(
                7L,
                201L,
                "bob",
                RoleNames.OPERATOR,
                "检查通过"));

        assertThat(provider.actions).containsExactly("APPROVE:lark-instance-101:lark-task-201:bob:检查通过");
        assertThat(provider.decisionLocalStatuses).containsExactly("PENDING:PENDING");
        assertThat(task.getStatus()).isEqualTo("APPROVED");
        assertThat(instance.getStatus()).isEqualTo("APPROVED");
        assertThat(view.status()).isEqualTo("APPROVED");
    }

    @Test
    void larkProviderDecisionFailureDoesNotUpdateLocalTaskOrInstance() {
        ApprovalDefinitionMapper definitionMapper = mock(ApprovalDefinitionMapper.class);
        ApprovalInstanceMapper instanceMapper = mock(ApprovalInstanceMapper.class);
        ApprovalTaskMapper taskMapper = mock(ApprovalTaskMapper.class);
        ApprovalAuditEventMapper auditMapper = mock(ApprovalAuditEventMapper.class);
        ApprovalDefinitionDO definition = definition();
        definition.setExternalProvider("LARK");
        definition.setExternalDefinitionCode("lark-definition-code");
        ApprovalTaskDO task = task(201L, 101L, "bob", "PENDING");
        task.setExternalTaskId("lark-task-201");
        ApprovalInstanceDO instance = instance("PENDING");
        instance.setExternalInstanceId("lark-instance-101");
        when(taskMapper.selectById(201L)).thenReturn(task);
        when(instanceMapper.selectById(101L)).thenReturn(instance);
        when(definitionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(definition);
        RecordingExternalApprovalProvider provider = new RecordingExternalApprovalProvider();
        provider.failDecision = true;
        ApprovalWorkflowService service = service(definitionMapper, instanceMapper, taskMapper, auditMapper,
                List.of(), List.of(provider));

        assertThatThrownBy(() -> service.approveTask(new ApprovalDecisionCommand(
                7L,
                201L,
                "bob",
                RoleNames.OPERATOR,
                "检查通过")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("external decision failed");

        assertThat(provider.decisionLocalStatuses).containsExactly("PENDING:PENDING");
        assertThat(task.getStatus()).isEqualTo("PENDING");
        assertThat(instance.getStatus()).isEqualTo("PENDING");
        verify(taskMapper, never()).updateById(any(ApprovalTaskDO.class));
        verify(instanceMapper, never()).updateById(any(ApprovalInstanceDO.class));
    }

    @Test
    void syncExternalLarkInstanceUpdatesLocalInstanceAndTasks() {
        ApprovalDefinitionMapper definitionMapper = mock(ApprovalDefinitionMapper.class);
        ApprovalInstanceMapper instanceMapper = mock(ApprovalInstanceMapper.class);
        ApprovalTaskMapper taskMapper = mock(ApprovalTaskMapper.class);
        ApprovalAuditEventMapper auditMapper = mock(ApprovalAuditEventMapper.class);
        ApprovalDefinitionDO definition = definition();
        definition.setExternalProvider("LARK");
        ApprovalInstanceDO instance = instance("PENDING");
        instance.setExternalInstanceId("lark-instance-101");
        ApprovalTaskDO task = task(201L, 101L, "bob", "PENDING");
        task.setExternalTaskId("lark-task-201");
        when(instanceMapper.selectById(101L)).thenReturn(instance);
        when(definitionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(definition);
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));
        RecordingExternalApprovalProvider provider = new RecordingExternalApprovalProvider();
        provider.syncResult = new ApprovalExternalSyncResult(
                "APPROVED",
                Map.of("lark-task-201", "APPROVED"));
        RecordingAutoActionHandler handler = new RecordingAutoActionHandler();
        ApprovalWorkflowService service = service(definitionMapper, instanceMapper, taskMapper, auditMapper,
                List.of(handler), List.of(provider));

        ApprovalInstanceView view = service.syncExternalInstance(7L, 101L);

        assertThat(provider.syncs).containsExactly("CANVAS_PUBLISH_DEFAULT:lark-instance-101");
        assertThat(task.getStatus()).isEqualTo("APPROVED");
        assertThat(instance.getStatus()).isEqualTo("APPROVED");
        assertThat(instance.getCompletedBy()).isEqualTo("lark");
        assertThat(instance.getAutoActionStatus()).isEqualTo("SUCCESS");
        assertThat(handler.executions).containsExactly("101:lark");
        assertThat(view.status()).isEqualTo("APPROVED");
        verify(taskMapper).updateById(task);
        verify(instanceMapper, org.mockito.Mockito.atLeastOnce()).updateById(instance);
        ArgumentCaptor<ApprovalAuditEventDO> audit = ArgumentCaptor.forClass(ApprovalAuditEventDO.class);
        verify(auditMapper, org.mockito.Mockito.atLeastOnce()).insert(audit.capture());
        assertThat(audit.getAllValues())
                .extracting(ApprovalAuditEventDO::getEventType)
                .contains("EXTERNAL_SYNC_TASK_APPROVED", "EXTERNAL_SYNC_INSTANCE_APPROVED");
    }

    @Test
    void syncPendingExternalInstancesSyncsBoundPendingInstancesWithinLimit() {
        ApprovalDefinitionMapper definitionMapper = mock(ApprovalDefinitionMapper.class);
        ApprovalInstanceMapper instanceMapper = mock(ApprovalInstanceMapper.class);
        ApprovalTaskMapper taskMapper = mock(ApprovalTaskMapper.class);
        ApprovalAuditEventMapper auditMapper = mock(ApprovalAuditEventMapper.class);
        ApprovalDefinitionDO definition = definition();
        definition.setExternalProvider("LARK");
        ApprovalInstanceDO first = instance("PENDING");
        first.setId(101L);
        first.setExternalInstanceId("lark-instance-101");
        ApprovalInstanceDO second = instance("PENDING");
        second.setId(102L);
        second.setExternalInstanceId("lark-instance-102");
        ApprovalTaskDO firstTask = task(201L, 101L, "bob", "PENDING");
        firstTask.setExternalTaskId("lark-task-201");
        ApprovalTaskDO secondTask = task(202L, 102L, "bob", "PENDING");
        secondTask.setExternalTaskId("lark-task-202");
        when(instanceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(first, second));
        when(definitionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(definition);
        when(taskMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(firstTask))
                .thenReturn(List.of(firstTask))
                .thenReturn(List.of(secondTask))
                .thenReturn(List.of(secondTask));
        RecordingExternalApprovalProvider provider = new RecordingExternalApprovalProvider();
        provider.syncResults.put("lark-instance-101", new ApprovalExternalSyncResult(
                "APPROVED",
                Map.of("lark-task-201", "APPROVED")));
        provider.syncResults.put("lark-instance-102", new ApprovalExternalSyncResult(
                "REJECTED",
                Map.of("lark-task-202", "REJECTED")));
        ApprovalWorkflowService service = service(definitionMapper, instanceMapper, taskMapper, auditMapper,
                List.of(), List.of(provider));

        int synced = service.syncPendingExternalInstances(7L, 20);

        assertThat(synced).isEqualTo(2);
        assertThat(provider.syncs)
                .containsExactly(
                        "CANVAS_PUBLISH_DEFAULT:lark-instance-101",
                        "CANVAS_PUBLISH_DEFAULT:lark-instance-102");
        assertThat(first.getStatus()).isEqualTo("APPROVED");
        assertThat(second.getStatus()).isEqualTo("REJECTED");
        verify(instanceMapper, org.mockito.Mockito.atLeast(2)).updateById(any(ApprovalInstanceDO.class));
        verify(taskMapper, org.mockito.Mockito.times(2)).updateById(any(ApprovalTaskDO.class));
    }

    private ApprovalWorkflowService service(ApprovalDefinitionMapper definitionMapper,
                                            ApprovalInstanceMapper instanceMapper,
                                            ApprovalTaskMapper taskMapper,
                                            ApprovalAuditEventMapper auditMapper,
                                            List<ApprovalAutoActionHandler> handlers) {
        return service(definitionMapper, instanceMapper, taskMapper, auditMapper, handlers, List.of());
    }

    private ApprovalWorkflowService service(ApprovalDefinitionMapper definitionMapper,
                                            ApprovalInstanceMapper instanceMapper,
                                            ApprovalTaskMapper taskMapper,
                                            ApprovalAuditEventMapper auditMapper,
                                            List<ApprovalAutoActionHandler> handlers,
                                            List<ApprovalExternalProvider> externalProviders) {
        return new ApprovalWorkflowService(
                definitionMapper,
                instanceMapper,
                taskMapper,
                auditMapper,
                handlers,
                externalProviders,
                CLOCK);
    }

    private ApprovalDefinitionDO definition() {
        ApprovalDefinitionDO row = new ApprovalDefinitionDO();
        row.setId(1L);
        row.setTenantId(0L);
        row.setDefinitionKey("CANVAS_PUBLISH_DEFAULT");
        row.setName("Canvas publish approval");
        row.setDomain("CANVAS");
        row.setTargetType("CANVAS");
        row.setEnabled(1);
        row.setMode("ANY_ONE");
        row.setMinApprovals(1);
        row.setDefaultDueHours(24);
        row.setExternalProvider("LOCAL");
        return row;
    }

    private ApprovalTaskDO task(Long id, Long instanceId, String approver, String status) {
        ApprovalTaskDO row = new ApprovalTaskDO();
        row.setId(id);
        row.setTenantId(7L);
        row.setInstanceId(instanceId);
        row.setStepNo(1);
        row.setApprover(approver);
        row.setStatus(status);
        return row;
    }

    private ApprovalInstanceDO instance(String status) {
        ApprovalInstanceDO row = new ApprovalInstanceDO();
        row.setId(101L);
        row.setTenantId(7L);
        row.setDefinitionKey("CANVAS_PUBLISH_DEFAULT");
        row.setDomain("CANVAS");
        row.setTargetType("CANVAS");
        row.setTargetId("62");
        row.setTargetVersionId(91L);
        row.setStatus(status);
        row.setSubmitter("alice");
        row.setRequestedAt(LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC));
        row.setAutoAction("PUBLISH_CANVAS");
        row.setAutoActionStatus("PENDING");
        return row;
    }

    private static final class RecordingAutoActionHandler implements ApprovalAutoActionHandler {
        private final List<String> executions = new ArrayList<>();

        @Override
        public boolean supports(String autoAction) {
            return "PUBLISH_CANVAS".equals(autoAction);
        }

        @Override
        public void execute(ApprovalInstanceDO instance, String actor) {
            executions.add(instance.getId() + ":" + actor);
        }
    }

    private static final class RecordingExternalApprovalProvider implements ApprovalExternalProvider {
        private final List<String> submissions = new ArrayList<>();
        private final List<String> actions = new ArrayList<>();
        private final List<String> decisionLocalStatuses = new ArrayList<>();
        private final List<String> syncs = new ArrayList<>();
        private final Map<String, ApprovalExternalSyncResult> syncResults = new java.util.LinkedHashMap<>();
        private ApprovalExternalSyncResult syncResult;
        private boolean failDecision;

        @Override
        public boolean supports(String provider) {
            return "LARK".equalsIgnoreCase(provider);
        }

        @Override
        public ApprovalExternalSubmissionResult submit(ApprovalDefinitionDO definition,
                                                       ApprovalInstanceDO instance,
                                                       List<ApprovalTaskDO> tasks,
                                                       ApprovalSubmitCommand command) {
            submissions.add(definition.getDefinitionKey() + ":" + definition.getExternalDefinitionCode()
                    + ":" + instance.getId());
            return new ApprovalExternalSubmissionResult(
                    "lark-instance-" + instance.getId(),
                    Map.of(201L, "lark-task-201", 202L, "lark-task-202"));
        }

        @Override
        public void decide(ApprovalDefinitionDO definition,
                           ApprovalInstanceDO instance,
                           ApprovalTaskDO task,
                           ApprovalDecisionCommand command,
                           boolean approve) {
            decisionLocalStatuses.add(task.getStatus() + ":" + instance.getStatus());
            if (failDecision) {
                throw new IllegalStateException("external decision failed");
            }
            actions.add((approve ? "APPROVE" : "REJECT")
                    + ":" + instance.getExternalInstanceId()
                    + ":" + task.getExternalTaskId()
                    + ":" + command.actor()
                    + ":" + command.comment());
        }

        @Override
        public ApprovalExternalSyncResult sync(ApprovalDefinitionDO definition, ApprovalInstanceDO instance) {
            syncs.add(definition.getDefinitionKey() + ":" + instance.getExternalInstanceId());
            return syncResults.getOrDefault(instance.getExternalInstanceId(), syncResult);
        }
    }
}
