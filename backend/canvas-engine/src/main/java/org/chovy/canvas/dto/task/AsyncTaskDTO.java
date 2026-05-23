package org.chovy.canvas.dto.task;

import org.chovy.canvas.domain.task.AsyncTask;

import java.time.LocalDateTime;

public record AsyncTaskDTO(
        Long id,
        String taskId,
        String taskType,
        String bizType,
        String bizId,
        String title,
        String status,
        Integer progress,
        String resultSummary,
        String errorMsg,
        String createdBy,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AsyncTaskDTO from(AsyncTask task) {
        return new AsyncTaskDTO(
                task.getId(),
                task.getTaskId(),
                task.getTaskType(),
                task.getBizType(),
                task.getBizId(),
                task.getTitle(),
                task.getStatus(),
                task.getProgress(),
                task.getResultSummary(),
                task.getErrorMsg(),
                task.getCreatedBy(),
                task.getStartedAt(),
                task.getFinishedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
