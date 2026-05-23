package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.springframework.stereotype.Component;
import org.chovy.canvas.engine.handler.NodeResult;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 发送 MQ 消息节点（SEND_MQ）。
 *
 * <p>当前为 mock 实现：仅打印日志并返回成功。
 * 未来接入真实 MQ 生产者时，建议输出 messageId 用于链路追踪。
 * 当前版本不会写入外部消息系统。
 */
@Slf4j
@Component
@NodeHandlerType("SEND_MQ")
public class SendMqHandler implements NodeHandler {

    /**
     * 发送 MQ 节点执行。
     *
     * <p>当前 mock：只记录日志与继续路由，不向外部 MQ 投递。
     */
    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        // messageCodeKey 决定消息模板；params 为消息参数映射
        String messageCodeKey = (String) config.get("messageCodeKey");
        List<Map<String, Object>> params = (List<Map<String, Object>>) config.getOrDefault("params", List.of());
        String nextNodeId = (String) config.get("nextNodeId");

        // 当前版本仅打印日志，不实际发消息。
        // params 暂未发送，仅作为后续对接 MQ producer 的协议预留。
        log.info("[SEND_MQ] 发送 MQ messageCode={} userId={} paramSize={}",
                messageCodeKey, ctx.getUserId(), params.size());
        // TODO: 接入 RocketMQ 生产者
        return Mono.just(NodeResult.ok(nextNodeId, Map.of()));
    }
}
