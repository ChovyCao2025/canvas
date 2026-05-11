package com.photon.canvas.engine.handlers;

import com.photon.canvas.engine.context.ExecutionContext;
import com.photon.canvas.engine.handler.NodeHandler;
import com.photon.canvas.engine.handler.NodeHandlerType;
import com.photon.canvas.engine.handler.NodeResult;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/** 发送 MQ 消息节点 — mock 直接返回成功 */
@Slf4j
@NodeHandlerType("SEND_MQ")
public class SendMqHandler implements NodeHandler {

    @Override
    @SuppressWarnings("unchecked")
    public NodeResult execute(Map<String, Object> config, ExecutionContext ctx) {
        String messageCodeKey = (String) config.get("messageCodeKey");
        List<Map<String, Object>> params = (List<Map<String, Object>>) config.getOrDefault("params", List.of());
        String nextNodeId = (String) config.get("nextNodeId");

        log.info("[SEND_MQ] 发送 MQ messageCode={} userId={}", messageCodeKey, ctx.getUserId());
        // TODO: 接入 RocketMQ 生产者
        return NodeResult.ok(nextNodeId, Map.of());
    }
}
