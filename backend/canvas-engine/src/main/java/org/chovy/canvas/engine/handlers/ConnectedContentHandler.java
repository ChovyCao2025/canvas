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

    @Autowired
    public ConnectedContentHandler(ConnectedContentGateway gateway, ObjectMapper objectMapper) {
        this(gateway, objectMapper, Clock.systemDefaultZone());
    }

    ConnectedContentHandler(ConnectedContentGateway gateway, ObjectMapper objectMapper, Clock clock) {
        this.gateway = gateway;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        PreparedRequest prepared;
        try {
            prepared = prepare(config, ctx);
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

    private PreparedRequest prepare(Map<String, Object> config, ExecutionContext ctx) {
        String url = string(config.get(URL), null);
        OutboundUrlValidator.validateHttpUrl(url);
        String method = string(config.get(METHOD), "GET").toUpperCase(Locale.ROOT);
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

    private NodeResult success(Map<String, Object> config, PreparedRequest prepared, String body, boolean cacheHit) {
        Map<String, Object> output = output("SUCCESS", cacheHit, parseBody(body), null, config);
        return NodeResult.ok(string(config.get(MapFieldKeys.NEXT_NODE_ID), null), output);
    }

    private NodeResult timeout(Map<String, Object> config, String message) {
        Map<String, Object> output = output("TIMEOUT", false, null, message, config);
        return routed("timeout", string(config.get(MapFieldKeys.TIMEOUT_NODE_ID), null), output,
                NodeOutcome.TIMEOUT, "CONNECTED_CONTENT_TIMEOUT", message);
    }

    private NodeResult failed(Map<String, Object> config, String code, String message, String body) {
        Map<String, Object> output = output(code, false, body == null ? null : parseBody(body), message, config);
        String failNodeId = string(config.get(MapFieldKeys.FAIL_NODE_ID), null);
        if (failNodeId == null) {
            return NodeResult.fail(message);
        }
        return routed("fail", failNodeId, output, NodeOutcome.FAIL, code, message);
    }

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

    @SuppressWarnings("unchecked")
    private void extractMappings(Object body, Object mappings, String prefix, Map<String, Object> output) {
        if (mappings instanceof List<?> list) {
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
            return;
        }
        if (mappings instanceof Map<?, ?> map) {
            map.forEach((path, outputKey) ->
                    putMappedValue(body, string(path, null), string(outputKey, null), prefix, output));
        }
    }

    private void putMappedValue(Object body, String path, String outputKey, String prefix, Map<String, Object> output) {
        if (path == null || outputKey == null || outputKey.isBlank()) {
            return;
        }
        Object value = readSimpleJsonPath(body, path);
        if (value != null) {
            output.put(key(prefix, outputKey), value);
        }
    }

    @SuppressWarnings("unchecked")
    private Object readSimpleJsonPath(Object body, String path) {
        if (body == null || path == null || !path.startsWith("$.")) {
            return null;
        }
        Object current = body;
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
        return current;
    }

    private Object parseBody(String body) {
        if (body == null || body.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(body, Object.class);
        } catch (Exception ignored) {
            return body;
        }
    }

    private Map<String, String> stringMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        map.forEach((key, val) -> {
            String name = string(key, null);
            if (name != null && !name.isBlank() && val != null) {
                result.put(name, String.valueOf(val));
            }
        });
        return result;
    }

    private String requestBody(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }

    private int boundedInt(Object value, int defaultValue, int min, int max) {
        int parsed = defaultValue;
        if (value instanceof Number number) {
            parsed = number.intValue();
        } else if (value instanceof String text && !text.isBlank()) {
            parsed = Integer.parseInt(text.trim());
        }
        return Math.max(min, Math.min(max, parsed));
    }

    private static String key(String prefix, String key) {
        return prefix == null || prefix.isBlank() ? key : prefix + "." + key;
    }

    private static String string(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? defaultValue : text;
    }

    private static int byteSize(String value) {
        return value == null ? 0 : value.getBytes(StandardCharsets.UTF_8).length;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

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
