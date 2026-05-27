package org.chovy.canvas.domain.task;

/**
 * 异步任务状态枚举。
 *
 * <p>描述后台任务从排队、运行到成功、失败或取消的生命周期状态，是任务查询和通知展示的统一口径。
 * <p>新增状态时需要同步检查任务服务状态流转、前端展示文案和订阅通知逻辑。
 */
public enum AsyncTaskStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELED
}
