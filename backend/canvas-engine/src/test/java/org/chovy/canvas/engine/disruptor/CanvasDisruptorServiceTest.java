package org.chovy.canvas.engine.disruptor;

import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.RingBuffer;
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
        service = new CanvasDisruptorService(mock(CanvasExecutionService.class), 1024, 1);

        @SuppressWarnings("unchecked")
        RingBuffer<CanvasExecutionEvent> ringBuffer = mock(RingBuffer.class);
        when(ringBuffer.tryNext()).thenThrow(InsufficientCapacityException.INSTANCE);
        ReflectionTestUtils.setField(service, "ringBuffer", ringBuffer);

        assertThatThrownBy(() -> service.publish(
                1L, "user-1", "MQ", "MQ_TRIGGER", "order.paid", Map.of(), "msg-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Disruptor Ring Buffer is full");

        verify(ringBuffer, never()).publish(0L);
    }
}
