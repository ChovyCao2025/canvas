package org.chovy.canvas.dto.task;

import org.chovy.canvas.dal.dataobject.AsyncTaskDO;

import java.time.LocalDateTime;

/**
 * 异步任务 数据传输对象。
 *
 * <p>用于在控制器、服务、异步任务或实时推送之间传递结构化数据，隔离外部 API 契约与数据库实体。
 * <p>该类型应保持轻量，只表达字段语义和序列化边界，不放入复杂业务流程。
 */
public record AsyncTaskDTO(
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
        LocalDateTime updatedAt
) {
    public static AsyncTaskDTO from(AsyncTaskDO task) {
        return new AsyncTaskDTO(
                task.getTaskId(),
                task.getTaskType(),
                task.getBizType(),
                task.getBizId(),
                task.getTitle(),
                task.getStatus(),
                task.getProgress(),
                task.getResultSummary(),
                task.getErrorMsg(),
                task.getStartedAt(),
                task.getFinishedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
