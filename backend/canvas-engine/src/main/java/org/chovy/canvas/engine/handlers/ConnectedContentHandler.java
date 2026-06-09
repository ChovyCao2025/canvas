package org.chovy.canvas.engine.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.OutboundUrlValidator;
import org.chovy.canvas.domain.canvas.ConnectedContentGateway;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeOutcome;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * ConnectedContentHandler 参与 engine.handlers 场景的画布执行引擎处理。
 */
@Component
@NodeHandlerType("CONNECTED_CONTENT")
public class ConnectedContentHandler implements NodeHandler {

    private static final String URL = "url";
    private static final String METHOD = "method";
    private static final String HEADERS = "headers";
    private static final String REQUEST_BODY = "requestBody";
    private static final String CACHE_TTL_SECONDS = "cacheTtlSeconds";
    private static final String TIMEOUT_MS = "timeoutMs";
    private static final String MAX_BYTES = "maxBytes";
    private static final String JSON_PATH_MAPPINGS = "jsonPathMappings";
    private static final String CONNECTED_CONTENT_STATUS = "connectedContentStatus";
    private static final String CONNECTED_CONTENT_CACHE_HIT = "connectedContentCacheHit";
    private static final String CONNECTED_CONTENT_BODY = "connectedContentBody";
    private static final String CONNECTED_CONTENT_ERROR = "connectedContentError";
    private static final int DEFAULT_CACHE_TTL_SECONDS = 300;
    private static final int DEFAULT_TIMEOUT_MS = 2_000;
    private static final int DEFAULT_MAX_BYTES = 65_536;
    private static final int MAX_TIMEOUT_MS = 10_000;
    private static final int MAX_ALLOWED_BYTES = 262_144;

    private final ConnectedContentGateway gateway;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 创建 ConnectedContentHandler 实例并注入 engine.handlers 场景依赖。
     * @param gateway gateway 参数，用于 ConnectedContentHandler 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public ConnectedContentHandler(ConnectedContentGateway gateway, ObjectMapper objectMapper) {
        this(gateway, objectMapper, Clock.systemDefaultZone());
    }

