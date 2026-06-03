package org.chovy.canvas.engine.audience;

import org.chovy.canvas.domain.notification.NotificationService;
import org.chovy.canvas.domain.task.AsyncTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Audience Compute Task Runner 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
@ExtendWith(MockitoExtension.class)
class AudienceComputeTaskRunnerTest {

    @Mock
    private AudienceBatchComputeService computeService;
    @Mock
    private AsyncTaskService asyncTaskService;
    @Mock
    private NotificationService notificationService;

    @Test
    void runNow_marksTaskSucceededAndCreatesSuccessNotification() {
        when(computeService.compute(7L)).thenReturn(AudienceComputeResult.ready(7L, "VIP 人群", 12L, 2));
        when(asyncTaskService.subscribers("task_1")).thenReturn(List.of("operator"));
        AudienceComputeTaskRunner runner = new AudienceComputeTaskRunner(computeService, asyncTaskService, notificationService);

        runner.runNow("task_1", 7L, "VIP 人群", "operator");

        verify(asyncTaskService).markRunning("task_1");
        verify(asyncTaskService).markSucceeded("task_1", "{\"audienceId\":7,\"estimatedSize\":12,\"bitmapSizeKb\":2}");
        verify(notificationService).createForTask(
                "operator",
                "TASK_SUCCEEDED",
                "人群计算完成",
                "VIP 人群 · 12 人",
                "/audiences?highlight=7&taskId=task_1",
                "task_1");
    }

    @Test
    void runNow_marksTaskFailedAndCreatesFailureNotification() {
        when(computeService.compute(7L)).thenReturn(AudienceComputeResult.failed(7L, "VIP 人群", "JDBC timeout"));
        when(asyncTaskService.subscribers("task_2")).thenReturn(List.of("operator"));
        AudienceComputeTaskRunner runner = new AudienceComputeTaskRunner(computeService, asyncTaskService, notificationService);

        runner.runNow("task_2", 7L, "VIP 人群", "operator");

        verify(asyncTaskService).markRunning("task_2");
        verify(asyncTaskService).markFailed("task_2", "JDBC timeout");
        verify(notificationService).createForTask(
                "operator",
                "TASK_FAILED",
                "人群计算失败",
                "VIP 人群 · JDBC timeout",
                "/audiences?highlight=7&taskId=task_2",
                "task_2");
    }

    @Test
    void runNow_marksFailedWhenComputeThrows() {
        when(computeService.compute(7L)).thenThrow(new RuntimeException("boom"));
        when(asyncTaskService.subscribers("task_3")).thenReturn(List.of("operator"));
        AudienceComputeTaskRunner runner = new AudienceComputeTaskRunner(computeService, asyncTaskService, notificationService);

        assertDoesNotThrow(() -> runner.runNow("task_3", 7L, "VIP 人群", "operator"));

        verify(asyncTaskService).markRunning("task_3");
        verify(asyncTaskService).markFailed("task_3", "boom");
        verify(notificationService).createForTask(
                "operator",
                "TASK_FAILED",
                "人群计算失败",
                "VIP 人群 · boom",
                "/audiences?highlight=7&taskId=task_3",
                "task_3");
    }

    @Test
    void runNow_doesNotThrowWhenNotificationFailsAfterSuccess() {
        when(computeService.compute(7L)).thenReturn(AudienceComputeResult.ready(7L, "VIP 人群", 12L, 2));
        when(asyncTaskService.subscribers("task_4")).thenReturn(List.of("operator"));
        doThrow(new RuntimeException("notification down")).when(notificationService).createForTask(
                "operator",
                "TASK_SUCCEEDED",
                "人群计算完成",
                "VIP 人群 · 12 人",
                "/audiences?highlight=7&taskId=task_4",
                "task_4");
        AudienceComputeTaskRunner runner = new AudienceComputeTaskRunner(computeService, asyncTaskService, notificationService);

        assertDoesNotThrow(() -> runner.runNow("task_4", 7L, "VIP 人群", "operator"));

        verify(asyncTaskService).markRunning("task_4");
        verify(asyncTaskService).markSucceeded("task_4", "{\"audienceId\":7,\"estimatedSize\":12,\"bitmapSizeKb\":2}");
    }

