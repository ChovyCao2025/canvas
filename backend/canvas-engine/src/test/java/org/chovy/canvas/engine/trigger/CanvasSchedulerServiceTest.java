package org.chovy.canvas.engine.trigger;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class CanvasSchedulerServiceTest {

    @Test
    void calcJitterReturnsZeroWhenDisabled() {
        assertThat(CanvasSchedulerService.calcJitter(0)).isEqualTo(Duration.ZERO);
        assertThat(CanvasSchedulerService.calcJitter(-1)).isEqualTo(Duration.ZERO);
    }

    @Test
    void calcJitterReturnsValueWithinExclusiveUpperBound() {
        for (int i = 0; i < 100; i++) {
            long millis = CanvasSchedulerService.calcJitter(60_000).toMillis();
            assertThat(millis).isBetween(0L, 59_999L);
        }
    }
}
