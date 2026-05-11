package com.photon.canvas.engine.handlers;

import com.photon.canvas.engine.context.ExecutionContext;
import com.photon.canvas.engine.handler.NodeHandler;
import com.photon.canvas.engine.handler.NodeHandlerType;
import com.photon.canvas.engine.handler.NodeResult;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/** 端内通知（MQTT 推送）— mock 直接返回成功，设 userReached */
@Slf4j
@NodeHandlerType("IN_APP_NOTIFY")
public class InAppNotifyHandler implements NodeHandler {

    @Override
    @SuppressWarnings("unchecked")
    public NodeResult execute(Map<String, Object> config, ExecutionContext ctx) {
        String messageCodeKey = (String) config.get("messageCodeKey");
        String nextNodeId     = (String) config.get("nextNodeId");
        List<Map<String, Object>> bizData = (List<Map<String, Object>>) config.getOrDefault("bizData", List.of());

        log.info("[IN_APP_NOTIFY] 推送端内通知 messageCode={} userId={}", messageCodeKey, ctx.getUserId());
        // TODO: 接入 MQTT 推送客户端
        return NodeResult.ok(nextNodeId, Map.of());
    }

    @Override public boolean isReachNode() { return true; }
}
