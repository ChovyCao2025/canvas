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

    private final ReachDeliveryService deliveryService;

    AbstractSendMessageHandler(ReachDeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    protected abstract String channel();

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String nodeId = string(config, "__nodeId", channel().toLowerCase() + "-send");
        String templateId = string(config, "templateId", string(config, "template_id", null));
        String successNodeId = string(config, "successNodeId", string(config, "nextNodeId", null));
        String failNodeId = string(config, "failNodeId", null);
        String idempotencyKey = string(config, "idempotencyKey",
                ctx.getExecutionId() + ":" + nodeId + ":" + channel());

        ReachDeliveryService.DeliveryRequest request = deliveryService.request(
                ctx.getExecutionId(),
                ctx.getCanvasId(),
                ctx.getUserId(),
                nodeId,
                channel(),
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
                        return NodeResult.routed("fail", failNodeId, output);
                    }
                    return NodeResult.fail("触达发送失败: " + result.errorMessage());
                });
    }

    @Override
    public boolean isReachNode() {
        return true;
    }

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

    @SuppressWarnings("unchecked")
    private Map<String, Object> variables(Map<String, Object> config, ExecutionContext ctx) {
        Object raw = config.getOrDefault("variables", config.get("variablesMapping"));
        if (!(raw instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        ((Map<Object, Object>) map).forEach((key, value) -> {
            if (key == null) return;
            variables.put(key.toString(), resolve(value, ctx));
        });
        return variables;
    }

    private Object resolve(Object value, ExecutionContext ctx) {
        if (value instanceof String text && text.startsWith("$")) {
            String field = text.startsWith("$.") ? text.substring(2) : text.substring(1);
            Object contextValue = ctx.getContextValue(field);
            return contextValue == null ? value : contextValue;
        }
        return value;
    }

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

    private void copy(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }

    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }
}
