package org.chovy.canvas.domain.task;

/**
 * 异步任务状态枚举。
 *
 * <p>描述后台任务从排队、运行到成功、失败或取消的生命周期状态，是任务查询和通知展示的统一口径。
 * <p>新增状态时需要同步检查任务服务状态流转、前端展示文案和订阅通知逻辑。
 */
public enum AsyncTaskStatus {
    /** 任务已入队，等待后台执行。 */
    QUEUED,
    /** 任务正在执行。 */
    RUNNING,
    /** 任务已成功完成。 */
    SUCCEEDED,
    /** 任务执行失败。 */
    FAILED,
    /** 任务在完成前被取消。 */
    CANCELED
}
