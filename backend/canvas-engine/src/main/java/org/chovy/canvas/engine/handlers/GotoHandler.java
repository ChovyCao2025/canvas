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
        String nodeId = string(config, "__nodeId", "goto");
        int maxJumps = number(config.get("maxJumps"), 1);
        if (maxJumps <= 0) {
            return Mono.just(NodeResult.fail("GOTO 必须配置 maxJumps > 0"));
        }
        int nextCount = ctx.getJumpCounts().merge(nodeId, 1, Integer::sum);
        if (nextCount > maxJumps) {
            // 跳转次数超限时走 max_exceeded，防止配置成无限回环。
            return Mono.just(NodeResult.routed("max_exceeded", string(config, "maxExceededNodeId", null),
                    Map.of(MapFieldKeys.JUMP_COUNT, nextCount, MapFieldKeys.JUMP_EXCEEDED, true)));
        }
        return Mono.just(NodeResult.routed("goto", string(config, "targetNodeId", null),
                Map.of(MapFieldKeys.JUMP_COUNT, nextCount)));
    }

    /**
     * 执行 number 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param value value 待写入、比较或转换的业务值
     * @param fallback fallback 方法执行所需的业务参数
     * @return 计算得到的数值结果
     */
    private int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    /**
     * 执行 string 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param key key 对应的缓存键、配置键或业务键
     * @param fallback fallback 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
     */
    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }
}
