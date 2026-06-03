package org.chovy.canvas.engine.trigger;

import org.chovy.canvas.engine.lane.ExecutionLane;
import org.chovy.canvas.infrastructure.redis.RedisKeyUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.Disposable;
import reactor.core.Disposables;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InFlightExecutionRegistryConcurrencyTest {

    @Mock StringRedisTemplate redis;
    @Mock RedisKeyUtil keys;

    private InFlightExecutionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new InFlightExecutionRegistry(redis, keys);
        ReflectionTestUtils.setField(registry, "globalTimeoutSec", 600L);
        when(keys.inflightCanvas(1L)).thenReturn("canvas:inflight:1");
        when(keys.inflightLane(ExecutionLane.STANDARD)).thenReturn("canvas:inflight:lane:standard");
        when(keys.inflightGlobal()).thenReturn("canvas:inflight:global");
        when(redis.execute(any(RedisScript.class), anyList(),
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(1L);
        when(redis.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(1L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void deregisterDoesNotRemoveConcurrentAcquire() throws Exception {
        CountDownLatch emptyObserved = new CountDownLatch(1);
        CountDownLatch newPutObserved = new CountDownLatch(1);
        CountDownLatch continueRemove = new CountDownLatch(1);
        PausingExecutionMap executionMap = new PausingExecutionMap(
                emptyObserved, newPutObserved, continueRemove);
        executionMap.put("old", Disposables.swap());

        ConcurrentHashMap<Long, ConcurrentHashMap<String, Disposable.Swap>> localRegistry =
                (ConcurrentHashMap<Long, ConcurrentHashMap<String, Disposable.Swap>>)
                        ReflectionTestUtils.getField(registry, "localRegistry");
        localRegistry.put(1L, executionMap);

        Thread remove = Thread.ofVirtual().start(() -> registry.deregister(1L, "old"));
        assertThat(emptyObserved.await(2, TimeUnit.SECONDS)).isTrue();
        Thread acquire = Thread.ofVirtual().start(() ->
                registry.tryAcquire(1L, "new", ExecutionLane.STANDARD, 10, 10, 10));
        newPutObserved.await(500, TimeUnit.MILLISECONDS);
        continueRemove.countDown();
        remove.join();
        acquire.join();

        assertThat(localRegistry.get(1L)).isNotNull();
        assertThat(localRegistry.get(1L)).containsKey("new");
    }

    static final class PausingExecutionMap extends ConcurrentHashMap<String, Disposable.Swap> {
        private final CountDownLatch emptyObserved;
        private final CountDownLatch newPutObserved;
        private final CountDownLatch continueRemove;

        PausingExecutionMap(CountDownLatch emptyObserved, CountDownLatch newPutObserved,
                            CountDownLatch continueRemove) {
            this.emptyObserved = emptyObserved;
            this.newPutObserved = newPutObserved;
            this.continueRemove = continueRemove;
        }

        @Override
        public Disposable.Swap put(String key, Disposable.Swap value) {
            Disposable.Swap previous = super.put(key, value);
            if ("new".equals(key)) {
                newPutObserved.countDown();
            }
            return previous;
        }

        @Override
        public boolean isEmpty() {
            boolean empty = super.isEmpty();
            if (empty) {
                emptyObserved.countDown();
                try {
                    if (!continueRemove.await(2, TimeUnit.SECONDS)) {
                        fail("timed out while forcing deregister race");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    fail("interrupted while forcing deregister race");
                }
            }
            return empty;
        }
    }
}
