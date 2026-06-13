package org.chovy.canvas.execution.adapter.external;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

import org.chovy.canvas.execution.application.CanvasTriggerApplicationService.TriggerRoute;
import org.chovy.canvas.execution.application.TriggerRouteStore;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisTriggerRouteAdapter implements TriggerRouteStore {

    private static final String PREFIX = "canvas:execution:trigger-route";

    private final ReactiveStringRedisTemplate redisTemplate;

    public RedisTriggerRouteAdapter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(TriggerRoute route) {
        if (redisTemplate != null) {
            redisTemplate.opsForValue()
                    .set(routeKey(route.tenantId(), route.triggerType(), route.matchKey()), serialize(route))
                    .block();
        }
    }

    @Override
    public void remove(Long tenantId, Long canvasId) {
        if (redisTemplate == null) {
            return;
        }
        routeKeys(routeKey(tenantId, "*", "*")).stream()
                .filter(key -> {
                    String value = routeValue(key);
                    if (value == null || value.isBlank()) {
                        return false;
                    }
                    TriggerRoute route = deserialize(value);
                    return Objects.equals(route.canvasId(), canvasId)
                            && (tenantId == null || Objects.equals(route.tenantId(), tenantId));
                })
                .forEach(key -> redisTemplate.delete(key).block());
    }

    @Override
    public List<TriggerRoute> routes() {
        if (redisTemplate == null) {
            return List.of();
        }
        return routeKeys(routeKey(null, "*", "*")).stream()
                .map(this::routeValue)
                .filter(value -> value != null && !value.isBlank())
                .map(this::deserialize)
                .toList();
    }

    @Override
    public List<TriggerRoute> routesFor(String triggerType, String matchKey) {
        if (redisTemplate == null) {
            return List.of();
        }
        return Stream.concat(
                        routeKeys(routeKey(null, triggerType, matchKey)).stream(),
                        routeKeys(routeKey(null, triggerType, "")).stream())
                .distinct()
                .map(this::routeValue)
                .filter(value -> value != null && !value.isBlank())
                .map(this::deserialize)
                .toList();
    }

    public String routeKey(Long tenantId, String triggerType, String matchKey) {
        String tenantPart = tenantId == null ? "*" : tenantId.toString();
        String typePart = triggerType == null || triggerType.isBlank()
                ? "MANUAL"
                : triggerType.toUpperCase(Locale.ROOT);
        String matchPart = matchKey == null || matchKey.isBlank() ? "_" : matchKey;
        return PREFIX + ":" + tenantPart + ":" + typePart + ":" + matchPart;
    }

    public String serialize(TriggerRoute route) {
        return route.tenantId() + "|" + route.canvasId() + "|" + route.versionId() + "|"
                + escape(route.triggerType()) + "|" + escape(route.matchKey());
    }

    public TriggerRoute deserialize(String serialized) {
        String[] parts = serialized.split("\\|", -1);
        if (parts.length != 5) {
            throw new IllegalArgumentException("invalid trigger route payload");
        }
        return new TriggerRoute(
                Long.parseLong(parts[0]),
                Long.parseLong(parts[1]),
                Long.parseLong(parts[2]),
                unescape(parts[3]),
                unescape(parts[4]));
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("%", "%25").replace("|", "%7C");
    }

    private String unescape(String value) {
        return value == null ? "" : value.replace("%7C", "|").replace("%25", "%");
    }

    private List<String> routeKeys(String pattern) {
        List<String> keys = redisTemplate.keys(pattern)
                .sort()
                .collectList()
                .block();
        return keys == null ? List.of() : keys;
    }

    private String routeValue(String key) {
        return redisTemplate.opsForValue().get(key).block();
    }
}
