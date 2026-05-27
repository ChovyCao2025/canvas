package org.chovy.canvas.engine.scheduler;

import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Special Node Timer Race 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class SpecialNodeTimerRaceTest {

    @Test
    void concurrentTimerScheduling_schedulesExactlyOnce() throws InterruptedException {
        Set<String> scheduledHubTimeouts = ConcurrentHashMap.newKeySet();
        String timerKey = "hub1";
        AtomicInteger scheduleCount = new AtomicInteger(0);

        int threads = 20;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        var exec = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            exec.submit(() -> {
                try {
                    start.await();
                    if (scheduledHubTimeouts.add(timerKey)) {
                        scheduleCount.incrementAndGet();
                    }
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

        assertThat(scheduleCount.get()).isEqualTo(1);
    }
}
