package org.chovy.canvas.engine.handlers;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 *
 * 响应处理：先读 String，再尝试 JSON 解析。
 * 非 JSON 响应（text/html 等）不报错，将原始 body 存入 output.body 继续执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@NodeHandlerType("API_CALL")
public class ApiCallHandler implements NodeHandler {

    private final ApiDefinitionMapper apiDefinitionMapper;
    private final WebClient.Builder   webClientBuilder;
    private final ObjectMapper        objectMapper;

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
            if (val instanceof String s) {
                // 兼容 Flyway 转义后的 $${key} 和标准 ${key} 两种写法
                String norm = s.startsWith("$${") ? s.substring(1) : s;
                if (norm.startsWith("${") && norm.endsWith("}")) {
                    val = ctx.getContextValue(norm.substring(2, norm.length() - 1));
                }
            }
            reqBody.put(entry.getKey(), val);
        }

        String method = def.getMethod() == null ? "POST" : def.getMethod().toUpperCase();
        String url    = def.getUrl();
        log.info("[API_CALL] → {} {} body={}", method, url, reqBody);

        // 先读 String，避免因响应 Content-Type 非 JSON 导致解码失败
        Mono<String> rawCall = "GET".equals(method)
            ? webClientBuilder.build().get().uri(url)
                .retrieve().bodyToMono(String.class)
            : webClientBuilder.build().post().uri(url)
                .bodyValue(reqBody)
                .retrieve().bodyToMono(String.class);

        String prefix = (outputPrefix != null && !outputPrefix.isBlank()) ? outputPrefix + "." : "";

        return rawCall
            .defaultIfEmpty("")
            .flatMap(body -> {
                log.info("[API_CALL] ← apiKey={} body={}", apiKey, body.length() > 200 ? body.substring(0, 200) + "..." : body);
                Map<String, Object> out = new HashMap<>();
                // 尝试解 JSON，失败就把原始 body 存入 output
                try {
                    if (body != null && !body.isBlank()) {
                        Map<?, ?> parsed = objectMapper.readValue(body, Map.class);
                        parsed.forEach((k, v) -> out.put(prefix + k, v));
                    }
                } catch (Exception ignored) {
                    out.put(prefix + "body", body);
                    out.put(prefix + "isJson", false);
                }
                out.put(prefix + "httpStatus", "200");
                return Mono.just(NodeResult.ok(nextNodeId, out));
            })
            .onErrorResume(e -> {
                log.error("[API_CALL] ✗ apiKey={} url={} error={}", apiKey, url, e.getMessage());
                return Mono.just(NodeResult.fail("接口调用异常: " + e.getMessage()));
            });
    }
}
