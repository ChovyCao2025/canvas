package org.chovy.canvas.engine.context;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Execution Context Perf Run 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class ExecutionContextPerfRunTest {

    @Test
    void storesPerfRunIdOnExecutionContext() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setPerfRunId("perf_20260523_001");

        assertThat(ctx.getPerfRunId()).isEqualTo("perf_20260523_001");
    }
}
