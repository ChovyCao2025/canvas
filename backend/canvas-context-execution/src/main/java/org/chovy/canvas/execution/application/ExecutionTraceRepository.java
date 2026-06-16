package org.chovy.canvas.execution.application;

import java.time.Instant;

import org.chovy.canvas.execution.api.trace.ExecutionTraceView;

/**
 * 定义 ExecutionTraceRepository 的执行上下文数据结构或业务契约。
 */
public interface ExecutionTraceRepository {

    /**
     * 执行 saveStarted 对应的业务处理。
     * @param trace trace 参数
     */
    void saveStarted(ExecutionTraceRecord trace);

    /**
     * 执行 appendNode 对应的业务处理。
     * @param nodeTrace nodeTrace 参数
     */
    void appendNode(ExecutionNodeTraceRecord nodeTrace);

    /**
     * 执行 markFinished 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param executionId executionId 参数
     * @param status status 参数
     * @param failureReason failureReason 参数
     * @param finishedAt finishedAt 参数
     */
    void markFinished(Long tenantId, String executionId, String status, String failureReason, Instant finishedAt);

    /**
     * 执行 get 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param executionId executionId 参数
     * @return 处理后的结果
     */
    ExecutionTraceView get(Long tenantId, String executionId);
}
