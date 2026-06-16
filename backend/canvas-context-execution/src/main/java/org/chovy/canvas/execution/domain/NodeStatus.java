package org.chovy.canvas.execution.domain;

/**
 * 定义 NodeStatus 的执行上下文数据结构或业务契约。
 */
public enum NodeStatus {
    /**
     * 表示 PENDING 枚举值。
     */
    PENDING,
    /**
     * 表示 RUNNING 枚举值。
     */
    RUNNING,
    /**
     * 表示 WAITING 枚举值。
     */
    WAITING,
    /**
     * 表示 SUCCESS 枚举值。
     */
    SUCCESS,
    /**
     * 表示 FAILED 枚举值。
     */
    FAILED,
    /**
     * 表示 TIMEOUT 枚举值。
     */
    TIMEOUT,
    /**
     * 表示 SUPPRESSED 枚举值。
     */
    SUPPRESSED,
    SKIPPED
}
