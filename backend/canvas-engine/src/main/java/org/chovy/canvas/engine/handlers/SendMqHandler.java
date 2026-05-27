package org.chovy.canvas.engine.handlers;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.dal.dataobject.MqMessageDefinitionDO;
import org.chovy.canvas.dal.mapper.MqMessageDefinitionMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
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

    /** RocketMQ 发送模板，用于投递画布触发消息。 */
    private final RocketMQTemplate rocketMQTemplate;

    /** MQ 消息定义访问器，用于按消息编码查找启用定义。 */
    private final MqMessageDefinitionMapper mqMapper;

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
                    MqMessageDefinitionDO def = mqMapper.selectOne(
                            new LambdaQueryWrapper<MqMessageDefinitionDO>()
                                    .eq(MqMessageDefinitionDO::getMessageCode, messageCodeKey)
                                    .eq(MqMessageDefinitionDO::getEnabled, 1));
                    if (def == null) {
                        return NodeResult.fail("SEND_MQ: 找不到消息定义 messageCode=" + messageCodeKey);
                    }
                    if (def.getTopic() == null || def.getTopic().isBlank()) {
                        return NodeResult.fail("SEND_MQ: 消息定义 topic 未配置 messageCode=" + messageCodeKey);
                    }

                    Map<String, Object> payload = buildPayload(config, ctx);
                    MqTriggerMessage message = new MqTriggerMessage(ctx.getUserId(), messageCodeKey, payload);
                    String destination = mqTopic + ":" + def.getTopic().trim();

                    // 使用 userId 做 orderly sharding key，保证同一用户消息顺序。
                    SendResult sendResult = rocketMQTemplate.syncSendOrderly(destination, message, ctx.getUserId());
                    if (sendResult == null || sendResult.getSendStatus() != SendStatus.SEND_OK) {
                        SendStatus status = sendResult != null ? sendResult.getSendStatus() : null;
                        throw new IllegalStateException("RocketMQ send status=" + status);
                    }

                    log.info("[SEND_MQ] 发送成功 destination={} userId={}", destination, ctx.getUserId());
                    return NodeResult.ok(nextNodeId, Map.of(MapFieldKeys.MQ_SENT, true));
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    log.error("[SEND_MQ] 发送失败 messageCode={}: {}", messageCodeKey, e.getMessage(), e);
                    return Mono.just(NodeResult.fail("SEND_MQ: 消息发送失败: " + e.getMessage()));
                });
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
     * 执行 copy Params 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param payload payload 请求体、消息体或事件载荷
     * @param rawParams rawParams 方法执行所需的业务参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
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
