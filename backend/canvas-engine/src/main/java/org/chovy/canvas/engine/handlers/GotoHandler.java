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
 * 跳转节点处理器。
 *
 * <p>由 DAG 执行器在运行画布节点时调用，读取节点 config 与执行上下文，产出 NodeResult 决定后续路由。
 * <p>处理器应保持单节点职责，跨节点编排、重试和状态持久化由执行引擎统一管理。
 */
@Component
@NodeHandlerType(NodeType.GOTO)
public class GotoHandler implements NodeHandler {
    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String nodeId = string(config, "__nodeId", "goto");
        int maxJumps = number(config.get("maxJumps"), 1);
        if (maxJumps <= 0) {
            return Mono.just(NodeResult.fail("GOTO 必须配置 maxJumps > 0"));
        }
        int nextCount = ctx.getJumpCounts().merge(nodeId, 1, Integer::sum);
        if (nextCount > maxJumps) {
            return Mono.just(NodeResult.routed("max_exceeded", string(config, "maxExceededNodeId", null),
                    Map.of(MapFieldKeys.JUMP_COUNT, nextCount, MapFieldKeys.JUMP_EXCEEDED, true)));
        }
        return Mono.just(NodeResult.routed("goto", string(config, "targetNodeId", null),
                Map.of(MapFieldKeys.JUMP_COUNT, nextCount)));
    }

    private int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }
}
