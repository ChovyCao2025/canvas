package org.chovy.canvas.engine.handlers;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DelayHandlerTest {

    @Test
    void applyJitterLeavesDelayUnchangedWhenDisabled() {
        assertThat(DelayHandler.applyJitter(1_000L, 0L)).isEqualTo(1_000L);
        assertThat(DelayHandler.applyJitter(1_000L, -1L)).isEqualTo(1_000L);
    }

    @Test
    void applyJitterAddsBoundedRandomDelay() {
        for (int i = 0; i < 100; i++) {
            long result = DelayHandler.applyJitter(1_000L, 300L);
            assertThat(result).isBetween(1_000L, 1_299L);
        }
    }
}
