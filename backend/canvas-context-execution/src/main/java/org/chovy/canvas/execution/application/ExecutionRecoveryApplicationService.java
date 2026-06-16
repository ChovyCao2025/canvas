package org.chovy.canvas.execution.application;

import java.util.LinkedHashMap;
import java.util.Map;

import org.chovy.canvas.canvas.application.UserInputResumePort;
import org.chovy.canvas.canvas.application.UserInputResumeRequest;
import org.springframework.stereotype.Service;

@Service
public class ExecutionRecoveryApplicationService implements UserInputResumePort {

    private final ExecutionTraceService traceService;

    public ExecutionRecoveryApplicationService(ExecutionTraceService traceService) {
        this.traceService = traceService;
    }

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
