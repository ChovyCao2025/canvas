package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 旅程转移节点处理器。
 *
 * <p>由 DAG 执行器在运行画布节点时调用，读取节点 config 与执行上下文，产出 NodeResult 决定后续路由。
 * <p>处理器应保持单节点职责，跨节点编排、重试和状态持久化由执行引擎统一管理。
 */
@Component
@NodeHandlerType(NodeType.TRANSFER_JOURNEY)
public class TransferJourneyHandler implements NodeHandler {
    /** 画布执行服务，用于异步触发目标旅程。 */
    private final CanvasExecutionService executionService;

    /**
     * 构造 TransferJourneyHandler 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param executionService executionService 方法执行所需的业务参数
     */
    public TransferJourneyHandler(@Lazy CanvasExecutionService executionService) {
        this.executionService = executionService;
    }

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
        Object target = config.get("targetJourneyId");
        if (target == null) {
            return Mono.just(NodeResult.fail("TRANSFER_JOURNEY 必须配置 targetJourneyId"));
        }
        Long targetJourneyId = Long.parseLong(target.toString());
        Map<String, Object> payload = new HashMap<>();
        if (Boolean.TRUE.equals(config.get("carryContext"))) {
            // carryContext 打开时导出父旅程上下文；导出结果已包含触发载荷且保留节点输出优先级。
            payload.putAll(ctx.exportContextValues());
        }
        payload.put(MapFieldKeys.SOURCE_EXECUTION_ID, ctx.getExecutionId());
        // 旅程转移是异步触发副作用，当前旅程不等待目标旅程执行结果。
        executionService.trigger(
                        targetJourneyId,
                        ctx.getUserId(),
                        TriggerType.TRANSFER_JOURNEY,
                        NodeType.DIRECT_CALL,
                        null,
                        payload,
                        ctx.getExecutionId() + ":transfer:" + targetJourneyId,
                        false)
                .subscribe();
        return Mono.just(NodeResult.ok(string(config, "nextNodeId", null),
                Map.of(MapFieldKeys.TRANSFERRED_JOURNEY_ID, targetJourneyId)));
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