    /**
     * 使用可替换时钟创建 Connected Content 处理器。
     *
     * @param gateway Connected Content 网关
     * @param objectMapper JSON 解析器
     * @param clock 当前时间来源，测试可固定
     */
    ConnectedContentHandler(ConnectedContentGateway gateway, ObjectMapper objectMapper, Clock clock) {
        this.gateway = gateway;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * 执行 Connected Content 节点。
     *
     * <p>方法先校验并构造外部 HTTP 请求；配置缓存 TTL 时优先读取已缓存内容，未命中才调用外部内容源并在成功后写入缓存。
     * 成功结果会把内容体、缓存命中标记和 JSON 映射字段写入节点输出；失败或超时会按节点失败语义返回。
     *
     * @param config 当前节点配置，包含 URL、方法、请求体、缓存 TTL、超时和字段映射
     * @param ctx 画布执行上下文，提供租户、用户和上游输出用于模板化请求
     * @return 异步节点结果，成功时按配置下一跳继续，失败时返回错误输出
     */
    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        PreparedRequest prepared;
        try {
            prepared = prepare(config, ctx);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IllegalArgumentException ex) {
            return Mono.just(failed(config, "CONNECTED_CONTENT_INVALID_CONFIG", ex.getMessage(), null));
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (prepared.cacheTtlSeconds() > 0) {
            Optional<ConnectedContentGateway.CachedContent> cached = gateway.findFresh(
                    prepared.tenantId(), prepared.cacheKey(), now);
            if (cached.isPresent()) {
                return Mono.just(success(config, prepared, cached.get().body(), true));
            }
        }

        return gateway.fetch(prepared.httpRequest())
                .timeout(Duration.ofMillis(prepared.timeoutMs()))
                .flatMap(body -> {
                    if (byteSize(body) > prepared.maxBytes()) {
                        return Mono.just(failed(config, "CONNECTED_CONTENT_PAYLOAD_TOO_LARGE",
                                "connected content payload exceeds " + prepared.maxBytes() + " bytes", body));
                    }
                    if (prepared.cacheTtlSeconds() > 0) {
                        gateway.save(prepared.tenantId(), prepared.cacheKey(), prepared.urlHash(), prepared.requestHash(),
                                body, now.plusSeconds(prepared.cacheTtlSeconds()));
                    }
                    return Mono.just(success(config, prepared, body, false));
                })
                .onErrorResume(java.util.concurrent.TimeoutException.class,
                        ex -> Mono.just(timeout(config, "connected content request timed out")))
                .onErrorResume(ex -> Mono.just(failed(config, "CONNECTED_CONTENT_PROVIDER_ERROR", ex.getMessage(), null)));
    }

    /**
     * 准备 connected content 请求和缓存键。
     *
     * @param config 节点配置
     * @param ctx 执行上下文
     * @return 标准化请求描述
     */
    private PreparedRequest prepare(Map<String, Object> config, ExecutionContext ctx) {
        // 准备本次处理所需的上下文和中间变量。
        String url = string(config.get(URL), null);
        OutboundUrlValidator.validateHttpUrl(url);
        String method = string(config.get(METHOD), "GET").toUpperCase(Locale.ROOT);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!"GET".equals(method) && !"POST".equals(method)) {
            throw new IllegalArgumentException("CONNECTED_CONTENT method only supports GET or POST");
        }
        Map<String, String> headers = stringMap(config.get(HEADERS));
        String requestBody = requestBody(config.get(REQUEST_BODY));
        int timeoutMs = boundedInt(config.get(TIMEOUT_MS), DEFAULT_TIMEOUT_MS, 1, MAX_TIMEOUT_MS);
        int maxBytes = boundedInt(config.get(MAX_BYTES), DEFAULT_MAX_BYTES, 1, MAX_ALLOWED_BYTES);
        int cacheTtlSeconds = boundedInt(config.get(CACHE_TTL_SECONDS), DEFAULT_CACHE_TTL_SECONDS, 0, 86_400);
        Long tenantId = ctx == null || ctx.getTenantId() == null ? 0L : ctx.getTenantId();
        String requestHash = sha256(method + "\n" + url + "\n" + headers + "\n" + requestBody);
        String urlHash = sha256(url);
        String cacheKey = "cc:" + tenantId + ":" + requestHash.substring(0, 40);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new PreparedRequest(
                tenantId,
                cacheKey,
                urlHash,
                requestHash,
                timeoutMs,
                maxBytes,
                cacheTtlSeconds,
                new ConnectedContentGateway.HttpRequest(url, method, headers, requestBody, timeoutMs, maxBytes)
        );
    }

    /**
     * 构造 connected content 成功结果。
     *
     * @param config 节点配置
     * @param prepared 已准备请求
     * @param body 响应体
     * @param cacheHit 是否命中缓存
     * @return 节点成功结果
     */
    private NodeResult success(Map<String, Object> config, PreparedRequest prepared, String body, boolean cacheHit) {
        Map<String, Object> output = output("SUCCESS", cacheHit, parseBody(body), null, config);
        return NodeResult.ok(string(config.get(MapFieldKeys.NEXT_NODE_ID), null), output);
    }

    /**
     * 构造 connected content 超时结果。
     *
     * @param config 节点配置
     * @param message 超时消息
     * @return 路由到 timeout 分支的节点结果
     */
    private NodeResult timeout(Map<String, Object> config, String message) {
        Map<String, Object> output = output("TIMEOUT", false, null, message, config);
        return routed("timeout", string(config.get(MapFieldKeys.TIMEOUT_NODE_ID), null), output,
                NodeOutcome.TIMEOUT, "CONNECTED_CONTENT_TIMEOUT", message);
    }

