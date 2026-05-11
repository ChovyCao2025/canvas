package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 触达平台节点：发 MQ 至触达平台，设 userReached */
@Slf4j
@NodeHandlerType("REACH_PLATFORM")
public class ReachPlatformHandler implements NodeHandler {

    private final WebClient webClient;

    public ReachPlatformHandler(@Value("${canvas.integration.reach-platform-url}") String url) {
        this.webClient = WebClient.builder().baseUrl(url).build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public NodeResult execute(Map<String, Object> config, ExecutionContext ctx) {
        String serviceSceneKey = (String) config.get("serviceSceneKey");
        List<Map<String, Object>> bizData = (List<Map<String, Object>>) config.getOrDefault("bizData", List.of());
        String nextNodeId = (String) config.get("nextNodeId");

        Map<String, Object> body = new HashMap<>();
        body.put("serviceSceneKey", serviceSceneKey);
        body.put("userId", ctx.getUserId());
        for (Map<String, Object> item : bizData) {
            String name  = (String) item.get("name");
            Object value = "CONTEXT".equals(item.get("valueType"))
                    ? ctx.getContextValue((String) item.get("value")) : item.get("value");
            if (name != null) body.put(name, value);
        }

        try {
            webClient.post().uri("/send").bodyValue(body).retrieve()
                    .bodyToMono(Map.class).block();
            log.info("[REACH_PLATFORM] 触达推送 scene={} userId={}", serviceSceneKey, ctx.getUserId());
            return NodeResult.ok(nextNodeId, Map.of());
        } catch (Exception e) {
            log.warn("[REACH_PLATFORM] 推送失败: {}", e.getMessage());
            return NodeResult.fail("触达平台调用失败: " + e.getMessage());
        }
    }

    @Override public boolean isReachNode() { return true; }
}
