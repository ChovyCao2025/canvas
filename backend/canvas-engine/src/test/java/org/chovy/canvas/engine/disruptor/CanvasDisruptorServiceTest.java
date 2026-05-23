package org.chovy.canvas.engine.disruptor;

import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.RingBuffer;
import org.chovy.canvas.engine.scheduler.CanvasMetrics;
import org.chovy.canvas.engine.request.CanvasExecutionRequestExecutor;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
}
