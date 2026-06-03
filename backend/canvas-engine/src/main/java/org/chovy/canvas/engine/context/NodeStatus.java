package org.chovy.canvas.engine.context;

/**
 * 节点执行状态枚举（用于 ExecutionContext.nodeStatuses）。
 */
public enum NodeStatus {
    /** 尚未开始执行。 */
    PENDING,

    /** 执行中。 */
    RUNNING,

    /** 汇聚条件未满足，等待更多上游触发（多阶段执行挂起状态） */
    WAITING,

    /** 执行成功。 */
    SUCCESS,

    /** 执行失败。 */
    FAILED,

    /** 等待或汇聚节点超时。 */
    TIMEOUT,

    /** 被营销授权、抑制名单或策略保护拦截。 */
    SUPPRESSED,

    /** 未被当前执行路径选中。 */
    SKIPPED
}
