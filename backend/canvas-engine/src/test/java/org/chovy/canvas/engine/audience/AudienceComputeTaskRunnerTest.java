package org.chovy.canvas.engine.audience;

import org.chovy.canvas.domain.notification.NotificationService;
import org.chovy.canvas.domain.task.AsyncTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
}
