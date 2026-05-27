package org.chovy.canvas.engine.handler;

/**
 * 节点执行结果归类枚举。
 *
 * <p>用于把不同 NodeResult 归一为成功、失败、等待、跳过等调度语义，帮助 DAG 引擎决定后续路由和状态落库。
 * <p>枚举值需要保持稳定，因为执行跟踪、告警和测试都会依赖这些结果分类。
 */
public enum NodeOutcome {
    SUCCESS,
    FAIL,
    TIMEOUT,
    SUPPRESSED,
    SKIPPED,
    PENDING
}
