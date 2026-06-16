package org.chovy.canvas.execution.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.chovy.canvas.execution.api.trace.ExecutionTraceView;

/**
 * 定义 InMemoryExecutionTraceRepository 的执行上下文数据结构或业务契约。
 */
public class InMemoryExecutionTraceRepository implements ExecutionTraceRepository {

    private final Map<Key, MutableTrace> traces = new ConcurrentHashMap<>();

    /**
     * 执行 saveStarted 对应的业务处理。
     * @param trace trace 参数
     */
    @Override
    public void saveStarted(ExecutionTraceRecord trace) {
        traces.put(new Key(trace.tenantId(), trace.executionId()), new MutableTrace(
                trace.tenantId(),
                trace.executionId(),
                trace.canvasId(),
                trace.status(),
                trace.startedAt(),
                trace.finishedAt(),
                trace.failureReason()));
    }

    /**
     * 执行 appendNode 对应的业务处理。
     * @param nodeTrace nodeTrace 参数
     */
    @Override
    public void appendNode(ExecutionNodeTraceRecord nodeTrace) {
        MutableTrace trace = trace(nodeTrace.tenantId(), nodeTrace.executionId());
        trace.nodeResults.add(new ExecutionTraceView.NodeResultView(
                nodeTrace.nodeId(),
                nodeTrace.nodeType(),
                nodeTrace.status(),
                nodeTrace.error(),
                nodeTrace.outputData()));
    }

    /**
     * 执行 markFinished 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param executionId executionId 参数
     * @param status status 参数
     * @param failureReason failureReason 参数
     * @param finishedAt finishedAt 参数
     */
    @Override
    public void markFinished(Long tenantId, String executionId, String status, String failureReason, Instant finishedAt) {
        MutableTrace trace = trace(tenantId, executionId);
        trace.status = status == null || status.isBlank() ? "UNKNOWN" : status;
        trace.failureReason = failureReason == null ? "" : failureReason;
        trace.finishedAt = finishedAt == null ? Instant.now() : finishedAt;
    }

    /**
     * 执行 get 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param executionId executionId 参数
     * @return 处理后的结果
     */
    @Override
    public ExecutionTraceView get(Long tenantId, String executionId) {
        MutableTrace trace = trace(tenantId, executionId);
        return new ExecutionTraceView(
                trace.tenantId,
                trace.executionId,
                trace.canvasId,
                trace.status,
                trace.startedAt,
                trace.finishedAt,
                List.copyOf(trace.nodeResults),
                trace.failureReason);
    }

    /**
     * 执行 trace 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param executionId executionId 参数
     * @return 处理后的结果
     */
    private MutableTrace trace(Long tenantId, String executionId) {
        MutableTrace trace = traces.get(new Key(tenantId, executionId));
        if (trace == null) {
            throw new IllegalStateException("execution trace not found: tenantId=" + tenantId
                    + ", executionId=" + executionId);
        }
        return trace;
    }

    /**
     * 定义 Key 的执行上下文数据结构或业务契约。
     * @param tenantId tenantId 对应的数据字段
     * @param executionId executionId 对应的数据字段
     */
    private record Key(Long tenantId, String executionId) {
    }

    /**
     * 定义 MutableTrace 的执行上下文数据结构或业务契约。
     */
    private static final class MutableTrace {
        /**
         * 保存 tenantId 对应的状态或配置。
         */
        private final Long tenantId;

        /**
         * 保存 executionId 对应的状态或配置。
         */
        private final String executionId;

        /**
         * 保存 canvasId 对应的状态或配置。
         */
        private final Long canvasId;

        /**
         * 保存 startedAt 对应的状态或配置。
         */
        private final Instant startedAt;
        private final List<ExecutionTraceView.NodeResultView> nodeResults = new ArrayList<>();

        /**
         * 保存 status 对应的状态或配置。
         */
        private String status;

        /**
         * 保存 finishedAt 对应的状态或配置。
         */
        private Instant finishedAt;

        /**
         * 保存 failureReason 对应的状态或配置。
         */
        private String failureReason;

        private MutableTrace(
                Long tenantId,
                String executionId,
                Long canvasId,
                String status,
                Instant startedAt,
                Instant finishedAt,
                String failureReason) {
            this.tenantId = tenantId;
            this.executionId = executionId;
            this.canvasId = canvasId;
            this.status = status;
            this.startedAt = startedAt;
            this.finishedAt = finishedAt;
            this.failureReason = failureReason;
        }
    }
}
