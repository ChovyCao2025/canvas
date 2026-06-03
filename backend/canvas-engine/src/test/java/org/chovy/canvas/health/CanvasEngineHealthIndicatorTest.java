package org.chovy.canvas.health;

import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.scheduler.TraceWriteBuffer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CanvasEngineHealthIndicatorTest {

    @Test
    void reportsDisruptorAndTraceBufferBacklogDetails() {
        CanvasDisruptorService disruptorService = mock(CanvasDisruptorService.class);
        TraceWriteBuffer traceWriteBuffer = mock(TraceWriteBuffer.class);
        when(disruptorService.backlog()).thenReturn(42L);
        when(traceWriteBuffer.pendingCount()).thenReturn(7);

        Health health = new CanvasEngineHealthIndicator(disruptorService, traceWriteBuffer).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("disruptorBacklog", 42L)
                .containsEntry("traceBufferPending", 7);
    }
}
