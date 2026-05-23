package org.chovy.canvas.engine.audience;

import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.domain.notification.NotificationService;
import org.chovy.canvas.domain.task.AsyncTaskService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;

@Slf4j
@Service
public class AudienceComputeTaskRunner {

    private static final int ERROR_LIMIT = 1000;
    private static final Duration DEFAULT_LOCK_RETRY_DELAY = Duration.ofSeconds(3);

    private final AudienceBatchComputeService computeService;
    private final AsyncTaskService asyncTaskService;
    private final NotificationService notificationService;
    private final Duration lockRetryDelay;

    public AudienceComputeTaskRunner(
            AudienceBatchComputeService computeService,
            AsyncTaskService asyncTaskService,
            NotificationService notificationService
    ) {
        this(computeService, asyncTaskService, notificationService, DEFAULT_LOCK_RETRY_DELAY);
    }

    AudienceComputeTaskRunner(
            AudienceBatchComputeService computeService,
            AsyncTaskService asyncTaskService,
            NotificationService notificationService,
            Duration lockRetryDelay
    ) {
        this.computeService = computeService;
        this.asyncTaskService = asyncTaskService;
        this.notificationService = notificationService;
        this.lockRetryDelay = lockRetryDelay;
    }

    public void start(String taskId, Long audienceId, String audienceName, String operator) {
        Thread.ofVirtual().start(() -> runNow(taskId, audienceId, audienceName, operator));
    }

    public void runNow(String taskId, Long audienceId, String audienceName, String operator) {
        asyncTaskService.markRunning(taskId);
        AudienceComputeResult result;
        try {
            result = computeWithLockRetry(audienceId, taskId);
        } catch (Exception e) {
            String error = errorMessage(e);
            log.error("[AUDIENCE] compute task failed taskId={} audienceId={}: {}", taskId, audienceId, error, e);
            markFailedBestEffort(taskId, error);
            createNotificationsBestEffort(
                    taskId,
                    operator,
                    "TASK_FAILED",
                    "人群计算失败",
                    displayName(null, audienceName, audienceId) + " · " + error,
                    targetUrl(audienceId, taskId));
            return;
        }
        if (result.success()) {
            asyncTaskService.markSucceeded(taskId, successSummary(result));
            createNotificationsBestEffort(
                    taskId,
                    operator,
                    "TASK_SUCCEEDED",
                    "人群计算完成",
                    displayName(result, audienceName, audienceId) + " · " + result.estimatedSize() + " 人",
                    targetUrl(audienceId, taskId));
            return;
        }
        String error = result.errorMsg() == null ? "计算失败" : result.errorMsg();
        asyncTaskService.markFailed(taskId, error);
        createNotificationsBestEffort(
                taskId,
                operator,
                "TASK_FAILED",
                "人群计算失败",
                displayName(result, audienceName, audienceId) + " · " + error,
                targetUrl(audienceId, taskId));
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

    private void markFailedBestEffort(String taskId, String error) {
        try {
            asyncTaskService.markFailed(taskId, error);
        } catch (Exception markException) {
            log.error("[AUDIENCE] failed to mark compute task failed taskId={}: {}",
                    taskId, markException.getMessage(), markException);
        }
    }

    private AudienceComputeResult computeWithLockRetry(Long audienceId, String taskId) throws InterruptedException {
        while (true) {
            AudienceComputeResult result = computeService.compute(audienceId);
            if (!result.inProgress()) {
                return result;
            }
            log.info("[AUDIENCE] compute task waiting for active lock taskId={} audienceId={}: {}",
                    taskId, audienceId, result.errorMsg());
            sleepBeforeLockRetry();
        }
    }

    private void sleepBeforeLockRetry() throws InterruptedException {
        if (lockRetryDelay.isZero() || lockRetryDelay.isNegative()) {
            return;
        }
        Thread.sleep(lockRetryDelay.toMillis());
    }

    private void createNotificationBestEffort(
            String operator, String type, String title, String content, String targetUrl, String taskId) {
        try {
            notificationService.createForTask(operator, type, title, content, targetUrl, taskId);
        } catch (Exception notificationException) {
            log.error("[AUDIENCE] failed to create compute task notification taskId={}: {}",
                    taskId, notificationException.getMessage(), notificationException);
        }
    }

    private void createNotificationsBestEffort(
            String taskId, String fallbackOperator, String type, String title, String content, String targetUrl) {
        for (String recipient : notificationRecipients(taskId, fallbackOperator)) {
            createNotificationBestEffort(recipient, type, title, content, targetUrl, taskId);
        }
    }

    private List<String> notificationRecipients(String taskId, String fallbackOperator) {
        LinkedHashSet<String> recipients = new LinkedHashSet<>();
        try {
            List<String> subscribers = asyncTaskService.subscribers(taskId);
            if (subscribers != null) {
                subscribers.stream()
                        .filter(this::hasText)
                        .forEach(recipients::add);
            }
        } catch (Exception e) {
            log.error("[AUDIENCE] failed to load compute task subscribers taskId={}: {}", taskId, e.getMessage(), e);
        }
        if (recipients.isEmpty() && hasText(fallbackOperator)) {
            recipients.add(fallbackOperator);
        }
        return List.copyOf(recipients);
    }

    private String displayName(AudienceComputeResult result, String audienceName, Long audienceId) {
        if (result != null && hasText(result.audienceName())) {
            return result.audienceName();
        }
        if (hasText(audienceName)) {
            return audienceName;
        }
        return "人群 " + audienceId;
    }

    private String errorMessage(Exception e) {
        String message = e.getMessage();
        if (!hasText(message)) {
            message = "计算失败";
        }
        return message.length() <= ERROR_LIMIT ? message : message.substring(0, ERROR_LIMIT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
