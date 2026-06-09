package org.chovy.canvas.engine.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDlqDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.infrastructure.concurrent.ManagedVirtualThreadExecutor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.RejectedExecutionException;

/**
 * Persists node execution failures to the replayable execution DLQ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutionDlqWriter {

    private final CanvasExecutionDlqMapper dlqMapper;
    private final CanvasMetrics metrics;
    private final ObjectMapper objectMapper;
    private final ManagedVirtualThreadExecutor backgroundExecutor;

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param ctx ctx 参数，用于 write 流程中的校验、计算或对象转换。
     * @param nodeId 业务对象 ID，用于定位具体记录。
     * @param nodeType 类型标识，用于选择对应处理分支。
     * @param cause cause 参数，用于 write 流程中的校验、计算或对象转换。
     * @param retryCount retry count 参数，用于 write 流程中的校验、计算或对象转换。
     */
    void write(ExecutionContext ctx, String nodeId, String nodeType, Throwable cause, int retryCount) {
        // 准备本次处理所需的上下文和中间变量。
        metrics.recordDlq(nodeType);
        try {
            String msg = cause.getMessage() != null ? cause.getMessage() : "unknown";
            CanvasExecutionDlqDO dlq = CanvasExecutionDlqDO.builder()
                    .executionId(ctx.getExecutionId())
                    .canvasId(ctx.getCanvasId())
                    .userId(ctx.getUserId())
                    .perfRunId(ctx.getPerfRunId())
                    .failedNodeId(nodeId)
                    .failedNodeType(nodeType)
                    .errorMsg(msg.substring(0, Math.min(500, msg.length())))
                    .retryCount(retryCount)
                    // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                    .triggerPayload(objectMapper.writeValueAsString(ctx.getTriggerPayload()))
                    .triggerType(ctx.getTriggerType())
                    .triggerNodeType(ctx.getTriggerNodeType())
                    .matchKey(ctx.getMatchKey())
                    .failedAt(LocalDateTime.now())
                    .build();
            try {
                backgroundExecutor.submit("canvas-dlq-write", () -> {
                    try {
                        dlqMapper.insert(dlq);
                    } catch (Exception e) {
                        log.error("[DLQ] 写入失败: {}", e.getMessage());
                    }
                });
            } catch (RejectedExecutionException e) {
                log.error("[DLQ] 写入被拒绝 executionId={} nodeId={}: {}",
                        ctx.getExecutionId(), nodeId, e.getMessage());
            }
            log.warn("[DLQ] executionId={} nodeId={} reason={}", ctx.getExecutionId(), nodeId, msg);
        } catch (Exception e) {
            log.error("[DLQ] 序列化失败: {}", e.getMessage());
        }
    }
}
