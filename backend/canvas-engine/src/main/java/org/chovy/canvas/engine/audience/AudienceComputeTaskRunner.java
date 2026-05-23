package org.chovy.canvas.engine.audience;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.domain.notification.NotificationService;
import org.chovy.canvas.domain.task.AsyncTaskService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AudienceComputeTaskRunner {

    private final AudienceBatchComputeService computeService;
    private final AsyncTaskService asyncTaskService;
    private final NotificationService notificationService;

    public void start(String taskId, Long audienceId, String audienceName, String operator) {
        Thread.ofVirtual().start(() -> runNow(taskId, audienceId, audienceName, operator));
    }

    public void runNow(String taskId, Long audienceId, String audienceName, String operator) {
        asyncTaskService.markRunning(taskId);
        AudienceComputeResult result = computeService.compute(audienceId);
        if (result.success()) {
            asyncTaskService.markSucceeded(taskId, successSummary(result));
            notificationService.createForTask(
                    operator,
                    "TASK_SUCCEEDED",
                    "人群计算完成",
                    result.audienceName() + " · " + result.estimatedSize() + " 人",
                    targetUrl(audienceId, taskId),
                    taskId);
            return;
        }
        String error = result.errorMsg() == null ? "计算失败" : result.errorMsg();
        asyncTaskService.markFailed(taskId, error);
        notificationService.createForTask(
                operator,
                "TASK_FAILED",
                "人群计算失败",
                audienceName + " · " + error,
                targetUrl(audienceId, taskId),
                taskId);
    }

    private String successSummary(AudienceComputeResult result) {
        return "{\"audienceId\":" + result.audienceId()
                + ",\"estimatedSize\":" + result.estimatedSize()
                + ",\"bitmapSizeKb\":" + result.bitmapSizeKb()
                + "}";
    }

    private String targetUrl(Long audienceId, String taskId) {
        return "/audiences?highlight=" + audienceId + "&taskId=" + taskId;
    }
}
