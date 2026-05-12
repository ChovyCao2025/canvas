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
import java.util.Map;

/** Tagger 离线标签：响应式 HTTP，无 block() */
@Slf4j @Component @NodeHandlerType("TAGGER_OFFLINE")
public class TaggerOfflineHandler implements NodeHandler {

    private final WebClient webClient;
    public TaggerOfflineHandler(@Value("${canvas.integration.tagger-service-url}") String url) {
        this.webClient = WebClient.builder().baseUrl(url).build();
    }

    @Override @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String tagCodeKey   = (String) config.get("tagCodeKey");
        Map<String, Object> params = (Map<String, Object>) config.getOrDefault("params", Map.of());
        String nextNodeId   = (String) config.get("nextNodeId");
        String expectedVal  = params.get("tagValue") != null ? params.get("tagValue").toString() : null;

        return webClient.get()
                .uri(u -> u.path("/offline").queryParam("tagCode", tagCodeKey).queryParam("userId", ctx.getUserId()).build())
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(resp -> {
                    Object tagValue = resp.get("tagValue");
                    if (tagValue == null || tagValue.toString().isBlank())
                        return Mono.just(NodeResult.fail("Tagger 离线标签为空，拦截流程"));
                    if (expectedVal != null && !expectedVal.equals(tagValue.toString()))
                        return Mono.just(NodeResult.fail("Tagger 离线标签值不匹配"));
                    return Mono.just(NodeResult.ok(nextNodeId, Map.of("tagValue", tagValue)));
                })
                .onErrorResume(e -> {
                    log.warn("[TAGGER_OFFLINE] 调用失败: {}", e.getMessage());
                    return Mono.just(NodeResult.fail("Tagger 查询异常: " + e.getMessage()));
                });
    }
}
