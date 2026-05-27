package org.chovy.canvas.engine.handlers;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Delay 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
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
