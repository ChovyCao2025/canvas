package org.chovy.canvas.perf;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Perf Run Context 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
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