    @Test
    void runNow_usesProvidedAudienceNameWhenResultNameIsNull() {
        when(computeService.compute(7L)).thenReturn(AudienceComputeResult.ready(7L, null, 12L, 2));
        when(asyncTaskService.subscribers("task_5")).thenReturn(List.of("operator"));
        AudienceComputeTaskRunner runner = new AudienceComputeTaskRunner(computeService, asyncTaskService, notificationService);

        runner.runNow("task_5", 7L, "VIP 人群", "operator");

        verify(notificationService).createForTask(
                "operator",
                "TASK_SUCCEEDED",
                "人群计算完成",
                "VIP 人群 · 12 人",
                "/audiences?highlight=7&taskId=task_5",
                "task_5");
    }

    @Test
    void runNow_createsSuccessNotificationsForAllTaskSubscribers() {
        when(computeService.compute(7L)).thenReturn(AudienceComputeResult.ready(7L, "VIP 人群", 12L, 2));
        when(asyncTaskService.subscribers("task_shared")).thenReturn(List.of("alice", "bob"));
        AudienceComputeTaskRunner runner = new AudienceComputeTaskRunner(computeService, asyncTaskService, notificationService);

        runner.runNow("task_shared", 7L, "VIP 人群", "alice");

        verify(notificationService).createForTask(
                "alice",
                "TASK_SUCCEEDED",
                "人群计算完成",
                "VIP 人群 · 12 人",
                "/audiences?highlight=7&taskId=task_shared",
                "task_shared");
        verify(notificationService).createForTask(
                "bob",
                "TASK_SUCCEEDED",
                "人群计算完成",
                "VIP 人群 · 12 人",
                "/audiences?highlight=7&taskId=task_shared",
                "task_shared");
    }

    @Test
    void runNow_retriesLockConflictWithoutMarkingTaskFailed() {
        when(computeService.compute(7L))
                .thenReturn(AudienceComputeResult.inProgress(7L, "VIP 人群", "已有计算任务正在运行"))
                .thenReturn(AudienceComputeResult.ready(7L, "VIP 人群", 12L, 2));
        when(asyncTaskService.subscribers("task_retry")).thenReturn(List.of("operator"));
        AudienceComputeTaskRunner runner = new AudienceComputeTaskRunner(
                computeService, asyncTaskService, notificationService, Duration.ZERO);

        runner.runNow("task_retry", 7L, "VIP 人群", "operator");

        verify(asyncTaskService).markRunning("task_retry");
        verify(asyncTaskService).markSucceeded("task_retry", "{\"audienceId\":7,\"estimatedSize\":12,\"bitmapSizeKb\":2}");
        verify(asyncTaskService, never()).markFailed("task_retry", "已有计算任务正在运行");
        verify(notificationService, never()).createForTask(
                "operator",
                "TASK_FAILED",
                "人群计算失败",
                "VIP 人群 · 已有计算任务正在运行",
                "/audiences?highlight=7&taskId=task_retry",
                "task_retry");
    }

    @Test
    void runNow_usesInjectedRetryWaiterBetweenLockConflicts() {
        AtomicInteger waitCalls = new AtomicInteger();
        when(computeService.compute(7L))
                .thenReturn(AudienceComputeResult.inProgress(7L, "VIP 人群", "已有计算任务正在运行"))
                .thenReturn(AudienceComputeResult.ready(7L, "VIP 人群", 12L, 2));
        when(asyncTaskService.subscribers("task_waiter")).thenReturn(List.of("operator"));
        AudienceComputeTaskRunner runner = new AudienceComputeTaskRunner(
                computeService,
                asyncTaskService,
                notificationService,
                Duration.ofMillis(50),
                delay -> waitCalls.incrementAndGet());

        runner.runNow("task_waiter", 7L, "VIP 人群", "operator");

        assertThat(waitCalls).hasValue(1);
        verify(asyncTaskService).markSucceeded("task_waiter", "{\"audienceId\":7,\"estimatedSize\":12,\"bitmapSizeKb\":2}");
    }
}
