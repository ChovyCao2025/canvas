package org.chovy.canvas.execution.api;

import java.util.Map;

import org.chovy.canvas.execution.api.trace.ExecutionTraceView;

/**
 * 定义 CanvasExecutionFacade 的执行上下文数据结构或业务契约。
 */
public interface CanvasExecutionFacade {

    /**
     * 执行 trigger 对应的业务处理。
     * @param command command 参数
     * @return 处理后的结果
     */
    ExecutionResultView trigger(ExecutionRequestCommand command);

    /**
     * 执行 trace 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param executionId executionId 参数
     * @return 处理后的结果
     */
    ExecutionTraceView trace(Long tenantId, String executionId);

    /**
     * 定义 ExecutionRequestCommand 的执行上下文数据结构或业务契约。
     * @param tenantId tenantId 对应的数据字段
     * @param canvasId canvasId 对应的数据字段
     * @param versionId versionId 对应的数据字段
     * @param triggerType triggerType 对应的数据字段
     * @param userId userId 对应的数据字段
     * @param payload payload 对应的数据字段
     * @param dryRun dryRun 对应的数据字段
     */
    record ExecutionRequestCommand(
            Long tenantId,
            Long canvasId,
            Long versionId,
            String triggerType,
            String userId,
            Map<String, Object> payload,
            boolean dryRun) {

        public ExecutionRequestCommand {
            if (tenantId == null || tenantId <= 0) {
                throw new IllegalArgumentException("tenantId is required");
            }
            if (canvasId == null || canvasId <= 0) {
                throw new IllegalArgumentException("canvasId is required");
            }
            triggerType = triggerType == null || triggerType.isBlank() ? "MANUAL" : triggerType;
            userId = userId == null ? "" : userId;
            payload = Map.copyOf(payload == null ? Map.of() : payload);
        }
    }

    /**
     * 定义 ExecutionResultView 的执行上下文数据结构或业务契约。
     * @param executionId executionId 对应的数据字段
     * @param status status 对应的数据字段
     */
    record ExecutionResultView(String executionId, String status) {
        public ExecutionResultView {
            if (executionId == null || executionId.isBlank()) {
                throw new IllegalArgumentException("executionId is required");
            }
            status = status == null || status.isBlank() ? "STARTED" : status;
        }
    }
}
