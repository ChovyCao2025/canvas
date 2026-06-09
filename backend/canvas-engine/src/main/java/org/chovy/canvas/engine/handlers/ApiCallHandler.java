package org.chovy.canvas.engine.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.OutboundUrlValidator;
import org.chovy.canvas.dal.dataobject.ApiDefinitionDO;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.infrastructure.cache.ApiDefinitionCache;
import org.chovy.canvas.infrastructure.redis.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.net.URI;
import java.nio.charset.StandardCharsets;
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

    /** 接口定义缓存，用于按 apiKey 获取启用的接口配置。 */
    private final ApiDefinitionCache apiDefinitionCache;

    /** WebClient 构造器，用于发起节点 HTTP 请求。 */
    private final WebClient.Builder   webClientBuilder;

    /** JSON 序列化器，用于解析接口响应体。 */
    private final ObjectMapper        objectMapper;

    /** 请求载荷构建器，负责组装接口入参与上下文回调参数。 */
    private final ApiCallPayloadBuilder payloadBuilder;

    /** Redis 客户端，用于秒级接口限流计数。 */
    private final StringRedisTemplate redis;

    /** Redis 键生成器，用于生成接口限流键。 */
    private final RedisKeyUtil        keys;

    /**
     * 执行当前节点或服务的核心处理流程。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String apiKey       = (String) config.get(MapFieldKeys.API_KEY);

        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (apiKey == null || apiKey.isBlank()) {
            return Mono.just(NodeResult.fail("API_CALL: apiKey 未配置"));
        }

        return Mono.fromCallable(() -> prepareApiCall(config, ctx, apiKey))
            .subscribeOn(Schedulers.boundedElastic())
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            .flatMap(prepared -> {
                if (prepared.failure() != null) {
                    return Mono.just(prepared.failure());
                }
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                return executePreparedCall(prepared);
            });
    }

    /**
     * 声明 API_CALL 会调用外部 HTTP 接口并可能产生外部副作用，需要节点级幂等保护。
     *
     * <p>返回 {@code true} 后，调度层会在执行前占用幂等记录；重复执行时可复用首次接口响应写入的上下文输出，避免重复
     * POST/PUT 等有副作用请求。
     *
     * @param config 当前接口节点配置，包含 apiKey、入参和幂等键
     * @param ctx 画布执行上下文
     * @return 始终为 {@code true}
     */
    @Override
    public boolean requiresSideEffectIdempotency(Map<String, Object> config, ExecutionContext ctx) {
        return true;
    }

    @SuppressWarnings("unchecked")
    /**
     * 准备 API 调用定义、限流检查和请求体。
     *
     * @param config 节点配置
     * @param ctx 执行上下文
     * @param apiKey 接口业务 key
     * @return 已准备的 API 调用或前置失败结果
     */
    private PreparedApiCall prepareApiCall(Map<String, Object> config, ExecutionContext ctx, String apiKey) {
        String outputPrefix = (String) config.getOrDefault(MapFieldKeys.OUTPUT_PREFIX, "");
        String nextNodeId   = (String) config.get(MapFieldKeys.NEXT_NODE_ID);

        ApiDefinitionDO def = apiDefinitionCache.getEnabled(apiKey);
        if (def == null) {
            return PreparedApiCall.failure(NodeResult.fail("API_CALL: 找不到接口定义 apiKey=" + apiKey));
        }
        if (def.getRateLimitPerSec() != null) {
            // API 定义启用频控时，先在 Redis 秒级窗口扣数，失败直接走节点失败分支。
            if (def.getRateLimitPerSec() <= 0) {
                return PreparedApiCall.failure(NodeResult.fail(
                        "API_CALL: 接口 " + apiKey + " 速率限制配置无效"));
            }
            RateLimitCheck rateLimitCheck;
            String rateLimitKey = keys.apiRateLimit(apiKey, Instant.now().getEpochSecond());
            try {
                rateLimitCheck = checkRateLimit(redis, rateLimitKey, def.getRateLimitPerSec());
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
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
        // 部分外部接口需要完整画布上下文，payloadBuilder 负责按接口定义补充回调和流程信息。
        List<Map<String, Object>> requestBody = payloadBuilder.build(
                reqBody, ctx, currentNodeId, includeContextPayload);

        return new PreparedApiCall(null, apiKey, outputPrefix, nextNodeId, def, requestBody, config);
    }

    /**
     * 执行已准备好的 API 调用。
     *
     * @param prepared 已准备的 API 调用
     * @return 节点执行结果
     */
    private Mono<NodeResult> executePreparedCall(PreparedApiCall prepared) {
        String apiKey = prepared.apiKey();
        String outputPrefix = prepared.outputPrefix();
        String nextNodeId = prepared.nextNodeId();
        ApiDefinitionDO def = prepared.def();
        List<Map<String, Object>> requestBody = prepared.requestBody();
        Map<String, Object> config = prepared.config();

        String method = def.getMethod() == null ? "POST" : def.getMethod().toUpperCase();
        String url    = def.getUrl();
        try {
            // 出站 URL 做白名单格式校验，避免节点配置把后端变成任意地址代理。
            OutboundUrlValidator.validateHttpUrl(url);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IllegalArgumentException e) {
            return Mono.just(NodeResult.fail("API_CALL: " + e.getMessage()));
        }
        log.info("[API_CALL] → apiKey={} method={} url={}", apiKey, method, safeUrlForLog(url));

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
                int responseBytes = body == null ? 0 : body.getBytes(StandardCharsets.UTF_8).length;
                log.info("[API_CALL] ← apiKey={} status=200 bytes={}", apiKey, responseBytes);
                Map<String, Object> out = new HashMap<>();
                // 尝试解 JSON，失败就把原始 body 存入 output
                try {
                    if (body != null && !body.isBlank()) {
                        Map<?, ?> parsed = objectMapper.readValue(body, Map.class);
                        parsed.forEach((k, v) -> out.put(prefix + k, v));
                    }
                // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
                } catch (Exception ignored) {
                    out.put(prefix + MapFieldKeys.BODY, body);
                    out.put(prefix + MapFieldKeys.IS_JSON, false);
                }
                out.put(prefix + MapFieldKeys.HTTP_STATUS, "200");
                if (!validateResponse(config, out)) {
                    // 响应规则不通过时不进入 success 下游，统一交给失败分支处理。
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
    /**
     * 根据节点响应规则校验接口输出。
     *
     * @param config 节点配置
     * @param output 接口输出字段
     * @return true 表示响应满足规则
     */
    private boolean validateResponse(Map<String, Object> config, Map<String, Object> output) {
        if (!Boolean.TRUE.equals(config.get(MapFieldKeys.VALIDATE_RESULT))) {
            return true;
        }
        // validateRules 复用条件求值器，对接口输出字段做后置业务校验。
        List<Map<String, Object>> rules =
                (List<Map<String, Object>>) config.get(MapFieldKeys.VALIDATE_RULES);
        return ConditionEvaluator.allMatch(rules, output);
    }

    /**
     * 判断指定接口在当前秒是否已超过限流。
     *
     * @param redis Redis 模板
     * @param apiKey 接口业务 key
     * @param limitPerSec 每秒限制
     * @param now 当前时间
     * @return true 表示已超过限流或限流检查失败
     */
    static boolean isRateLimitExceeded(StringRedisTemplate redis,
                                       String apiKey, int limitPerSec, Instant now) {
        String key = "canvas:ratelimit:" + apiKey + ":" + now.getEpochSecond();
        RateLimitCheck check = checkRateLimit(redis, key, limitPerSec);
        return check == RateLimitCheck.EXCEEDED || check == RateLimitCheck.CHECK_FAILED;
    }

    /**
     * 执行 Redis 秒级限流计数。
     *
     * @param redis Redis 模板
     * @param key 限流键
     * @param limitPerSec 每秒限制
     * @return 限流检查结果
     */
    private static RateLimitCheck checkRateLimit(StringRedisTemplate redis, String key, int limitPerSec) {
        if (limitPerSec <= 0) {
            return RateLimitCheck.CHECK_FAILED;
        }
        Long count = redis.opsForValue().increment(key);
        if (count == null) {
            return RateLimitCheck.CHECK_FAILED;
        }
        if (count == 1L) {
            // 第一次命中秒级桶时设置短 TTL，避免异常路径留下永久计数键。
            return Boolean.TRUE.equals(redis.expire(key, Duration.ofSeconds(2)))
                    ? RateLimitCheck.ALLOWED
                    : RateLimitCheck.CHECK_FAILED;
        }
        return count > limitPerSec ? RateLimitCheck.EXCEEDED : RateLimitCheck.ALLOWED;
    }

    /**
     * 生成可安全记录到日志的 URL。
     *
     * @param url 原始 URL
     * @return 去掉查询、用户信息和片段后的 URL
     */
    private static String safeUrlForLog(String url) {
        try {
            URI uri = URI.create(url);
            return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(),
                    uri.getPath(), null, null).toString();
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            return "<invalid-url>";
        }
    }

    /**
     * RateLimitCheck 枚举类型。
     */
    private enum RateLimitCheck {
        /** 限流检查通过，可以继续调用接口。 */
        ALLOWED,

        /** 当前秒级限流桶已超出配置阈值。 */
        EXCEEDED,

        /** 限流计数或过期时间设置失败。 */
        CHECK_FAILED
    }

    /**
     * PreparedApiCall record.
     * @param failure 前置校验失败时直接返回的节点结果.
     * @param apiKey 当前调用使用的接口业务键.
     * @param outputPrefix 写入上下文时使用的输出字段前缀.
     * @param nextNodeId 接口调用成功后继续执行的下游节点 ID.
     * @param def 已启用的接口定义配置.
     * @param requestBody 已解析并组装完成的 HTTP 请求体.
     * @param config 当前节点原始配置，用于后置响应校验.
     */
    private record PreparedApiCall(
        NodeResult failure,
        String apiKey,
        String outputPrefix,
        String nextNodeId,
        ApiDefinitionDO def,
        List<Map<String, Object>> requestBody,
        Map<String, Object> config
    ) {
        /**
         * 构造前置校验失败的 API 调用准备结果。
         *
         * @param failure 失败节点结果
         * @return 失败准备结果
         */
        static PreparedApiCall failure(NodeResult failure) {
            return new PreparedApiCall(failure, null, null, null, null, null, Map.of());
        }
    }
}
