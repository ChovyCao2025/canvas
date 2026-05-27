package org.chovy.canvas.engine.context;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Execution Context Concurrency 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class ExecutionContextConcurrencyTest {

    @Test
    void timeout_and_suppressed_are_terminal_node_states() {
        ExecutionContext ctx = new ExecutionContext();

        ctx.setNodeStatus("timeout-node", NodeStatus.TIMEOUT);
        ctx.setNodeStatus("suppressed-node", NodeStatus.SUPPRESSED);
        ctx.setNodeStatus("waiting-node", NodeStatus.WAITING);

        assertThat(ctx.isNodeDone("timeout-node")).isTrue();
        assertThat(ctx.isNodeDone("suppressed-node")).isTrue();
        assertThat(ctx.isNodeDone("waiting-node")).isFalse();
    }

    @RepeatedTest(20)
    void concurrentPutNodeOutput_doesNotCorrupt() throws InterruptedException {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("c1");
        ctx.setCanvasId(1L);
        ctx.setVersionId(1L);
        ctx.setUserId("u1");
        ctx.setTriggerType("SCHEDULED");

        int threads = 20;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        var exec = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            exec.submit(() -> {
                try {
                    start.await();
                    ctx.putNodeOutput("node-" + idx, Map.of("key-" + idx, "val-" + idx));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await();
        exec.shutdown();

        assertThat(ctx.getNodeOutputs()).hasSize(threads);
        for (int i = 0; i < threads; i++) {
            assertThat(ctx.getContextValue("key-" + i)).isEqualTo("val-" + i);
        }
        assertThatNoException().isThrownBy(() ->
            ctx.getNodeOutputs().forEach((k, v) -> { /* read all */ }));
    }
}
