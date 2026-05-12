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

/** 接口调用节点：响应式 HTTP，无 block() */
@Slf4j @Component @NodeHandlerType("API_CALL")
public class ApiCallHandler implements NodeHandler {

    private final WebClient webClient;
    public ApiCallHandler(@Value("${canvas.integration.api-call-base-url}") String url) {
        this.webClient = WebClient.builder().baseUrl(url).build();
    }

    @Override @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String bizLineKey  = (String) config.get("bizLineKey");
        String apiKey      = (String) config.get("apiKey");
        List<Map<String, Object>> paramList = (List<Map<String, Object>>) config.getOrDefault("params", List.of());
        Boolean validateResult = (Boolean) config.get("validateResult");
        List<Map<String, Object>> rules = (List<Map<String, Object>>) config.get("validateRules");
        String nextNodeId  = (String) config.get("nextNodeId");

        Map<String, Object> reqParams = new HashMap<>();
        reqParams.put("bizLineKey", bizLineKey);
        reqParams.put("apiKey",     apiKey);
        for (Map<String, Object> p : paramList) {
            String k = (String) p.get("paramKey");
            Object v = "CONTEXT".equals(p.get("valueType")) ? ctx.getContextValue((String) p.get("value")) : p.get("value");
            if (k != null) reqParams.put(k, v);
        }

        return webClient.post().uri("/call").bodyValue(reqParams)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(resp -> {
                    Map<String, Object> out = new HashMap<>(resp);
                    if (Boolean.TRUE.equals(validateResult) && rules != null) {
                        // 临时 ctx 包含接口返回值供规则引用
                        ExecutionContext tmp = new ExecutionContext();
                        tmp.getFlatContext().putAll(ctx.getFlatContext());
                        tmp.getFlatContext().putAll(resp);
                        for (Map<String, Object> rule : rules) {
                            if (!IfConditionHandler.evaluate(rule, tmp))
                                return Mono.just(NodeResult.fail("接口校验规则不通过: " + rule.get("field")));
                        }
                    }
                    return Mono.just(NodeResult.ok(nextNodeId, out));
                })
                .onErrorResume(e -> {
                    log.warn("[API_CALL] 调用失败 api={}: {}", apiKey, e.getMessage());
                    return Mono.just(NodeResult.fail("接口调用异常: " + e.getMessage()));
                });
    }
}
