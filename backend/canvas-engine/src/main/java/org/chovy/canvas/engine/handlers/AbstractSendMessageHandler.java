package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.delivery.ReachDeliveryService;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeResult;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 发送类节点处理器抽象基类。
 *
 * <p>封装短信、邮件、Push、站内信、微信等触达节点共同的参数解析、策略校验和发送记录写入流程。
 * <p>子类只需要声明具体渠道类型和渠道特有字段，跨节点路由、重试和状态持久化仍由执行引擎统一管理。
 */
abstract class AbstractSendMessageHandler implements NodeHandler {

    /** 触达发送服务，负责统一落库和调用具体渠道。 */
    private final ReachDeliveryService deliveryService;

    /**
     * 构造 AbstractSendMessageHandler 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param deliveryService deliveryService 方法执行所需的业务参数
     */
    AbstractSendMessageHandler(ReachDeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    /**
     * 执行 channel 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @return 转换或查询得到的字符串结果
     */
    protected abstract String channel();

    protected String channel(Map<String, Object> config) {
        return channel();
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
        String channel = channel(config);
        String nodeId = string(config, "__nodeId", channel.toLowerCase() + "-send");
        String templateId = string(config, "templateId", string(config, "template_id", null));
        String successNodeId = string(config, "successNodeId", string(config, "nextNodeId", null));
        String failNodeId = string(config, "failNodeId", null);
        String idempotencyKey = string(config, "idempotencyKey",
                ctx.getExecutionId() + ":" + nodeId + ":" + channel);

        // 发送请求里固定带执行、画布、用户、节点和渠道信息，便于触达记录做幂等与审计。
        ReachDeliveryService.DeliveryRequest request = deliveryService.request(
                ctx.getExecutionId(),
                ctx.getCanvasId(),
                ctx.getUserId(),
                nodeId,
                channel,
                templateId,
                content(config),
                variables(config, ctx),
                idempotencyKey
        );

        return deliveryService.send(request)
                .map(result -> {
                    Map<String, Object> output = output(result);
                    if (result.sent()) {
                        return NodeResult.routed("success", successNodeId, output);
                    }
                    if (failNodeId != null && !failNodeId.isBlank()) {
                        // 配置了失败分支时，将发送失败作为可路由业务结果，而不是直接中断旅程。
                        return NodeResult.routed("fail", failNodeId, output);
                    }
                    return NodeResult.fail("触达发送失败: " + result.errorMessage());
                });
    }

    /**
     * 判断 is Reach Node 相关的业务数据。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @return 判断结果，true 表示校验通过或条件成立
     */
    @Override
    public boolean isReachNode() {
        return true;
    }

    /**
     * 执行 content 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @return 按业务键组织的映射结果
     */
    private Map<String, Object> content(Map<String, Object> config) {
        Map<String, Object> content = new LinkedHashMap<>();
        copy(config, content, "subject");
        copy(config, content, "previewText");
        copy(config, content, "title");
        copy(config, content, "body");
        copy(config, content, "content");
        copy(config, content, "imageUrl");
        copy(config, content, "clickUrl");
        copy(config, content, "fromName");
        copy(config, content, "fromEmail");
        return content;
    }

    /**
     * 执行 variables 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 按业务键组织的映射结果
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> variables(Map<String, Object> config, ExecutionContext ctx) {
        Object raw = config.getOrDefault("variables", config.get("variablesMapping"));
        if (!(raw instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        ((Map<Object, Object>) map).forEach((key, value) -> {
            if (key == null) return;
            // 变量值支持 $field / $.field 从上下文取值，用于模板个性化渲染。
            variables.put(key.toString(), resolve(value, ctx));
        });
        return variables;
    }

    /**
     * 构建、解析或转换 resolve 相关的业务数据。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param value value 待写入、比较或转换的业务值
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 方法执行后的业务结果
     */
    private Object resolve(Object value, ExecutionContext ctx) {
        if (value instanceof String text && text.startsWith("$")) {
            String field = text.startsWith("$.") ? text.substring(2) : text.substring(1);
            Object contextValue = ctx.getContextValue(field);
            return contextValue == null ? value : contextValue;
        }
        return value;
    }

    /**
     * 执行 output 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param result result 方法执行所需的业务参数
     * @return 按业务键组织的映射结果
     */
    private Map<String, Object> output(ReachDeliveryService.DeliveryResult result) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("sendRecordId", result.recordId());
        output.put("sendStatus", result.sent() ? "SENT" : "FAILED");
        output.put("duplicate", result.duplicate());
        if (result.externalMessageId() != null) {
            output.put("externalMessageId", result.externalMessageId());
        }
        if (result.errorMessage() != null) {
            output.put("errorMessage", result.errorMessage());
        }
        return output;
    }

    /**
     * 执行 copy 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param source source 方法执行所需的业务参数
     * @param target target 方法执行所需的业务参数
     * @param key key 对应的缓存键、配置键或业务键
     */
    private void copy(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
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
