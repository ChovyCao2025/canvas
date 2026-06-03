package org.chovy.canvas.health;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.scheduler.TraceWriteBuffer;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CanvasEngineHealthIndicator implements HealthIndicator {

    private final CanvasDisruptorService disruptorService;
    private final TraceWriteBuffer traceWriteBuffer;

    @Override
    public Health health() {
        return Health.up()
                .withDetail("disruptorBacklog", disruptorService.backlog())
                .withDetail("traceBufferPending", traceWriteBuffer.pendingCount())
                .build();
    }
}
