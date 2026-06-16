package org.chovy.canvas.execution.api.dryrun;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 定义 ExecutionDryRunContractTest 的执行上下文数据结构或业务契约。
 */
class ExecutionDryRunContractTest {

    /**
     * 执行 dryRunReturnsTraceWithoutPublishingOrMutatingCanvas 对应的业务处理。
     */
    @Test
    void dryRunReturnsTraceWithoutPublishingOrMutatingCanvas() {
        RecordingDryRunFacade facade = new RecordingDryRunFacade();
        ExecutionDryRunFacade.DryRunCommand command = new ExecutionDryRunFacade.DryRunCommand(
                3L,
                4L,
                5L,
                "{\"user\":{\"id\":\"u1\"}}",
                true);

        ExecutionDryRunFacade.DryRunResultView result = facade.dryRun(command);

        assertThat(result.executionId()).isEqualTo("dry-run-1");
        assertThat(result.published()).isFalse();
        assertThat(result.trace()).containsEntry("mode", "mock");
        assertThat(facade.publishCalls).isZero();
    }

    /**
     * 定义 RecordingDryRunFacade 的执行上下文数据结构或业务契约。
     */
    private static final class RecordingDryRunFacade implements ExecutionDryRunFacade {
        /**
         * 保存 publishCalls 对应的状态或配置。
         */
        private int publishCalls;

        /**
         * 执行 dryRun 对应的业务处理。
         * @param command command 参数
         */
        @Override
        public DryRunResultView dryRun(DryRunCommand command) {
            return new DryRunResultView(
                    "dry-run-1",
                    false,
                    Map.of("mode", command.mockMode() ? "mock" : "live"),
                    List.of("start"));
        }
    }
}
