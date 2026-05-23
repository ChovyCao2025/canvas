package org.chovy.canvas.perf;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PerfRunContextTest {

    @Test
    void extractsValidPerfRunIdFromPayload() {
        assertThat(PerfRunContext.extract(Map.of("perfRunId", "perf_20260523_001")))
                .isEqualTo("perf_20260523_001");
    }

    @Test
    void returnsNullForMissingPerfRunId() {
        assertThat(PerfRunContext.extract(Map.of("orderId", "O-1"))).isNull();
    }

    @Test
    void returnsNullForBlankPerfRunId() {
        assertThat(PerfRunContext.extract(Map.of("perfRunId", "   "))).isNull();
    }

    @Test
    void rejectsUnsafeCharactersByReturningNull() {
        assertThat(PerfRunContext.extract(Map.of("perfRunId", "perf;drop"))).isNull();
    }
}
