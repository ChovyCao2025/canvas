package org.chovy.canvas.execution.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.chovy.canvas.canvas.application.UserInputResumeRequest;
import org.chovy.canvas.execution.api.trace.ExecutionTraceView;
import org.junit.jupiter.api.Test;

/**
 * 定义 ExecutionRecoveryApplicationServiceTest 的执行上下文数据结构或业务契约。
 */
class ExecutionRecoveryApplicationServiceTest {

    /**
     * 执行 userInputResumePortRecordsResumeTrace 对应的业务处理。
     */
    @Test
    void userInputResumePortRecordsResumeTrace() {
        ExecutionTraceService traceService = new ExecutionTraceService(new InMemoryExecutionTraceRepository());
        ExecutionRecoveryApplicationService service = new ExecutionRecoveryApplicationService(traceService);
        traceService.start(3L, "exec-resume-1", 12L, 13L);

        service.requestResume(new UserInputResumeRequest(
                3L,
                12L,
                13L,
                "exec-resume-1",
                "form-node",
                "user-3",
                99L,
                "SUBMITTED",
                Map.of("answer", "yes")));

        ExecutionTraceView trace = traceService.trace(3L, "exec-resume-1");

        assertThat(trace.status()).isEqualTo("RESUMED");
        assertThat(trace.nodeResults()).hasSize(1);
        assertThat(trace.nodeResults().get(0).nodeId()).isEqualTo("form-node");
        assertThat(trace.nodeResults().get(0).outputData())
                .containsEntry("resumeStatus", "SUBMITTED")
                .containsEntry("responseId", 99L)
                .containsEntry("answer", "yes");
    }
}
