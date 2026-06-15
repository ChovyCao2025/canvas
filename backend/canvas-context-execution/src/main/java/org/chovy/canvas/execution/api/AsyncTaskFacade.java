package org.chovy.canvas.execution.api;

import java.time.LocalDateTime;
import java.util.List;

public interface AsyncTaskFacade {

    List<AsyncTaskView> listTasks(AsyncTaskQuery query);

    AsyncTaskView getTask(String taskId, String username, boolean admin);

    record AsyncTaskQuery(
            String taskType,
            String bizType,
            List<String> bizIds,
            List<String> statuses,
            String username,
            boolean admin,
            int page,
            int size) {
    }

    record AsyncTaskView(
            String taskId,
            String taskType,
            String bizType,
            String bizId,
            String title,
            String status,
            Integer progress,
            String resultSummary,
            String errorMsg,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String createdBy) {
    }
}
