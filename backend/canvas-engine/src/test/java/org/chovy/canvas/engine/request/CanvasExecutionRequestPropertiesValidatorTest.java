package org.chovy.canvas.engine.request;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CanvasExecutionRequestPropertiesValidatorTest {

    @Test
    void validateAcceptsSafeExecutionRequestSettings() {
        CanvasExecutionRequestPropertiesValidator validator = validator();

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void validateRejectsHeartbeatIntervalThatCanOutliveRunningLease() {
        CanvasExecutionRequestPropertiesValidator validator = new CanvasExecutionRequestPropertiesValidator(
                200, 1_000L, 5_000L, 5, 60L, 60_000L, 60_000L, 0);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("heartbeat-interval-ms")
                .hasMessageContaining("running-stale-sec");
    }

    @Test
    void validateRejectsMaxRetryDelayBelowBaseRetryDelay() {
        CanvasExecutionRequestPropertiesValidator validator = new CanvasExecutionRequestPropertiesValidator(
                200, 1_000L, 30_000L, 5, 300L, 5_000L, 60_000L, 0);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("max-retry-delay-ms")
                .hasMessageContaining("retry-delay-ms");
    }

    @Test
    void validateRejectsNegativePerCanvasBatchLimit() {
        CanvasExecutionRequestPropertiesValidator validator = new CanvasExecutionRequestPropertiesValidator(
                200, 1_000L, 5_000L, 5, 300L, 60_000L, 60_000L, -1);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("per-canvas-batch-limit");
    }

    private CanvasExecutionRequestPropertiesValidator validator() {
        return new CanvasExecutionRequestPropertiesValidator(
                200, 1_000L, 5_000L, 5, 300L, 60_000L, 60_000L, 0);
    }
}
