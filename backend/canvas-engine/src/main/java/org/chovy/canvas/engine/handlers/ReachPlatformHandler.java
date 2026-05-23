package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.*;

/**
 * 触达平台节点（REACH_PLATFORM）。
 *
 * <p>把画布上下文数据映射为触达平台接口参数，调用 `/send` 下发消息。
 * 当前仅关心调用成败，不解析复杂回执结构。
 * 该节点属于“触达类节点”，会参与触达统计。
 */
@Slf4j @Component @NodeHandlerType("REACH_PLATFORM")
public class ReachPlatformHandler implements NodeHandler {

    /** 触达平台 HTTP 客户端。 */
    private final WebClient webClient;
    public ReachPlatformHandler(@Value("${canvas.integration.reach-platform-url}") String url) {
        // baseUrl 由配置注入，便于按环境切换触达网关
        this.webClient = WebClient.builder().baseUrl(url).build();
    }

    @Override @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        // scene 决定触达模板；bizData 决定模板变量
        String serviceSceneKey = (String) config.get("serviceSceneKey");
        List<Map<String, Object>> bizData = (List<Map<String, Object>>) config.getOrDefault("bizData", List.of());
        String nextNodeId = (String) config.get("nextNodeId");

        // 构建请求体：固定带 userId，其余字段由节点配置映射
        Map<String, Object> body = new HashMap<>();
        body.put("serviceSceneKey", serviceSceneKey);
        body.put("userId", ctx.getUserId());
        for (Map<String, Object> item : bizData) {
            String name = (String) item.get("name");
            Object val  = "CONTEXT".equals(item.get("valueType")) ? ctx.getContextValue((String) item.get("value")) : item.get("value");
            if (name != null) body.put(name, val);
        }

        return webClient.post().uri("/send").bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(resp -> {
                    // 这里只记录成功并继续，不把平台回执透传到上下文
                    log.info("[REACH_PLATFORM] 触达推送 scene={} userId={} bizDataSize={}",
                            serviceSceneKey, ctx.getUserId(), bizData.size());
                    return NodeResult.ok(nextNodeId, Map.of());
                })
                .onErrorResume(e -> {
                    log.warn("[REACH_PLATFORM] 推送失败: {}", e.getMessage());
                    return Mono.just(NodeResult.fail("触达平台调用失败: " + e.getMessage()));
                });
    }
    @Override public boolean isReachNode() { return true; }
}
