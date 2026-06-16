package org.chovy.canvas.execution.application;

import java.util.LinkedHashMap;
import java.util.Map;

import org.chovy.canvas.canvas.application.UserInputResumePort;
import org.chovy.canvas.canvas.application.UserInputResumeRequest;
import org.springframework.stereotype.Service;

/**
 * 定义 ExecutionRecoveryApplicationService 的执行上下文数据结构或业务契约。
 */
@Service
public class ExecutionRecoveryApplicationService implements UserInputResumePort {

    /**
     * 保存 traceService 对应的状态或配置。
     */
    private final ExecutionTraceService traceService;

    /**
     * 执行 ExecutionRecoveryApplicationService 对应的业务处理。
     * @param traceService traceService 参数
     */
    public ExecutionRecoveryApplicationService(ExecutionTraceService traceService) {
        this.traceService = traceService;
    }

    /**
     * 执行 requestResume 对应的业务处理。
     * @param request request 参数
     */
    @Override
    public void requestResume(UserInputResumeRequest request) {
        Map<String, Object> output = new LinkedHashMap<>(request.payload());
        output.put("responseId", request.responseId());
        output.put("resumeStatus", request.resumeStatus());
        traceService.recordResume(
                request.tenantId(),
                request.canvasId(),
                request.versionId(),
                request.executionId(),
                request.nodeId(),
                "RESUMED",
                output);
    }
}
