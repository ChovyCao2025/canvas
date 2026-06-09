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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TransferJourneyHandler 参与 engine.handlers 场景的画布执行引擎处理。
 */
@Component
@NodeHandlerType(NodeType.TRANSFER_JOURNEY)
public class TransferJourneyHandler implements NodeHandler {

    private final CanvasExecutionService executionService;

    /**
     * 创建 TransferJourneyHandler 实例并注入 engine.handlers 场景依赖。
     * @param executionService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public TransferJourneyHandler(@Lazy CanvasExecutionService executionService) {
        this.executionService = executionService;
    }

    /**
     * 执行旅程转移节点并触发目标画布。
     *
     * <p>方法读取 {@code targetJourneyId}，可按 {@code carryContext} 把触发 payload 和已产出的节点输出复制到新旅程输入；
     * 随后调用执行服务以 TRANSFER_JOURNEY 触发类型启动目标旅程。当前节点本身不设置下一跳，成功输出目标旅程和来源执行 ID。
     *
     * @param config 当前节点配置，包含目标旅程 ID 和是否携带上下文
     * @param ctx 当前执行上下文，提供用户、执行实例和节点输出
     * @return 目标旅程触发完成后的节点结果
     */
    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        Long targetJourneyId = longValue(config == null ? null : config.get("targetJourneyId"));
        if (targetJourneyId == null) {
            return Mono.just(NodeResult.fail("TRANSFER_JOURNEY: targetJourneyId is required"));
        }
        String userId = ctx == null ? null : ctx.getUserId();
        String sourceExecutionId = ctx == null ? null : ctx.getExecutionId();
        Map<String, Object> payload = Boolean.TRUE.equals(config == null ? null : config.get("carryContext"))
                /**
                 * 执行 carriedPayload 流程，围绕 carried payload 完成校验、计算或结果组装。
                 *
                 * @return 返回 carriedPayload 流程生成的业务结果。
                 */
                ? carriedPayload(ctx)
                : new LinkedHashMap<>();
        if (sourceExecutionId != null) {
            payload.put(MapFieldKeys.SOURCE_EXECUTION_ID, sourceExecutionId);
        }
        String msgId = sourceExecutionId + ":transfer:" + targetJourneyId;
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("targetJourneyId", targetJourneyId);
        if (sourceExecutionId != null) {
            output.put(MapFieldKeys.SOURCE_EXECUTION_ID, sourceExecutionId);
        }
        return executionService.trigger(targetJourneyId, userId, TriggerType.TRANSFER_JOURNEY,
                        NodeType.DIRECT_CALL, null, payload, msgId, false)
                .thenReturn(NodeResult.ok(null, output));
    }

    /**
     * 构造转旅程时携带的上下文载荷。
     *
     * @param ctx 当前执行上下文
     * @return 合并触发载荷和节点输出后的载荷
     */
    private Map<String, Object> carriedPayload(ExecutionContext ctx) {
        Map<String, Object> payload = new LinkedHashMap<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (ctx == null) {
            return payload;
        }
        if (ctx.getTriggerPayload() != null) {
            payload.putAll(ctx.getTriggerPayload());
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        ctx.getNodeOutputs().forEach((nodeId, output) -> output.forEach((key, value) -> {
            if (key != null && value != null) {
                payload.put(key, value);
                payload.put(nodeId + "." + key, value);
            }
        }));
        // 汇总前面计算出的状态和明细，返回给调用方。
        return payload;
    }

    /**
     * 将对象转换为 Long。
     *
     * @param value 原始值
     * @return Long 值，空值返回 null
     */
    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return Long.parseLong(value.toString());
    }
}