    /**
     * 构造 connected content 失败结果。
     *
     * @param config 节点配置
     * @param code 错误码
     * @param message 错误消息
     * @param body 响应体，可为空
     * @return 节点失败或 fail 分支结果
     */
    private NodeResult failed(Map<String, Object> config, String code, String message, String body) {
        Map<String, Object> output = output(code, false, body == null ? null : parseBody(body), message, config);
        String failNodeId = string(config.get(MapFieldKeys.FAIL_NODE_ID), null);
        if (failNodeId == null) {
            return NodeResult.fail(message);
        }
        return routed("fail", failNodeId, output, NodeOutcome.FAIL, code, message);
    }

    /**
     * 构造带显式路由的节点结果。
     *
     * @param routeHandle 路由句柄
     * @param nodeId 目标节点 ID
     * @param output 节点输出
     * @param outcome 节点结果类型
     * @param reasonCode 原因码
     * @param reasonMessage 原因说明
     * @return 路由节点结果
     */
    private NodeResult routed(String routeHandle,
                              String nodeId,
                              Map<String, Object> output,
                              NodeOutcome outcome,
                              String reasonCode,
                              String reasonMessage) {
        Map<String, String> routes = nodeId == null || nodeId.isBlank() ? Map.of() : Map.of(routeHandle, nodeId);
        return new NodeResult(null, null, null, null, null, output, true, null, false,
                outcome, routes, reasonCode, reasonMessage, null);
    }

    /**
     * 构造 connected content 输出字段。
     *
     * @param status 状态
     * @param cacheHit 是否命中缓存
     * @param body 响应体解析结果
     * @param error 错误信息
     * @param config 节点配置
     * @return 节点输出字段
     */
    private Map<String, Object> output(String status,
                                       boolean cacheHit,
                                       Object body,
                                       String error,
                                       Map<String, Object> config) {
        Map<String, Object> output = new LinkedHashMap<>();
        String prefix = string(config.get(MapFieldKeys.OUTPUT_PREFIX), "");
        output.put(key(prefix, CONNECTED_CONTENT_STATUS), status);
        output.put(key(prefix, CONNECTED_CONTENT_CACHE_HIT), cacheHit);
        if (body != null) {
            output.put(key(prefix, CONNECTED_CONTENT_BODY), body);
            extractMappings(body, config.get(JSON_PATH_MAPPINGS), prefix, output);
        }
        if (error != null) {
            output.put(key(prefix, CONNECTED_CONTENT_ERROR), error);
        }
        return output;
    }

