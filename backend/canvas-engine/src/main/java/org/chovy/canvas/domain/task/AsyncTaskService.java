package org.chovy.canvas.domain.task;

public interface AsyncTaskService {

    void markRunning(String taskId);

    void markSucceeded(String taskId, String resultJson);

    void markFailed(String taskId, String errorMessage);
}
