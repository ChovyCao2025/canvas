package org.chovy.canvas.engine.trigger;

import org.chovy.canvas.domain.canvas.CanvasMapper;
import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.infra.cache.CanvasConfigCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasSchedulerJitterTest {

    @Test
    @DisplayName("jitterMaxMs=0 时返回零延迟")
    void no_jitter_triggers_immediately() {
        Duration d = CanvasSchedulerService.calcJitter(0);
        assertThat(d).isEqualTo(Duration.ZERO);
    }

    @Test
    @DisplayName("zero jitter 会同步 dispatch 且不创建延迟订阅")
    void zero_jitter_dispatches_synchronously() {
        SchedulerFixture fixture = newFixture();
        CanvasSchedulerService service = fixture.service();
        CanvasExecutionService executionService = fixture.executionService();
        CanvasSchedulerService.PendingJitterGroup group = service.createPendingJitterGroup("1:node-1");

        when(executionService.trigger(
                anyLong(), anyString(), anyString(), anyString(), any(),
                any(Map.class), anyString(), anyBoolean()))
                .thenReturn(Mono.just(Map.of()));

        Disposable pending = service.scheduleTriggerWithJitter(group, 1L, "user-0", Duration.ZERO);

        assertThat(pending.isDisposed()).isTrue();
        verify(executionService, times(1)).trigger(
                anyLong(), anyString(), anyString(), anyString(), any(),
                any(Map.class), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("jitterMaxMs=60000 时延迟在 [0, 60000) ms 范围内")
    void jitter_in_range() {
        for (int i = 0; i < 100; i++) {
            Duration d = CanvasSchedulerService.calcJitter(60_000L);
            assertThat(d.toMillis()).isBetween(0L, 59_999L);
        }
    }

    @Test
    @DisplayName("cancelTask 会取消尚未触发的 jitter 回调")
    void cancel_task_disposes_pending_jitter_callbacks() throws InterruptedException {
        SchedulerFixture fixture = newFixture();
        CanvasSchedulerService service = fixture.service();
        CanvasExecutionService executionService = fixture.executionService();

        when(executionService.trigger(
                anyLong(), anyString(), anyString(), anyString(), any(),
                any(Map.class), anyString(), anyBoolean()))
                .thenReturn(Mono.just(Map.of()));

        CanvasSchedulerService.PendingJitterGroup group = service.createPendingJitterGroup("1:node-1");
        Disposable pending = service.scheduleTriggerWithJitter(group, 1L, "user-1", Duration.ofMillis(200));
        service.cancelTask("1:node-1");

        Thread.sleep(350);

        assertThat(pending.isDisposed()).isTrue();
        verify(executionService, never()).trigger(
                anyLong(), anyString(), anyString(), anyString(), any(),
                any(Map.class), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("已取消的生命周期组不会接受新的 jitter 调度")
    void canceled_group_rejects_future_scheduling() throws InterruptedException {
        SchedulerFixture fixture = newFixture();
        CanvasSchedulerService service = fixture.service();
        CanvasExecutionService executionService = fixture.executionService();

        when(executionService.trigger(
                anyLong(), anyString(), anyString(), anyString(), any(),
                any(Map.class), anyString(), anyBoolean()))
                .thenReturn(Mono.just(Map.of()));

        CanvasSchedulerService.PendingJitterGroup group = service.createPendingJitterGroup("1:node-1");
        service.cancelTask("1:node-1");

        Disposable pending = service.scheduleTriggerWithJitter(group, 1L, "user-2", Duration.ofMillis(80));

        Thread.sleep(200);

        assertThat(pending.isDisposed()).isTrue();
        verify(executionService, never()).trigger(
                anyLong(), anyString(), anyString(), anyString(), any(),
                any(Map.class), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("cancelAll 后旧 group 不能再追加零延迟 jitter")
    void canceled_group_rejects_zero_delay_after_cancel_all() {
        SchedulerFixture fixture = newFixture();
        CanvasSchedulerService service = fixture.service();
        CanvasExecutionService executionService = fixture.executionService();

        when(executionService.trigger(
                anyLong(), anyString(), anyString(), anyString(), any(),
                any(Map.class), anyString(), anyBoolean()))
                .thenReturn(Mono.just(Map.of()));

        CanvasSchedulerService.PendingJitterGroup group = service.createPendingJitterGroup("1:node-1");
        service.cancelAll();

        Disposable pending = service.scheduleTriggerWithJitter(group, 1L, "user-3", Duration.ZERO);

        assertThat(pending.isDisposed()).isTrue();
        verify(executionService, never()).trigger(
                anyLong(), anyString(), anyString(), anyString(), any(),
                any(Map.class), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("已终止 group 的延迟回调不会 dispatch trigger")
    void terminated_group_does_not_dispatch_trigger() {
        SchedulerFixture fixture = newFixture();
        CanvasSchedulerService service = fixture.service();
        CanvasExecutionService executionService = fixture.executionService();

        when(executionService.trigger(
                anyLong(), anyString(), anyString(), anyString(), any(),
                any(Map.class), anyString(), anyBoolean()))
                .thenReturn(Mono.just(Map.of()));

        CanvasSchedulerService.PendingJitterGroup group = service.createPendingJitterGroup("1:node-1");
        group.terminate();

        service.dispatchScheduledTrigger(group, 1L, "user-4");

        verify(executionService, never()).trigger(
                anyLong(), anyString(), anyString(), anyString(), any(),
                any(Map.class), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("未实际注册调度时不会留下 orphan jitter group")
    void no_orphan_group_when_schedule_not_installed() {
        SchedulerFixture fixture = newFixture();
        CanvasSchedulerService service = fixture.service();
        DagGraph graph = Mockito.mock(DagGraph.class);
        DagParser.CanvasNode node = Mockito.mock(DagParser.CanvasNode.class);

        when(graph.allNodeIds()).thenReturn(Set.of("node-1"));
        when(graph.getNode("node-1")).thenReturn(node);
        when(node.getType()).thenReturn(NodeType.SCHEDULED_TRIGGER);
        when(node.getBizConfig()).thenReturn(Map.of());
        when(node.getConfig()).thenReturn(Map.of());

        service.registerScheduledTriggers(1L, graph);

        assertThat(service.hasPendingJitterGroup("1:node-1")).isFalse();
    }

    @Test
    @DisplayName("triggerTime 解析失败时不会留下 orphan jitter group")
    void no_orphan_group_when_trigger_time_is_invalid() {
        SchedulerFixture fixture = newFixture();
        CanvasSchedulerService service = fixture.service();
        DagGraph graph = Mockito.mock(DagGraph.class);
        DagParser.CanvasNode node = Mockito.mock(DagParser.CanvasNode.class);

        when(graph.allNodeIds()).thenReturn(Set.of("node-1"));
        when(graph.getNode("node-1")).thenReturn(node);
        when(node.getType()).thenReturn(NodeType.SCHEDULED_TRIGGER);
        when(node.getBizConfig()).thenReturn(Map.of("triggerTime", "invalid-time"));
        when(node.getConfig()).thenReturn(Map.of());

        assertThatThrownBy(() -> service.registerScheduledTriggers(1L, graph))
                .isInstanceOf(RuntimeException.class);

        assertThat(service.hasPendingJitterGroup("1:node-1")).isFalse();
    }

    @Test
    @DisplayName("one-shot 调度成功执行后会在 idle 时清理 group 和 active task")
    void one_shot_group_is_cleaned_up_after_successful_run() throws InterruptedException {
        SchedulerFixture fixture = newFixture();
        CanvasSchedulerService service = fixture.service();
        CanvasExecutionService executionService = fixture.executionService();
        TaskScheduler taskScheduler = fixture.taskScheduler();
        DagGraph graph = Mockito.mock(DagGraph.class);
        DagParser.CanvasNode node = Mockito.mock(DagParser.CanvasNode.class);
        AtomicReference<Runnable> scheduledJob = new AtomicReference<>();
        ScheduledFuture<?> future = Mockito.mock(ScheduledFuture.class);

        when(executionService.trigger(
                anyLong(), anyString(), anyString(), anyString(), any(),
                any(Map.class), anyString(), anyBoolean()))
                .thenReturn(Mono.just(Map.of()));
        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class))).thenAnswer(invocation -> {
            scheduledJob.set(invocation.getArgument(0));
            return future;
        });
        when(graph.allNodeIds()).thenReturn(Set.of("node-1"));
        when(graph.getNode("node-1")).thenReturn(node);
        when(node.getType()).thenReturn(NodeType.SCHEDULED_TRIGGER);
        when(node.getBizConfig()).thenReturn(Map.of(
                "triggerTime", "2026-05-23T14:00:00",
                "userSource", Map.of("type", "USER_LIST", "userIds", java.util.List.of("user-1"))));
        when(node.getConfig()).thenReturn(Map.of());

        service.registerScheduledTriggers(1L, graph);
        scheduledJob.get().run();

        waitUntil(() -> !service.hasPendingJitterGroup("1:node-1") && !service.hasActiveTask("1:node-1"));

        assertThat(service.hasPendingJitterGroup("1:node-1")).isFalse();
        assertThat(service.hasActiveTask("1:node-1")).isFalse();
    }

    @Test
    @DisplayName("one-shot 执行同步异常后也会清理 group 和 active task")
    void one_shot_group_is_cleaned_up_after_execution_exception() throws InterruptedException {
        SchedulerFixture fixture = newFixture();
        CanvasSchedulerService service = fixture.service();
        TaskScheduler taskScheduler = fixture.taskScheduler();
        DagGraph graph = Mockito.mock(DagGraph.class);
        DagParser.CanvasNode node = Mockito.mock(DagParser.CanvasNode.class);
        AtomicReference<Runnable> scheduledJob = new AtomicReference<>();
        ScheduledFuture<?> future = Mockito.mock(ScheduledFuture.class);

        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class))).thenAnswer(invocation -> {
            scheduledJob.set(invocation.getArgument(0));
            return future;
        });
        when(graph.allNodeIds()).thenReturn(Set.of("node-1"));
        when(graph.getNode("node-1")).thenReturn(node);
        when(node.getType()).thenReturn(NodeType.SCHEDULED_TRIGGER);
        when(node.getBizConfig()).thenReturn(Map.of(
                "triggerTime", "2026-05-23T14:00:00",
                "userSource", "invalid-user-source"));
        when(node.getConfig()).thenReturn(Map.of());

        service.registerScheduledTriggers(1L, graph);

        assertThatThrownBy(() -> scheduledJob.get().run())
                .isInstanceOf(ClassCastException.class);

        waitUntil(() -> !service.hasPendingJitterGroup("1:node-1") && !service.hasActiveTask("1:node-1"));

        assertThat(service.hasPendingJitterGroup("1:node-1")).isFalse();
        assertThat(service.hasActiveTask("1:node-1")).isFalse();
    }

    @Test
    @DisplayName("one-shot 在 schedule 期间同步执行完成后不会复活 active task")
    void one_shot_does_not_resurrect_active_task_after_immediate_execution() {
        SchedulerFixture fixture = newFixture();
        CanvasSchedulerService service = fixture.service();
        TaskScheduler taskScheduler = fixture.taskScheduler();
        CanvasExecutionService executionService = fixture.executionService();
        DagGraph graph = Mockito.mock(DagGraph.class);
        DagParser.CanvasNode node = Mockito.mock(DagParser.CanvasNode.class);
        ScheduledFuture<?> future = Mockito.mock(ScheduledFuture.class);

        when(executionService.trigger(
                anyLong(), anyString(), anyString(), anyString(), any(),
                any(Map.class), anyString(), anyBoolean()))
                .thenReturn(Mono.just(Map.of()));
        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class))).thenAnswer(invocation -> {
            Runnable job = invocation.getArgument(0);
            job.run();
            return future;
        });
        when(graph.allNodeIds()).thenReturn(Set.of("node-1"));
        when(graph.getNode("node-1")).thenReturn(node);
        when(node.getType()).thenReturn(NodeType.SCHEDULED_TRIGGER);
        when(node.getBizConfig()).thenReturn(Map.of(
                "triggerTime", "2026-05-23T14:00:00",
                "userSource", Map.of("type", "USER_LIST", "userIds", java.util.List.of())));
        when(node.getConfig()).thenReturn(Map.of());

        service.registerScheduledTriggers(1L, graph);

        assertThat(service.hasPendingJitterGroup("1:node-1")).isFalse();
        assertThat(service.hasActiveTask("1:node-1")).isFalse();
    }

    @Test
    @DisplayName("cron 在 schedule 返回前被取消时不会复活 active task")
    void cron_does_not_resurrect_active_task_after_cancel_during_schedule() {
        SchedulerFixture fixture = newFixture();
        CanvasSchedulerService service = fixture.service();
        TaskScheduler taskScheduler = fixture.taskScheduler();
        ScheduledFuture<?> future = Mockito.mock(ScheduledFuture.class);
        DagGraph graph = Mockito.mock(DagGraph.class);
        DagParser.CanvasNode node = Mockito.mock(DagParser.CanvasNode.class);

        when(taskScheduler.schedule(any(Runnable.class), any(Trigger.class))).thenAnswer(invocation -> {
            service.cancelTask("1:node-1");
            return future;
        });
        when(graph.allNodeIds()).thenReturn(Set.of("node-1"));
        when(graph.getNode("node-1")).thenReturn(node);
        when(node.getType()).thenReturn(NodeType.SCHEDULED_TRIGGER);
        when(node.getBizConfig()).thenReturn(Map.of("cronExpression", "0 0/5 * * * ?"));
        when(node.getConfig()).thenReturn(Map.of());

        service.registerScheduledTriggers(1L, graph);

        assertThat(service.hasPendingJitterGroup("1:node-1")).isFalse();
        assertThat(service.hasActiveTask("1:node-1")).isFalse();
        verify(future, times(1)).cancel(false);
    }

    @Test
    @DisplayName("cancelAll 后不会再创建新的调度生命周期")
    void cancel_all_prevents_new_registration() {
        SchedulerFixture fixture = newFixture();
        CanvasSchedulerService service = fixture.service();
        TaskScheduler taskScheduler = fixture.taskScheduler();
        DagGraph graph = Mockito.mock(DagGraph.class);
        DagParser.CanvasNode node = Mockito.mock(DagParser.CanvasNode.class);

        service.cancelAll();

        when(graph.allNodeIds()).thenReturn(Set.of("node-1"));
        when(graph.getNode("node-1")).thenReturn(node);
        when(node.getType()).thenReturn(NodeType.SCHEDULED_TRIGGER);
        when(node.getBizConfig()).thenReturn(Map.of("cronExpression", "0 0/5 * * * ?"));
        when(node.getConfig()).thenReturn(Map.of());

        service.registerScheduledTriggers(1L, graph);

        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Trigger.class));
        assertThat(service.hasPendingJitterGroup("1:node-1")).isFalse();
        assertThat(service.hasActiveTask("1:node-1")).isFalse();
    }

    @Test
    @DisplayName("cancelAll 在 cron schedule 过程中发生时不会复活 active task")
    void cancel_all_during_cron_schedule_prevents_activation() {
        SchedulerFixture fixture = newFixture();
        CanvasSchedulerService service = fixture.service();
        TaskScheduler taskScheduler = fixture.taskScheduler();
        ScheduledFuture<?> future = Mockito.mock(ScheduledFuture.class);
        DagGraph graph = Mockito.mock(DagGraph.class);
        DagParser.CanvasNode node = Mockito.mock(DagParser.CanvasNode.class);

        when(taskScheduler.schedule(any(Runnable.class), any(Trigger.class))).thenAnswer(invocation -> {
            service.cancelAll();
            return future;
        });
        when(graph.allNodeIds()).thenReturn(Set.of("node-1"));
        when(graph.getNode("node-1")).thenReturn(node);
        when(node.getType()).thenReturn(NodeType.SCHEDULED_TRIGGER);
        when(node.getBizConfig()).thenReturn(Map.of("cronExpression", "0 0/5 * * * ?"));
        when(node.getConfig()).thenReturn(Map.of());

        service.registerScheduledTriggers(1L, graph);

        assertThat(service.hasPendingJitterGroup("1:node-1")).isFalse();
        assertThat(service.hasActiveTask("1:node-1")).isFalse();
        verify(future, times(1)).cancel(false);
    }

    private SchedulerFixture newFixture() {
        TaskScheduler taskScheduler = Mockito.mock(TaskScheduler.class);
        CanvasMapper canvasMapper = Mockito.mock(CanvasMapper.class);
        CanvasConfigCache configCache = Mockito.mock(CanvasConfigCache.class);
        CanvasExecutionService executionService = Mockito.mock(CanvasExecutionService.class);
        return new SchedulerFixture(
                new CanvasSchedulerService(taskScheduler, canvasMapper, configCache, executionService),
                executionService,
                taskScheduler);
    }

    private void waitUntil(CheckedCondition condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 1000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.evaluate()) return;
            Thread.sleep(10);
        }
        throw new AssertionError("Condition not met before timeout");
    }

    @FunctionalInterface
    private interface CheckedCondition {
        boolean evaluate();
    }

    private record SchedulerFixture(
            CanvasSchedulerService service,
            CanvasExecutionService executionService,
            TaskScheduler taskScheduler) {}
}
