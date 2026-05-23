package org.chovy.canvas.infra.redis;

import lombok.RequiredArgsConstructor;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 画布触发路由表（Redis Set）。
 * 所有 key 通过 RedisKeyUtil 构造，支持命名空间前缀。
 */
@Service
@RequiredArgsConstructor
public class TriggerRouteService {

    private final StringRedisTemplate redis;
    private final RedisKeyUtil keys;
    private final ReactiveRedisConnectionFactory reactiveFactory;
    private final Cache<String, Set<String>> mqRouteCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofSeconds(30))
            .build();

    public void registerMq(Long canvasId, String topicKey) {
        redis.opsForSet().add(keys.triggerMq(topicKey), String.valueOf(canvasId));
        mqRouteCache.invalidate(topicKey);
    }
    public void registerBehavior(Long canvasId, String eventCode) {
        redis.opsForSet().add(keys.triggerBehavior(eventCode), String.valueOf(canvasId));
    }
    public void registerTagger(Long canvasId, String tagCodeKey) {
        redis.opsForSet().add(keys.triggerTagger(tagCodeKey), String.valueOf(canvasId));
    }
    public void removeMq(Long canvasId, String topicKey) {
        redis.opsForSet().remove(keys.triggerMq(topicKey), String.valueOf(canvasId));
        mqRouteCache.invalidate(topicKey);
    }
    public void removeBehavior(Long canvasId, String eventCode) {
        redis.opsForSet().remove(keys.triggerBehavior(eventCode), String.valueOf(canvasId));
    }
    public void removeTagger(Long canvasId, String tagCodeKey) {
        redis.opsForSet().remove(keys.triggerTagger(tagCodeKey), String.valueOf(canvasId));
    }
    public Set<String> getCanvasByMqTopic(String topicKey) {
        return mqRouteCache.get(topicKey, this::loadMqRoute);
    }
    public Set<String> getCanvasByBehavior(String eventCode) {
        Set<String> ids = redis.opsForSet().members(keys.triggerBehavior(eventCode));
        return ids != null ? ids : Set.of();
    }
    public Set<String> getCanvasByTagger(String tagCodeKey) {
        Set<String> ids = redis.opsForSet().members(keys.triggerTagger(tagCodeKey));
        return ids != null ? ids : Set.of();
    }

    public void clearMqRoutes() {
        replaceMqRoutes(Map.of());
    }

    public void replaceMqRoutes(Map<String, Set<String>> routes) {
        Map<String, Set<String>> snapshot = routes.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank())
                .map(entry -> Map.entry(entry.getKey(), sanitizeCanvasIds(entry.getValue())))
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
        List<String> oldKeys = scanMqRouteKeys();

        mqRouteCache.invalidateAll();
        redis.execute(new SessionCallback<List<Object>>() {
            @Override
            @SuppressWarnings("unchecked")
            public <K, V> List<Object> execute(RedisOperations<K, V> operations) {
                operations.multi();
                if (!oldKeys.isEmpty()) {
                    operations.delete((Collection<K>) oldKeys);
                }
                snapshot.forEach((topicKey, canvasIds) ->
                        operations.opsForSet().add(
                                (K) keys.triggerMq(topicKey),
                                (V[]) canvasIds.toArray(String[]::new)));
                return operations.exec();
            }
        });
        mqRouteCache.invalidateAll();
    }

    /**
     * 检查路由表是否为空（用 SCAN，不用 KEYS，设计文档 6.4节）。
     * blockFirst() 在 @PostConstruct 非 reactive 上下文中调用，阻塞安全。
     */
    public boolean isRouteTableEmpty() {
        try {
            java.nio.ByteBuffer firstKey = reactiveFactory.getReactiveConnection()
                    .keyCommands()
                    .scan(ScanOptions.scanOptions().match(keys.triggerPattern()).count(1).build())
                    .blockFirst();
            return firstKey == null;
        } catch (Exception e) {
            return true; // Redis 不可用时保守返回 true
        }
    }

    private Set<String> loadMqRoute(String topicKey) {
        Set<String> ids = redis.opsForSet().members(keys.triggerMq(topicKey));
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(new HashSet<>(ids));
    }

    private List<String> scanMqRouteKeys() {
        ScanOptions options = ScanOptions.scanOptions()
                .match(keys.triggerMqPattern())
                .count(1000)
                .build();
        List<String> result = new ArrayList<>(1000);
        try (Cursor<String> cursor = redis.scan(options)) {
            while (cursor.hasNext()) {
                result.add(cursor.next());
            }
        }
        return result;
    }

    private Set<String> sanitizeCanvasIds(Set<String> canvasIds) {
        if (canvasIds == null || canvasIds.isEmpty()) {
            return Set.of();
        }
        return canvasIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> !id.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}
