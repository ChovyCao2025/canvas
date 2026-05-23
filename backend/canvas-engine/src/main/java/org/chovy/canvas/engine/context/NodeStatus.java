package org.chovy.canvas.engine.context;

public enum NodeStatus {
    PENDING,
    RUNNING,
    /** LOGIC_RELATION / HUB 条件未满足，等待更多上游触发（多阶段执行挂起状态） */
    WAITING,
    SUCCESS,
    FAILED,
    TIMEOUT,
    SUPPRESSED,
    SKIPPED,
    PARTIAL_FAIL   // PRIORITY 所有分支失败但有 nextNodeId
}
