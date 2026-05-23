package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 延迟器：使用 Mono.delay() 在响应式调度器上等待，不占用线程。
 * 比 Thread.sleep() 更符合 Reactor 异步模型。
 *
 * 适用场景：
 * - 短延时等待（秒/分钟/小时）；
 * - 与后续节点编排形成“冷却后继续”流程。
 */
@Component @NodeHandlerType("DELAY")
public class DelayHandler implements NodeHandler {

    /**
     * 延迟节点执行。
     *
     * <p>仅负责“时间等待 + 路由放行”，不修改上下文字段。
     */
    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        // duration + unit 共同决定等待时长，默认 SECOND
        int duration    = config.get("duration") instanceof Number n ? n.intValue() : 0;
        String unit     = (String) config.getOrDefault("unit", "SECOND");
        String nextNodeId = (String) config.get("nextNodeId");

        // 统一换算成毫秒，后续交给 Mono.delay 做非阻塞等待
        long millis = switch (unit) {
            case "MINUTE" -> TimeUnit.MINUTES.toMillis(duration);
            case "HOUR"   -> TimeUnit.HOURS.toMillis(duration);
            default       -> TimeUnit.SECONDS.toMillis(duration);
        };

        // Mono.delay() 不占用线程，优于 Thread.sleep()
        return Mono.delay(Duration.ofMillis(millis))
                // 到时后直接路由到下游
                .thenReturn(NodeResult.ok(nextNodeId, Map.of()));
    }
}
