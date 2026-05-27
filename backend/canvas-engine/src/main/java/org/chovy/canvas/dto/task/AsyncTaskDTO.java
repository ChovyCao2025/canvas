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
        /** 对外暴露的任务 ID，用于前端轮询和通知关联。 */
        String taskId,
        /** 任务类型，如 AUDIENCE_COMPUTE、TAG_IMPORT 等。 */
        String taskType,
        /** 业务类型，用于区分任务归属的业务域。 */
        String bizType,
        /** 业务对象 ID，如人群 ID、导入批次 ID 等。 */
        String bizId,
        /** 任务展示标题。 */
        String title,
        /** 任务状态，见 {@link org.chovy.canvas.domain.task.AsyncTaskStatus}。 */
        String status,
        /** 任务进度百分比，取值范围 0~100。 */
        Integer progress,
        /** 任务结果摘要，成功或部分成功时用于前端展示。 */
        String resultSummary,
        /** 任务失败原因或异常摘要。 */
        String errorMsg,
        /** 任务开始执行时间。 */
        LocalDateTime startedAt,
        /** 任务结束时间，成功、失败或取消时写入。 */
        LocalDateTime finishedAt,
        /** 任务创建时间。 */
        LocalDateTime createdAt,
        /** 任务最后更新时间。 */
        LocalDateTime updatedAt
) {
    /**
     * 执行 from 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param task task 方法执行所需的业务参数
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
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
