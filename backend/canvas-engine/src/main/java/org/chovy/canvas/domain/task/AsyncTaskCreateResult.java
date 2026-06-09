package org.chovy.canvas.domain.task;

import org.chovy.canvas.dal.dataobject.AsyncTaskDO;

/**
 * 异步任务 Create 数据传输对象。
 *
 * <p>用于在控制器、服务、异步任务或实时推送之间传递结构化数据，隔离外部 API 契约与数据库实体。
 * <p>该类型应保持轻量，只表达字段语义和序列化边界，不放入复杂业务流程。
 * @param task 创建或复用的异步任务实体.
 * @param created true 表示本次新建任务，false 表示复用已有运行中任务.
 */
public record AsyncTaskCreateResult(
        AsyncTaskDO task,
        boolean created
) {
}
