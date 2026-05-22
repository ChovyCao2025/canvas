package org.chovy.canvas.engine.scheduler;

import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;

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
