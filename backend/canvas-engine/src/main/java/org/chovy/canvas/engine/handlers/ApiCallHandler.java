package org.chovy.canvas.engine.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.domain.meta.ApiDefinition;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.infra.cache.ApiDefinitionCache;
import org.chovy.canvas.infra.redis.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.time.Duration;
import java.time.Instant;
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

    private final ApiDefinitionCache apiDefinitionCache;
    private final WebClient.Builder   webClientBuilder;
    private final ObjectMapper        objectMapper;
    private final ApiCallPayloadBuilder payloadBuilder;
    private final StringRedisTemplate redis;
    private final RedisKeyUtil        keys;

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String apiKey       = (String) config.get(MapFieldKeys.API_KEY);

        if (apiKey == null || apiKey.isBlank()) {
            return Mono.just(NodeResult.fail("API_CALL: apiKey 未配置"));
        }

        return Mono.fromCallable(() -> prepareApiCall(config, ctx, apiKey))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(prepared -> {
                if (prepared.failure() != null) {
                    return Mono.just(prepared.failure());
                }
                return executePreparedCall(prepared);
            });
    }

    @SuppressWarnings("unchecked")
    private PreparedApiCall prepareApiCall(Map<String, Object> config, ExecutionContext ctx, String apiKey) {
        String outputPrefix = (String) config.getOrDefault(MapFieldKeys.OUTPUT_PREFIX, "");
        String nextNodeId   = (String) config.get(MapFieldKeys.NEXT_NODE_ID);

        ApiDefinition def = apiDefinitionCache.getEnabled(apiKey);
        if (def == null) {
            return PreparedApiCall.failure(NodeResult.fail("API_CALL: 找不到接口定义 apiKey=" + apiKey));
        }
        if (def.getRateLimitPerSec() != null) {
            if (def.getRateLimitPerSec() <= 0) {
                return PreparedApiCall.failure(NodeResult.fail(
                        "API_CALL: 接口 " + apiKey + " 速率限制配置无效"));
            }
            RateLimitCheck rateLimitCheck;
            String rateLimitKey = keys.apiRateLimit(apiKey, Instant.now().getEpochSecond());
            try {
                rateLimitCheck = checkRateLimit(redis, rateLimitKey, def.getRateLimitPerSec());
            } catch (Exception e) {
                log.error("[API_CALL] 速率限制检查失败 apiKey={} key={}", apiKey, rateLimitKey, e);
                return PreparedApiCall.failure(NodeResult.fail(
                        "API_CALL: 接口 " + apiKey + " 速率限制检查失败"));
            }
            if (rateLimitCheck == RateLimitCheck.CHECK_FAILED) {
                log.error("[API_CALL] 速率限制检查失败 apiKey={} key={}", apiKey, rateLimitKey);
                return PreparedApiCall.failure(NodeResult.fail(
                        "API_CALL: 接口 " + apiKey + " 速率限制检查失败"));
            }
            if (rateLimitCheck == RateLimitCheck.EXCEEDED) {
                log.warn("[API_CALL] 速率限制 apiKey={} limit={}/s", apiKey, def.getRateLimitPerSec());
                return PreparedApiCall.failure(NodeResult.fail(
                        "API_CALL: 接口 " + apiKey + " 调用已达速率限制（" +
                                def.getRateLimitPerSec() + " req/s）"));
            }
        }

        // 构建请求体，值若为 ${ctxKey} 则从上下文取
        Map<String, Object> inputParams = (Map<String, Object>) config.getOrDefault(MapFieldKeys.INPUT_PARAMS, Map.of());
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
        boolean includeContextPayload = Integer.valueOf(1).equals(def.getIncludeContextPayload());
        String currentNodeId = (String) config.getOrDefault(MapFieldKeys.NODE_ID_INTERNAL, "");
        List<Map<String, Object>> requestBody = payloadBuilder.build(
                reqBody, ctx, currentNodeId, includeContextPayload);

        return new PreparedApiCall(null, apiKey, outputPrefix, nextNodeId, def, requestBody, config);
    }

    private Mono<NodeResult> executePreparedCall(PreparedApiCall prepared) {
        String apiKey = prepared.apiKey();
        String outputPrefix = prepared.outputPrefix();
        String nextNodeId = prepared.nextNodeId();
        ApiDefinition def = prepared.def();
        List<Map<String, Object>> requestBody = prepared.requestBody();
        Map<String, Object> config = prepared.config();

        String method = def.getMethod() == null ? "POST" : def.getMethod().toUpperCase();
        String url    = def.getUrl();
        log.info("[API_CALL] → {} {} body={}", method, url, requestBody);

        // 先读 String，避免因响应 Content-Type 非 JSON 导致解码失败
        Mono<String> rawCall = "GET".equals(method)
            ? webClientBuilder.build().get().uri(url)
                .retrieve().bodyToMono(String.class)
            : webClientBuilder.build().post().uri(url)
                .bodyValue(requestBody)
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
                    out.put(prefix + MapFieldKeys.BODY, body);
                    out.put(prefix + MapFieldKeys.IS_JSON, false);
                }
                out.put(prefix + MapFieldKeys.HTTP_STATUS, "200");
                if (!validateResponse(config, out)) {
                    return Mono.just(NodeResult.fail("API_CALL: 响应校验不通过"));
                }
                return Mono.just(NodeResult.ok(nextNodeId, out));
            })
            .onErrorResume(e -> {
                log.error("[API_CALL] ✗ apiKey={} url={} error={}", apiKey, url, e.getMessage());
                return Mono.just(NodeResult.fail("接口调用异常: " + e.getMessage()));
            });
    }

    @SuppressWarnings("unchecked")
    private boolean validateResponse(Map<String, Object> config, Map<String, Object> output) {
        if (!Boolean.TRUE.equals(config.get(MapFieldKeys.VALIDATE_RESULT))) {
            return true;
        }
        List<Map<String, Object>> rules =
                (List<Map<String, Object>>) config.get(MapFieldKeys.VALIDATE_RULES);
        return ConditionEvaluator.allMatch(rules, output);
    }

    static boolean isRateLimitExceeded(StringRedisTemplate redis,
                                       String apiKey, int limitPerSec, Instant now) {
        String key = "canvas:ratelimit:" + apiKey + ":" + now.getEpochSecond();
        RateLimitCheck check = checkRateLimit(redis, key, limitPerSec);
        return check == RateLimitCheck.EXCEEDED || check == RateLimitCheck.CHECK_FAILED;
    }

    private static RateLimitCheck checkRateLimit(StringRedisTemplate redis, String key, int limitPerSec) {
        if (limitPerSec <= 0) {
            return RateLimitCheck.CHECK_FAILED;
        }
        Long count = redis.opsForValue().increment(key);
        if (count == null) {
            return RateLimitCheck.CHECK_FAILED;
        }
        if (count == 1L) {
            return Boolean.TRUE.equals(redis.expire(key, Duration.ofSeconds(2)))
                    ? RateLimitCheck.ALLOWED
                    : RateLimitCheck.CHECK_FAILED;
        }
        return count > limitPerSec ? RateLimitCheck.EXCEEDED : RateLimitCheck.ALLOWED;
    }

    private enum RateLimitCheck {
        ALLOWED,
        EXCEEDED,
        CHECK_FAILED
    }

    private record PreparedApiCall(
            NodeResult failure,
            String apiKey,
            String outputPrefix,
            String nextNodeId,
            ApiDefinition def,
            List<Map<String, Object>> requestBody,
            Map<String, Object> config
    ) {
        static PreparedApiCall failure(NodeResult failure) {
            return new PreparedApiCall(failure, null, null, null, null, null, Map.of());
        }
    }
}
