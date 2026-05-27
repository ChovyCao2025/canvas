package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
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

    /** 默认最大随机抖动时间，单位毫秒。 */
    @Value("${canvas.delay.jitter-max-ms:0}")
    private long defaultJitterMaxMs;

    /**
     * 执行当前节点或服务的核心处理流程。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        int duration    = config.get("duration") instanceof Number n ? n.intValue() : 0;
        String unit     = (String) config.getOrDefault("unit", "SECOND");
        String nextNodeId = (String) config.get(MapFieldKeys.NEXT_NODE_ID);
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

    /**
     * 执行 apply Jitter 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param baseMillis baseMillis 方法执行所需的业务参数
     * @param jitterMaxMs jitterMaxMs 方法执行所需的业务参数
     * @return 计算得到的数值结果
     */
    static long applyJitter(long baseMillis, long jitterMaxMs) {
        if (jitterMaxMs <= 0) return baseMillis;
        return baseMillis + ThreadLocalRandom.current().nextLong(0, jitterMaxMs);
    }
}
