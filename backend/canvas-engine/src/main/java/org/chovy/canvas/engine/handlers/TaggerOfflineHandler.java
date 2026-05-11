package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/** Tagger 离线标签：HTTP 查询标签，为空则拦截 */
@Slf4j
@NodeHandlerType("TAGGER_OFFLINE")
public class TaggerOfflineHandler implements NodeHandler {

    private final WebClient webClient;

    public TaggerOfflineHandler(@Value("${canvas.integration.tagger-service-url}") String url) {
        this.webClient = WebClient.builder().baseUrl(url).build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public NodeResult execute(Map<String, Object> config, ExecutionContext ctx) {
        String tagCodeKey = (String) config.get("tagCodeKey");
        Map<String, Object> params = (Map<String, Object>) config.getOrDefault("params", Map.of());
        String nextNodeId = (String) config.get("nextNodeId");

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = webClient.get()
                    .uri(u -> u.path("/offline").queryParam("tagCode", tagCodeKey)
                            .queryParam("userId", ctx.getUserId()).build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            Object tagValue = resp != null ? resp.get("tagValue") : null;
            if (tagValue == null || tagValue.toString().isBlank()) {
                return NodeResult.fail("Tagger 离线标签为空，拦截流程");
            }

            String expectedValue = params.get("tagValue") != null ? params.get("tagValue").toString() : null;
            if (expectedValue != null && !expectedValue.equals(tagValue.toString())) {
                return NodeResult.fail("Tagger 离线标签值不匹配");
            }

            return NodeResult.ok(nextNodeId, Map.of("tagValue", tagValue));
        } catch (Exception e) {
            log.warn("[TAGGER_OFFLINE] 调用失败: {}", e.getMessage());
            return NodeResult.fail("Tagger 查询异常: " + e.getMessage());
        }
    }
}
