package org.chovy.canvas.health;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.scheduler.TraceWriteBuffer;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * CanvasEngineHealthIndicator 支撑 health 场景的后端处理。
 */
@Component
@RequiredArgsConstructor
public class CanvasEngineHealthIndicator implements HealthIndicator {

    private final CanvasDisruptorService disruptorService;
    private final TraceWriteBuffer traceWriteBuffer;

    /**
     * health 查询 health 场景的业务数据。
     * @return 返回 health 流程生成的业务结果。
     */
    @Override
    public Health health() {
        return Health.up()
                .withDetail("disruptorBacklog", disruptorService.backlog())
                .withDetail("traceBufferPending", traceWriteBuffer.pendingCount())
                .build();
    }
}
