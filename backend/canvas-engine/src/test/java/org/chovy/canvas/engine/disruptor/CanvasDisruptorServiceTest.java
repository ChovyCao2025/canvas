package org.chovy.canvas.engine.disruptor;

import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.RingBuffer;
import org.chovy.canvas.engine.scheduler.CanvasMetrics;
import org.chovy.canvas.engine.request.CanvasExecutionRequestExecutor;
import org.chovy.canvas.engine.reactive.BackgroundSubscriptionRegistry;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Canvas Disruptor 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class CanvasDisruptorServiceTest {

    private CanvasDisruptorService service;

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.shutdown();
        }
    }

    @Test
    void publishThrowsWhenRingBufferHasNoAvailableCapacity() throws Exception {
        CanvasMetrics metrics = mock(CanvasMetrics.class);
        service = new CanvasDisruptorService(
                mock(CanvasExecutionService.class), mock(CanvasExecutionRequestExecutor.class), metrics, 1024, 1);

        @SuppressWarnings("unchecked")
        RingBuffer<CanvasExecutionEvent> ringBuffer = mock(RingBuffer.class);
        when(ringBuffer.tryNext()).thenThrow(InsufficientCapacityException.INSTANCE);
        ReflectionTestUtils.setField(service, "ringBuffer", ringBuffer);

        assertThatThrownBy(() -> service.publish(
                1L, "user-1", "MQ", "MQ_TRIGGER", "order.paid", Map.of(), "msg-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Disruptor Ring Buffer is full");

        verify(ringBuffer, never()).publish(0L);
        verify(metrics).recordDisruptorOverflow("MQ");
    }

    @Test
    void publishRecordsPublishedMetricAfterPuttingEventIntoRingBuffer() throws Exception {
        CanvasMetrics metrics = mock(CanvasMetrics.class);
        service = new CanvasDisruptorService(
                mock(CanvasExecutionService.class), mock(CanvasExecutionRequestExecutor.class), metrics, 1024, 1);

        @SuppressWarnings("unchecked")
        RingBuffer<CanvasExecutionEvent> ringBuffer = mock(RingBuffer.class);
        CanvasExecutionEvent event = new CanvasExecutionEvent();
        when(ringBuffer.tryNext()).thenReturn(0L);
        when(ringBuffer.get(0L)).thenReturn(event);
        ReflectionTestUtils.setField(service, "ringBuffer", ringBuffer);

        service.publish(1L, "user-1", "MQ", "MQ_TRIGGER", "order.paid", Map.of("k", "v"), "msg-1");

        verify(ringBuffer).publish(0L);
        verify(metrics).recordDisruptorPublished("MQ");
    }

    @Test
    void publishRequestRecordsPublishedMetricForPersistentRequest() throws Exception {
        CanvasMetrics metrics = mock(CanvasMetrics.class);
        service = new CanvasDisruptorService(
                mock(CanvasExecutionService.class), mock(CanvasExecutionRequestExecutor.class), metrics, 1024, 1);

        @SuppressWarnings("unchecked")
        RingBuffer<CanvasExecutionEvent> ringBuffer = mock(RingBuffer.class);
        CanvasExecutionEvent event = new CanvasExecutionEvent();
        when(ringBuffer.tryNext()).thenReturn(0L);
        when(ringBuffer.get(0L)).thenReturn(event);
        ReflectionTestUtils.setField(service, "ringBuffer", ringBuffer);

        service.publishRequest("req-1");

        verify(ringBuffer).publish(0L);
        verify(metrics).recordDisruptorPublished("REQUEST");
    }

    @Test
    void workerTracksCanvasExecutionSubscriptionAsBackgroundTask() throws Exception {
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        CanvasMetrics metrics = mock(CanvasMetrics.class);
        BackgroundSubscriptionRegistry backgroundSubscriptions = new BackgroundSubscriptionRegistry();
        CountDownLatch subscribed = new CountDownLatch(1);
        when(executionService.triggerFromDisruptor(
                eq(1L), eq("user-1"), eq("MQ"), eq("MQ_TRIGGER"), eq("order.paid"),
                any(), eq("msg-1"), any(), any()))
                .thenReturn(Mono.<Map<String, Object>>never().doOnSubscribe(subscription -> subscribed.countDown()));
        service = new CanvasDisruptorService(
                executionService,
                mock(CanvasExecutionRequestExecutor.class),
                metrics,
                1024,
                1,
                backgroundSubscriptions);

        service.publish(1L, "user-1", "MQ", "MQ_TRIGGER", "order.paid", Map.of(), "msg-1");

        assertThat(subscribed.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat((Integer) ReflectionTestUtils.invokeMethod(backgroundSubscriptions, "activeCount"))
                .isEqualTo(1);
    }
}
