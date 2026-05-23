package org.chovy.canvas.engine.audience;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.domain.notification.NotificationService;
import org.chovy.canvas.domain.task.AsyncTaskService;

@RequiredArgsConstructor
public class AudienceComputeTaskRunner {

    private final AudienceBatchComputeService computeService;
    private final AsyncTaskService asyncTaskService;
    private final NotificationService notificationService;

    public void runNow(String taskId, Long audienceId, String audienceName, String operator) {
        asyncTaskService.markRunning(taskId);
        AudienceComputeResult result = computeService.compute(audienceId);
        if (result.ready()) {
            asyncTaskService.markSucceeded(taskId, successPayload(result));
            notificationService.createForTask(
                    operator,
                    "TASK_SUCCEEDED",
                    "人群计算完成",
                    audienceName + " · " + result.estimatedSize() + " 人",
                    "/audiences?highlight=" + audienceId + "&taskId=" + taskId,
                    taskId
            );
            return;
        }

        asyncTaskService.markFailed(taskId, result.errorMsg());
        notificationService.createForTask(
                operator,
                "TASK_FAILED",
                "人群计算失败",
                audienceName + " · " + result.errorMsg(),
                "/audiences?highlight=" + audienceId + "&taskId=" + taskId,
                taskId
        );
    }

    private String successPayload(AudienceComputeResult result) {
        return "{\"audienceId\":" + result.audienceId()
                + ",\"estimatedSize\":" + result.estimatedSize()
                + ",\"bitmapSizeKb\":" + result.bitmapSizeKb()
                + "}";
    }
}
