package org.chovy.canvas.execution.application;

import java.time.Instant;
import java.util.Map;

import org.chovy.canvas.execution.api.trace.ExecutionTraceView;
import org.springframework.stereotype.Service;

/**
 * 定义 ExecutionTraceService 的执行上下文数据结构或业务契约。
 */
@Service
public class ExecutionTraceService {

    /**
     * 保存 repository 对应的状态或配置。
     */
    private final ExecutionTraceRepository repository;

    /**
     * 执行 ExecutionTraceService 对应的业务处理。
     * @param repository repository 参数
     */
    public ExecutionTraceService(ExecutionTraceRepository repository) {
        this.repository = repository;
    }

    /**
     * 执行 start 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param executionId executionId 参数
     * @param canvasId canvasId 参数
     * @param versionId versionId 参数
     */
    public void start(Long tenantId, String executionId, Long canvasId, Long versionId) {
        repository.saveStarted(new ExecutionTraceRecord(
                tenantId,
                executionId,
                canvasId,
                versionId,
                "RUNNING",
                Instant.now(),
                null,
                ""));
    }

    public void recordNode(
            Long tenantId,
            String executionId,
            String nodeId,
            String nodeType,
            String status,
            String error,
            Map<String, Object> outputData) {
        repository.appendNode(new ExecutionNodeTraceRecord(
                tenantId,
                executionId,
                nodeId,
                nodeType,
                status,
                error,
                outputData,
                Instant.now()));
    }

    /**
     * 执行 finish 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param executionId executionId 参数
     * @param status status 参数
     * @param failureReason failureReason 参数
     */
    public void finish(Long tenantId, String executionId, String status, String failureReason) {
        repository.markFinished(tenantId, executionId, status, failureReason, Instant.now());
    }

    public void recordResume(
            Long tenantId,
            Long canvasId,
            Long versionId,
            String executionId,
            String nodeId,
            String status,
            Map<String, Object> outputData) {
        recordNode(tenantId, executionId, nodeId, "USER_INPUT", "SUCCESS", "", outputData);
        finish(tenantId, executionId, status, "");
    }

    /**
     * 执行 trace 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param executionId executionId 参数
     * @return 处理后的结果
     */
    public ExecutionTraceView trace(Long tenantId, String executionId) {
        return repository.get(tenantId, executionId);
    }
}
