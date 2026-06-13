package org.chovy.canvas.execution.api.dryrun;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ExecutionDryRunContractTest {

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

    private static final class RecordingDryRunFacade implements ExecutionDryRunFacade {
        private int publishCalls;

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
