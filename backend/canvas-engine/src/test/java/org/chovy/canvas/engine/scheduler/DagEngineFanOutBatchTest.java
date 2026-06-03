package org.chovy.canvas.engine.scheduler;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies large-audience fan-out is batched and rate-limited. */
class DagEngineFanOutBatchTest {

    @Test
    void fanOutSubmitsBatchesWithoutExceedingConcurrencyLimit() {
        int batchSize = 10;
        int maxConcurrent = 3;
        List<String> allUsers = Stream.iterate(0, i -> i + 1)
                .limit(100)
                .map(i -> "user-" + i)
                .toList();
        CopyOnWriteArrayList<List<String>> submittedBatches = new CopyOnWriteArrayList<>();
        AtomicInteger currentInFlight = new AtomicInteger(0);
        AtomicInteger maxInFlight = new AtomicInteger(0);

        DagEngine.FanOutBatcher batcher = new DagEngine.FanOutBatcher(batchSize, maxConcurrent);
        try {
            batcher.fanOut(allUsers.stream(), batch -> {
                int inFlight = currentInFlight.incrementAndGet();
                maxInFlight.updateAndGet(current -> Math.max(current, inFlight));
                try {
                    submittedBatches.add(batch);
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    currentInFlight.decrementAndGet();
                }
            });
        } finally {
            batcher.shutdown();
        }

        assertThat(submittedBatches).hasSize(10);
        assertThat(submittedBatches).allSatisfy(batch -> assertThat(batch).hasSize(10));
        assertThat(submittedBatches.stream().flatMap(List::stream).distinct()).hasSize(100);
        assertThat(maxInFlight.get()).isLessThanOrEqualTo(maxConcurrent);
    }
}
