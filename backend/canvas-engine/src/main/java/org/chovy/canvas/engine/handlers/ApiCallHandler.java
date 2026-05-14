package org.chovy.canvas.engine.handlers;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.domain.meta.ApiDefinition;
import org.chovy.canvas.domain.meta.ApiDefinitionMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.*;

/**
 * 接口调用节点：从 api_definition 表查 URL，直接发起 HTTP 请求。
 * config: apiKey(必填), inputParams(Map<String,String> 支持 ${ctxKey} 引用),
 *         outputPrefix(出参 context key 前缀), nextNodeId
 */
@Slf4j
@Component
@RequiredArgsConstructor
@NodeHandlerType("API_CALL")
public class ApiCallHandler implements NodeHandler {

    private final ApiDefinitionMapper apiDefinitionMapper;
    private final WebClient.Builder webClientBuilder;

    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String apiKey       = (String) config.get("apiKey");
        String outputPrefix = (String) config.getOrDefault("outputPrefix", "");
        String nextNodeId   = (String) config.get("nextNodeId");

        if (apiKey == null || apiKey.isBlank()) {
            return Mono.just(NodeResult.fail("API_CALL: apiKey 未配置"));
        }

        ApiDefinition def = apiDefinitionMapper.selectOne(
            new LambdaQueryWrapper<ApiDefinition>()
                .eq(ApiDefinition::getApiKey, apiKey)
                .eq(ApiDefinition::getEnabled, 1)
        );
        if (def == null) {
            return Mono.just(NodeResult.fail("API_CALL: 找不到接口定义 apiKey=" + apiKey));
        }

        // 构建请求体，值若为 ${ctxKey} 则从上下文取
        Map<String, Object> inputParams = (Map<String, Object>) config.getOrDefault("inputParams", Map.of());
        Map<String, Object> reqBody = new HashMap<>();
        for (Map.Entry<String, Object> entry : inputParams.entrySet()) {
            Object val = entry.getValue();
            if (val instanceof String s && s.startsWith("${") && s.endsWith("}")) {
                val = ctx.getContextValue(s.substring(2, s.length() - 1));
            }
            reqBody.put(entry.getKey(), val);
        }

        String method = def.getMethod() == null ? "POST" : def.getMethod().toUpperCase();
        String url    = def.getUrl();
        log.info("[API_CALL] {} {} inputParams={}", method, url, reqBody);

        Mono<Map> call = "GET".equals(method)
            ? webClientBuilder.build().get().uri(url).retrieve().bodyToMono(Map.class)
            : webClientBuilder.build().post().uri(url).bodyValue(reqBody).retrieve().bodyToMono(Map.class);

        return call
            .flatMap(resp -> {
                String prefix = (outputPrefix != null && !outputPrefix.isBlank()) ? outputPrefix + "." : "";
                Map<String, Object> out = new HashMap<>();
                resp.forEach((k, v) -> out.put(prefix + k, v));
                log.info("[API_CALL] 成功 apiKey={} resp={}", apiKey, resp);
                return Mono.just(NodeResult.ok(nextNodeId, out));
            })
            .onErrorResume(e -> {
                log.warn("[API_CALL] 失败 apiKey={} url={}: {}", apiKey, url, e.getMessage());
                return Mono.just(NodeResult.fail("接口调用异常: " + e.getMessage()));
            });
    }
}
