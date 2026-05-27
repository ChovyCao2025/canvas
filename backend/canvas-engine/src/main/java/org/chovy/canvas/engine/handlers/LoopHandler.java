package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 循环节点处理器。
 *
 * <p>由 DAG 执行器在运行画布节点时调用，读取节点 config 与执行上下文，产出 NodeResult 决定后续路由。
 * <p>处理器应保持单节点职责，跨节点编排、重试和状态持久化由执行引擎统一管理。
 */
@Component
@NodeHandlerType(NodeType.LOOP)
public class LoopHandler implements NodeHandler {
    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String nodeId = string(config, "__nodeId", "loop");
        int maxIterations = number(config.get("maxIterations"), 1);
        if (maxIterations <= 0) {
            return Mono.just(NodeResult.fail("LOOP 必须配置 maxIterations > 0"));
        }
        int nextCount = ctx.getLoopIterations().merge(nodeId, 1, Integer::sum);
        boolean exit = exitConditionMet(config, ctx);
        if (exit) {
            return Mono.just(NodeResult.routed("exit", string(config, "exitNodeId", string(config, "nextNodeId", null)),
                    Map.of(MapFieldKeys.LOOP_ITERATIONS, nextCount, MapFieldKeys.LOOP_EXITED, true)));
        }
        if (nextCount > maxIterations) {
            return Mono.just(NodeResult.routed("max_exceeded", string(config, "maxExceededNodeId", null),
                    Map.of(MapFieldKeys.LOOP_ITERATIONS, nextCount, MapFieldKeys.LOOP_EXCEEDED, true)));
        }
        return Mono.just(NodeResult.routed("loop", string(config, "loopStartNodeId", null),
                Map.of(MapFieldKeys.LOOP_ITERATIONS, nextCount)));
    }

    private boolean exitConditionMet(Map<String, Object> config, ExecutionContext ctx) {
        String field = string(config, "exitField", null);
        if (field == null) return false;
        Object actual = ctx.getContextValue(field);
        Object expected = config.get("exitValue");
        return expected == null ? actual != null : expected.toString().equals(String.valueOf(actual));
    }

    private int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }
}