    /**
     * 根据 JSON path 映射提取响应字段。
     *
     * @param body 响应体对象
     * @param mappings 映射配置
     * @param prefix 输出前缀
     * @param output 输出 Map
     */
    @SuppressWarnings("unchecked")
    private void extractMappings(Object body, Object mappings, String prefix, Map<String, Object> output) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (mappings instanceof List<?> list) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    String path = string(map.get("path") == null ? map.get("jsonPath") : map.get("path"), null);
                    String outputKey = string(map.get("outputKey") == null ? map.get("fieldKey") : map.get("outputKey"), null);
                    if (outputKey == null) {
                        outputKey = string(map.get("key"), null);
                    }
                    putMappedValue(body, path, outputKey, prefix, output);
                }
            }
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        if (mappings instanceof Map<?, ?> map) {
            map.forEach((path, outputKey) ->
                    putMappedValue(body, string(path, null), string(outputKey, null), prefix, output));
        }
    }

    /**
     * 读取并写入单个映射字段。
     *
     * @param body 响应体对象
     * @param path JSON path
     * @param outputKey 输出 key
     * @param prefix 输出前缀
     * @param output 输出 Map
     */
    private void putMappedValue(Object body, String path, String outputKey, String prefix, Map<String, Object> output) {
        if (path == null || outputKey == null || outputKey.isBlank()) {
            return;
        }
        Object value = readSimpleJsonPath(body, path);
        if (value != null) {
            output.put(key(prefix, outputKey), value);
        }
    }

    /**
     * 读取简单的 $.a.b 形式 JSON path。
     *
     * @param body 响应体对象
     * @param path JSON path
     * @return 命中的值，未命中时返回 null
     */
    @SuppressWarnings("unchecked")
    private Object readSimpleJsonPath(Object body, String path) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (body == null || path == null || !path.startsWith("$.")) {
            return null;
        }
        Object current = body;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String segment : path.substring(2).split("\\.")) {
            if (segment.isBlank()) {
                return null;
            }
            if (current instanceof Map<?, ?> map) {
                current = ((Map<String, Object>) map).get(segment);
            } else {
                return null;
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return current;
    }

    /**
     * 解析响应体，JSON 解析失败时保留原始字符串。
     *
     * @param body 响应体字符串
     * @return JSON 对象或原始字符串
     */
    private Object parseBody(String body) {
        if (body == null || body.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(body, Object.class);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ignored) {
            return body;
        }
    }

    /**
     * 将配置值转换为字符串 Map。
     *
     * @param value 原始配置值
     * @return 字符串 Map
     */
    private Map<String, String> stringMap(Object value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        map.forEach((key, val) -> {
            String name = string(key, null);
            if (name != null && !name.isBlank() && val != null) {
                result.put(name, String.valueOf(val));
            }
        });
        // 汇总前面计算出的状态和明细，返回给调用方。
        return result;
    }

    /**
     * 将请求体配置转换为字符串。
     *
     * @param value 原始请求体配置
     * @return 请求体字符串
     */
    private String requestBody(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writeValueAsString(value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }

    /**
     * 解析有上下界的整数配置。
     *
     * @param value 原始配置值
     * @param defaultValue 默认值
     * @param min 最小值
     * @param max 最大值
     * @return 约束后的整数
     */
    private int boundedInt(Object value, int defaultValue, int min, int max) {
        int parsed = defaultValue;
        if (value instanceof Number number) {
            parsed = number.intValue();
        // 根据前序判断结果进入后续条件分支。
        } else if (value instanceof String text && !text.isBlank()) {
            parsed = Integer.parseInt(text.trim());
        }
        return Math.max(min, Math.min(max, parsed));
    }

    /**
     * 生成带输出前缀的 key。
     *
     * @param prefix 输出前缀
     * @param key 字段 key
     * @return 带前缀的字段 key
     */
    private static String key(String prefix, String key) {
        return prefix == null || prefix.isBlank() ? key : prefix + "." + key;
    }

    /**
     * 将对象转换为非空字符串。
     *
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 字符串值或默认值
     */
    private static String string(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? defaultValue : text;
    }

    /**
     * 计算字符串 UTF-8 字节长度。
     *
     * @param value 原始字符串
     * @return 字节长度
     */
    private static int byteSize(String value) {
        return value == null ? 0 : value.getBytes(StandardCharsets.UTF_8).length;
    }

    /**
     * 计算 SHA-256 摘要。
     *
     * @param value 原始字符串
     * @return 十六进制摘要
     */
    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    /**
     * connected content 请求准备结果。
     *
     * @param tenantId 租户 ID
     * @param cacheKey 缓存 key
     * @param urlHash URL 摘要
     * @param requestHash 请求摘要
     * @param timeoutMs 超时时间
     * @param maxBytes 最大响应字节数
     * @param cacheTtlSeconds 缓存 TTL 秒数
     * @param httpRequest HTTP 请求描述
     */
    private record PreparedRequest(Long tenantId,
                                   String cacheKey,
                                   String urlHash,
                                   String requestHash,
                                   int timeoutMs,
                                   int maxBytes,
                                   int cacheTtlSeconds,
                                   ConnectedContentGateway.HttpRequest httpRequest) {
    }
}
