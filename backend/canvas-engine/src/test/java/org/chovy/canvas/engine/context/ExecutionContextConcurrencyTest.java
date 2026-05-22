package org.chovy.canvas.engine.context;

import org.junit.jupiter.api.RepeatedTest;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class ExecutionContextConcurrencyTest {

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
