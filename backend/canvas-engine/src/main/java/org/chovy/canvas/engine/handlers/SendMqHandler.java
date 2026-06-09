package org.chovy.canvas.engine.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.dal.dataobject.MqMessageDefinitionDO;
import org.chovy.canvas.domain.meta.MqMessageDefinitionService;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.infrastructure.mq.CanvasMessageBus;
import org.chovy.canvas.infrastructure.mq.MqTriggerMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 发送 MQ 消息节点：将业务消息发送到 RocketMQ CANVAS_MQ_TRIGGER topic。
 * Tag = MqMessageDefinitionDO.topic，供消费端路由到对应画布。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@NodeHandlerType("SEND_MQ")
public class SendMqHandler implements NodeHandler {

    /** 画布消息总线，用于投递画布触发消息。 */
    private final CanvasMessageBus messageBus;

    /** MQ 消息定义服务，用于隔离 handler 与持久层细节。 */
    private final MqMessageDefinitionService mqMessageDefinitionService;

    /** RocketMQ 画布触发消息 Topic。 */
    @Value("${canvas.mq.topic:CANVAS_MQ_TRIGGER}")
    private String mqTopic;

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
        String rawMessageCodeKey = (String) config.get("messageCodeKey");
        String messageCodeKey = rawMessageCodeKey != null ? rawMessageCodeKey.trim() : null;
        String nextNodeId = (String) config.get(MapFieldKeys.NEXT_NODE_ID);

        if (messageCodeKey == null || messageCodeKey.isBlank()) {
            return Mono.just(NodeResult.fail("SEND_MQ: messageCodeKey 未配置"));
        }

        return Mono.fromCallable(() -> {
                    // 消息编码先解析为启用的消息定义，再用定义中的 topic 作为 RocketMQ tag。
                    MqMessageDefinitionDO def =
                            mqMessageDefinitionService.findEnabledByMessageCode(messageCodeKey);
                    if (def == null) {
                        return NodeResult.fail("SEND_MQ: 找不到消息定义 messageCode=" + messageCodeKey);
                    }
                    if (def.getTopic() == null || def.getTopic().isBlank()) {
                        return NodeResult.fail("SEND_MQ: 消息定义 topic 未配置 messageCode=" + messageCodeKey);
                    }

                    Map<String, Object> payload = buildPayload(config, ctx);
                    MqTriggerMessage message = new MqTriggerMessage(ctx.getUserId(), messageCodeKey, payload);
                    String tag = def.getTopic().trim();

                    // 使用 userId 做 orderly sharding key，保证同一用户消息顺序。
                    messageBus.publishOrderly(mqTopic, tag, message, ctx.getUserId());

                    log.info("[SEND_MQ] 发送成功 destination={}:{} userId={}", mqTopic, tag, ctx.getUserId());
                    return NodeResult.ok(nextNodeId, Map.of(MapFieldKeys.MQ_SENT, true));
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    log.error("[SEND_MQ] 发送失败 messageCode={}: {}", messageCodeKey, e.getMessage(), e);
                    return Mono.just(NodeResult.fail("SEND_MQ: 消息发送失败: " + e.getMessage()));
                });
    }

    /**
     * 声明 SEND_MQ 节点会产生外部 MQ 投递副作用，调度层需要先占用节点副作用幂等记录。
     *
     * <p>返回 {@code true} 后，同一执行上下文、节点和操作键重复进入时会复用已完成输出，避免重复发送 RocketMQ 消息。
     *
     * @param config 当前节点配置，主要读取显式幂等键和消息编码
     * @param ctx 画布执行上下文，提供租户、执行实例和用户维度
     * @return 始终为 {@code true}，表示本节点必须走副作用幂等保护
     */
    @Override
    public boolean requiresSideEffectIdempotency(Map<String, Object> config, ExecutionContext ctx) {
        return true;
    }

    /**
     * 构造 SEND_MQ 副作用操作键。
     *
     * <p>优先使用节点配置中的显式幂等键；未配置时按用户和 {@code messageCodeKey} 生成稳定键，调度层再结合执行实例、
     * 节点 ID 和节点类型计算最终哈希键。该键决定重复执行时是否跳过 RocketMQ 投递并复用上下文输出。
     *
     * @param config 当前节点配置，读取 {@code idempotencyKey} 和 {@code messageCodeKey}
     * @param ctx 画布执行上下文，读取用户 ID
     * @return 用于节点副作用幂等表的业务操作键
     */
    @Override
    public String sideEffectOperationKey(Map<String, Object> config, ExecutionContext ctx) {
        Object explicit = config.get(MapFieldKeys.IDEMPOTENCY_KEY);
        if (explicit != null && !explicit.toString().isBlank()) {
            return explicit.toString();
        }
        Object messageCodeKey = config.get("messageCodeKey");
        return ctx.getUserId() + ":send-mq:" + (messageCodeKey == null ? "" : messageCodeKey);
    }

    /**
     * 构建、解析或转换 build Payload 相关的业务数据。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 按业务键组织的映射结果
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> buildPayload(Map<String, Object> config, ExecutionContext ctx) {
        Map<String, Object> payload = new HashMap<>();
        copyParams(payload, config.get(MapFieldKeys.PARAMS), ctx);
        copyParams(payload, config.get(MapFieldKeys.INPUT_PARAMS), ctx);
        return payload;
    }

    /**
     * 将节点参数配置复制到 MQ payload。
     *
     * @param payload 目标 payload
     * @param rawParams 原始参数配置
     * @param ctx 执行上下文
     */
    private void copyParams(Map<String, Object> payload, Object rawParams, ExecutionContext ctx) {
        if (rawParams == null) {
            return;
        }
        if (rawParams instanceof Map<?, ?> paramsMap) {
            paramsMap.forEach((key, value) -> {
                if (key != null) {
                    // Map 形态直接按 key 写入 payload，value 仍支持上下文表达式。
                    payload.put(String.valueOf(key), resolveValue(value, ctx));
                }
            });
            return;
        }
        if (rawParams instanceof List<?> paramsList) {
            for (Object item : paramsList) {
                if (!(item instanceof Map<?, ?> param)) {
                    throw new IllegalArgumentException("SEND_MQ: params 列表元素必须是对象");
                }
                Object key = param.get("key");
                if (key != null) {
                    // 列表形态兼容前端参数配置器的 key/value 结构。
                    payload.put(String.valueOf(key), resolveValue(param.get("value"), ctx));
                }
            }
            return;
        }
        throw new IllegalArgumentException("SEND_MQ: params 必须是对象或列表");
    }

    /**
     * 构建、解析或转换 resolve Value 相关的业务数据。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param value value 待写入、比较或转换的业务值
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 方法执行后的业务结果
     */
    private Object resolveValue(Object value, ExecutionContext ctx) {
        if (value instanceof String stringValue) {
            String normalized = stringValue.startsWith("$${") ? stringValue.substring(1) : stringValue;
            if (normalized.startsWith("${") && normalized.endsWith("}")) {
                // 兼容 Flyway 转义后的 $${field} 和运行时标准 ${field}。
                return ctx.getContextValue(normalized.substring(2, normalized.length() - 1));
            }
        }
        return value;
    }
}
