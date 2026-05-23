package org.chovy.canvas.controller;

import org.chovy.canvas.domain.audience.AudienceDefinition;
import org.chovy.canvas.domain.audience.AudienceDefinitionMapper;
import org.chovy.canvas.domain.audience.AudienceStatMapper;
import org.chovy.canvas.domain.notification.NotificationService;
import org.chovy.canvas.domain.task.AsyncTask;
import org.chovy.canvas.domain.task.AsyncTaskCreateResult;
import org.chovy.canvas.domain.task.AsyncTaskService;
import org.chovy.canvas.dto.task.ComputeTaskResp;
import org.chovy.canvas.engine.audience.AudienceBatchComputeService;
import org.chovy.canvas.engine.audience.AudienceComputeTaskRunner;
import org.chovy.canvas.engine.audience.AudienceSchedulerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AudienceControllerTaskTest {

    @Mock
    private AudienceDefinitionMapper definitionMapper;
    @Mock
    private AudienceStatMapper statMapper;
    @Mock
    private AudienceBatchComputeService computeService;
    @Mock
    private AudienceSchedulerService schedulerService;
    @Mock
    private AsyncTaskService taskService;
    @Mock
    private AudienceComputeTaskRunner runner;
    @Mock
    private NotificationService notificationService;

    @Test
    void compute_returnsTaskIdAndStartsRunnerWhenTaskCreated() {
        AudienceDefinition definition = audience(7L, "VIP 人群");
        when(definitionMapper.selectById(7L)).thenReturn(definition);
        when(taskService.createOrReuseRunning(
                "AUDIENCE_COMPUTE",
                "AUDIENCE",
                "7",
                "计算人群：VIP 人群",
                "system"))
                .thenReturn(new AsyncTaskCreateResult(task("task_1", "QUEUED"), true));
        AudienceController controller = controller();

        var response = controller.compute(7L).block();

        assertThat(response.getCode()).isZero();
        ComputeTaskResp data = response.getData();
        assertThat(data.taskId()).isEqualTo("task_1");
        assertThat(data.status()).isEqualTo("QUEUED");
        verify(runner).start("task_1", 7L, "VIP 人群", "system");
    }

    @Test
    void compute_reusesExistingTaskWithoutStartingRunner() {
        AudienceDefinition definition = audience(7L, "VIP 人群");
        when(definitionMapper.selectById(7L)).thenReturn(definition);
        when(taskService.createOrReuseRunning(
                "AUDIENCE_COMPUTE",
                "AUDIENCE",
                "7",
                "计算人群：VIP 人群",
                "system"))
                .thenReturn(new AsyncTaskCreateResult(task("task_existing", "RUNNING"), false));
        AudienceController controller = controller();

        var response = controller.compute(7L).block();

        assertThat(response.getData().taskId()).isEqualTo("task_existing");
        assertThat(response.getData().status()).isEqualTo("RUNNING");
        verify(runner, never()).start(any(), any(), any(), any());
    }

    @Test
    void compute_reusedTerminalTaskCreatesCatchUpNotificationWithoutStartingRunner() {
        AudienceDefinition definition = audience(7L, "VIP 人群");
        when(definitionMapper.selectById(7L)).thenReturn(definition);
        when(taskService.createOrReuseRunning(
                "AUDIENCE_COMPUTE",
                "AUDIENCE",
                "7",
                "计算人群：VIP 人群",
                "system"))
                .thenReturn(new AsyncTaskCreateResult(task("task_existing", "SUCCEEDED"), false));
        AudienceController controller = controller();

        var response = controller.compute(7L).block();

        assertThat(response.getData().taskId()).isEqualTo("task_existing");
        assertThat(response.getData().status()).isEqualTo("SUCCEEDED");
        verify(runner, never()).start(any(), any(), any(), any());
        verify(notificationService).createForTask(
                "system",
                "TASK_SUCCEEDED",
                "人群计算完成",
                "VIP 人群 · 任务已完成",
                "/audiences?highlight=7&taskId=task_existing",
                "task_existing");
    }

    @Test
    void compute_rejectsMissingAudienceWithoutEnqueueing() {
        when(definitionMapper.selectById(7L)).thenReturn(null);
        AudienceController controller = controller();

        assertThatThrownBy(() -> controller.compute(7L).block())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Audience not found: 7");
        verifyNoInteractions(taskService, runner);
    }

    @Test
    void compute_rejectsDisabledAudienceWithoutEnqueueing() {
        AudienceDefinition definition = audience(7L, "VIP 人群");
        definition.setEnabled(0);
        when(definitionMapper.selectById(7L)).thenReturn(definition);
        AudienceController controller = controller();

        assertThatThrownBy(() -> controller.compute(7L).block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Audience disabled: 7");
        verifyNoInteractions(taskService, runner);
    }

    @Test
    void create_setsCreatedByAndEnqueuesCompute() {
        AudienceDefinition body = audience(null, "新建人群");
        AudienceDefinition created = audience(11L, "新建人群");
        when(computeService.create(body)).thenReturn(created);
        when(taskService.createOrReuseRunning(
                "AUDIENCE_COMPUTE",
                "AUDIENCE",
                "11",
                "计算人群：新建人群",
                "system"))
                .thenReturn(new AsyncTaskCreateResult(task("task_create", "QUEUED"), true));
        AudienceController controller = controller();

        var response = controller.create(body).block();

        assertThat(response.getData()).isSameAs(created);
        assertThat(body.getCreatedBy()).isEqualTo("system");
        verify(schedulerService).refresh(any(AudienceDefinition.class), any(Runnable.class));
        verify(runner).start("task_create", 11L, "新建人群", "system");
    }

    @Test
    void update_persistsAndEnqueuesSavedDefinition() {
        AudienceDefinition body = audience(null, "请求名称");
        AudienceDefinition saved = audience(12L, "保存后名称");
        when(computeService.update(body)).thenReturn(true);
        when(definitionMapper.selectById(12L)).thenReturn(saved);
        when(taskService.createOrReuseRunning(
                "AUDIENCE_COMPUTE",
                "AUDIENCE",
                "12",
                "计算人群：保存后名称",
                "system"))
                .thenReturn(new AsyncTaskCreateResult(task("task_update", "QUEUED"), true));
        AudienceController controller = controller();

        var response = controller.update(12L, body).block();

        assertThat(response.getCode()).isZero();
        assertThat(body.getId()).isEqualTo(12L);
        verify(computeService).update(body);
        verify(schedulerService).refresh(same(saved), any(Runnable.class));
        verify(runner).start("task_update", 12L, "保存后名称", "system");
    }

    @Test
    void update_rejectsMissingAudienceWithoutSchedulingOrEnqueueing() {
        AudienceDefinition body = audience(null, "请求名称");
        when(computeService.update(body)).thenReturn(false);
        AudienceController controller = controller();

        assertThatThrownBy(() -> controller.update(12L, body).block())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Audience not found: 12");
        assertThat(body.getId()).isEqualTo(12L);
        verify(computeService).update(body);
        verifyNoInteractions(schedulerService, taskService, runner);
    }

    @Test
    void update_rejectsMissingSavedAudienceWithoutSchedulingOrEnqueueing() {
        AudienceDefinition body = audience(null, "请求名称");
        when(computeService.update(body)).thenReturn(true);
        when(definitionMapper.selectById(12L)).thenReturn(null);
        AudienceController controller = controller();

        assertThatThrownBy(() -> controller.update(12L, body).block())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Audience not found: 12");
        assertThat(body.getId()).isEqualTo(12L);
        verify(computeService).update(body);
        verify(definitionMapper).selectById(12L);
        verifyNoInteractions(schedulerService, taskService, runner);
    }

    @Test
    void compute_usesFallbackNameWhenDefinitionNameIsBlank() {
        AudienceDefinition definition = audience(13L, " ");
        when(definitionMapper.selectById(13L)).thenReturn(definition);
        when(taskService.createOrReuseRunning(
                "AUDIENCE_COMPUTE",
                "AUDIENCE",
                "13",
                "计算人群：人群 13",
                "system"))
                .thenReturn(new AsyncTaskCreateResult(task("task_fallback", "QUEUED"), true));
        AudienceController controller = controller();

        var response = controller.compute(13L).block();

        assertThat(response.getData().taskId()).isEqualTo("task_fallback");
        verify(runner).start("task_fallback", 13L, "人群 13", "system");
    }

    private AudienceController controller() {
        return new AudienceController(
                definitionMapper,
                statMapper,
                computeService,
                schedulerService,
                taskService,
                runner,
                notificationService);
    }

    private AudienceDefinition audience(Long id, String name) {
        AudienceDefinition definition = new AudienceDefinition();
        definition.setId(id);
        definition.setName(name);
        definition.setEnabled(1);
        return definition;
    }

    private AsyncTask task(String taskId, String status) {
        AsyncTask task = new AsyncTask();
        task.setTaskId(taskId);
        task.setStatus(status);
        return task;
    }
}
