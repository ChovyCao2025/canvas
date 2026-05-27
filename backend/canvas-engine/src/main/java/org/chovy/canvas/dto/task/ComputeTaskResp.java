package org.chovy.canvas.dto.task;

/**
 * Compute Task 数据传输对象。
 *
 * <p>用于在控制器、服务、异步任务或实时推送之间传递结构化数据，隔离外部 API 契约与数据库实体。
 * <p>该类型应保持轻量，只表达字段语义和序列化边界，不放入复杂业务流程。
 */
public record ComputeTaskResp(
        /** 异步计算任务 ID，用于前端轮询任务状态。 */
        String taskId,
        /** 任务入队后的初始状态或当前状态。 */
        String status
) {
}
