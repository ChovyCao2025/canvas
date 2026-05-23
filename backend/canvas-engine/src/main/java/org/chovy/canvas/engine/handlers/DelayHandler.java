package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 延迟器：使用 Mono.delay() 在响应式调度器上等待，不占用线程。
 * 比 Thread.sleep() 更符合 Reactor 异步模型。
 */
@Component @NodeHandlerType("DELAY")
public class DelayHandler implements NodeHandler {

    @Value("${canvas.delay.jitter-max-ms:0}")
    private long defaultJitterMaxMs;

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        int duration    = config.get("duration") instanceof Number n ? n.intValue() : 0;
        String unit     = (String) config.getOrDefault("unit", "SECOND");
        String nextNodeId = (String) config.get("nextNodeId");
        long jitterMaxMs = config.get("jitterMaxMs") instanceof Number n
                ? n.longValue()
                : defaultJitterMaxMs;

        long millis = switch (unit) {
            case "MINUTE" -> TimeUnit.MINUTES.toMillis(duration);
            case "HOUR"   -> TimeUnit.HOURS.toMillis(duration);
            default       -> TimeUnit.SECONDS.toMillis(duration);
        };

        // Mono.delay() 不占用线程，优于 Thread.sleep()；可选 jitter 用于 Wait 集中唤醒削峰。
        return Mono.delay(Duration.ofMillis(applyJitter(millis, jitterMaxMs)))
                .thenReturn(NodeResult.ok(nextNodeId, Map.of()));
    }

    static long applyJitter(long baseMillis, long jitterMaxMs) {
        if (jitterMaxMs <= 0) return baseMillis;
        return baseMillis + ThreadLocalRandom.current().nextLong(0, jitterMaxMs);
    }
}
