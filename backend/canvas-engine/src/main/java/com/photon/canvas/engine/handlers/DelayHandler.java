package com.photon.canvas.engine.handlers;

import com.photon.canvas.engine.context.ExecutionContext;
import com.photon.canvas.engine.handler.NodeHandler;
import com.photon.canvas.engine.handler.NodeHandlerType;
import com.photon.canvas.engine.handler.NodeResult;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 延迟器：等待 duration 指定的时长后继续。
 * 使用 Java 21 虚拟线程 sleep，不占 OS 线程。
 */
@NodeHandlerType("DELAY")
public class DelayHandler implements NodeHandler {

    @Override
    public NodeResult execute(Map<String, Object> config, ExecutionContext ctx) {
        int duration = config.get("duration") instanceof Number n ? n.intValue() : 0;
        String unit  = (String) config.getOrDefault("unit", "SECOND");
        String nextNodeId = (String) config.get("nextNodeId");

        long millis = switch (unit) {
            case "MINUTE" -> TimeUnit.MINUTES.toMillis(duration);
            case "HOUR"   -> TimeUnit.HOURS.toMillis(duration);
            default       -> TimeUnit.SECONDS.toMillis(duration);
        };

        try {
            Thread.sleep(millis);  // 虚拟线程 sleep，不占 OS 线程
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return NodeResult.fail("延迟器被中断");
        }

        return NodeResult.ok(nextNodeId, Map.of());
    }
}
