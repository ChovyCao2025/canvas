package org.chovy.canvas.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 画布触发路由表（Redis Set）。
 * 所有 key 通过 RedisKeyUtil 构造，支持命名空间前缀。
 *
 * 路由语义：
 * - key: 某个触发条件（topic/event/tag）；
 * - set members: 命中的 canvasId 集合。
 */
@Service
@RequiredArgsConstructor
public class TriggerRouteService {

    /** 阻塞式 Redis 模板，用于 Set 增删查。 */
    private final StringRedisTemplate redis;

    /** Redis key 工具，集中生成业务 key。 */
    private final RedisKeyUtil keys;

    /** 响应式 Redis 连接工厂。 */
    private final ReactiveRedisConnectionFactory reactiveFactory;
    /** MQ 触发路由本地缓存。 */
    private final Cache<String, Set<String>> mqRouteCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofSeconds(30))
            .build();
    /** 路由变更锁 TTL。 */
    private static final Duration ROUTE_MUTATION_LOCK_TTL = Duration.ofSeconds(30);
    /** 等待路由变更锁的最大毫秒数。 */
    private static final long ROUTE_MUTATION_LOCK_WAIT_MS = 5_000L;

    /** 注册 MQ 触发路由：topicKey -> canvasId。 */
    public void registerMq(Long canvasId, String topicKey) {
        withRouteMutationLock(() -> {
            redis.opsForSet().add(keys.triggerMq(topicKey), String.valueOf(canvasId));
            mqRouteCache.invalidate(topicKey);
        });
    }
    /** 注册行为事件触发路由：eventCode -> canvasId。 */
    public void registerBehavior(Long canvasId, String eventCode) {
        redis.opsForSet().add(keys.triggerBehavior(eventCode), String.valueOf(canvasId));
    }
    /** 注册 tagger 实时触发路由：tagCodeKey -> canvasId。 */
    public void registerTagger(Long canvasId, String tagCodeKey) {
        redis.opsForSet().add(keys.triggerTagger(tagCodeKey), String.valueOf(canvasId));
    }
    /** 移除 MQ 触发路由。 */
    public void removeMq(Long canvasId, String topicKey) {
        withRouteMutationLock(() -> {
            redis.opsForSet().remove(keys.triggerMq(topicKey), String.valueOf(canvasId));
            mqRouteCache.invalidate(topicKey);
        });
    }
    /** 移除行为事件触发路由。 */
    public void removeBehavior(Long canvasId, String eventCode) {
        redis.opsForSet().remove(keys.triggerBehavior(eventCode), String.valueOf(canvasId));
    }
    /** 移除 tagger 实时触发路由。 */
    public void removeTagger(Long canvasId, String tagCodeKey) {
        redis.opsForSet().remove(keys.triggerTagger(tagCodeKey), String.valueOf(canvasId));
    }
    /** 按 MQ topicKey 查询订阅画布 ID 集合。 */
    public Set<String> getCanvasByMqTopic(String topicKey) {
        return mqRouteCache.get(topicKey, this::loadMqRoute);
    }
    /** 按事件编码查询订阅画布 ID 集合。 */
    public Set<String> getCanvasByBehavior(String eventCode) {
        Set<String> ids = redis.opsForSet().members(keys.triggerBehavior(eventCode));
        return ids != null ? ids : Set.of();
    }
    /** 按标签 key 查询订阅画布 ID 集合。 */
    public Set<String> getCanvasByTagger(String tagCodeKey) {
        Set<String> ids = redis.opsForSet().members(keys.triggerTagger(tagCodeKey));
        return ids != null ? ids : Set.of();
    }

    /** 清空 MQ 触发路由表。 */
    public void clearMqRoutes() {
        replaceMqRoutes(Map.of());
    }

    /** 批量替换 MQ 触发路由表。 */
    public void replaceMqRoutes(Map<String, Set<String>> routes) {
        Map<String, Set<String>> snapshot = routes.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank())
                .map(entry -> Map.entry(entry.getKey(), sanitizeCanvasIds(entry.getValue())))
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
        withRouteMutationLock(() -> {
            markRouteRebuilding();
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
            markRouteReady();
        });
    }

    /** 判断触发路由表是否处于可用状态。 */
    public boolean isRouteReady() {
        return Boolean.TRUE.equals(redis.hasKey(keys.triggerRouteReady()));
    }

    /** 标记触发路由表已就绪。 */
    public void markRouteReady() {
        redis.opsForValue().set(keys.triggerRouteReady(), "1");
    }

    /** 标记触发路由表正在重建。 */
    public void markRouteRebuilding() {
        redis.delete(keys.triggerRouteReady());
        mqRouteCache.invalidateAll();
    }

    /**
     * 检查路由表是否为空（用 SCAN，不用 KEYS，设计文档 6.4节）。
     * blockFirst() 在 @PostConstruct 非 reactive 上下文中调用，阻塞安全。
     */
    public boolean isRouteTableEmpty() {
        try {
            // 只取第一条命中即可判空，避免全量扫描带来的资源消耗
            java.nio.ByteBuffer firstKey = reactiveFactory.getReactiveConnection()
                    .keyCommands()
                    .scan(ScanOptions.scanOptions().match(keys.triggerPattern()).count(1).build())
                    .blockFirst();
            return firstKey == null;
        } catch (Exception e) {
            // Redis 不可用时保守返回 true，由上层触发重建路由流程
            return true;
        }
    }

    /** 从 Redis 加载 MQ topic 路由并清洗无效画布 ID。 */
    private Set<String> loadMqRoute(String topicKey) {
        Set<String> ids = redis.opsForSet().members(keys.triggerMq(topicKey));
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        return sanitizeCanvasIds(ids);
    }

    /** 使用 Redis 分布式锁串行化 MQ 路由重建和单点增删操作。 */
    private void withRouteMutationLock(Runnable action) {
        String lockKey = keys.triggerRouteMutationLock();
        String token = UUID.randomUUID().toString();
        long deadline = System.currentTimeMillis() + ROUTE_MUTATION_LOCK_WAIT_MS;
        boolean acquired = false;
        while (System.currentTimeMillis() <= deadline) {
            acquired = Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(lockKey, token, ROUTE_MUTATION_LOCK_TTL));
            if (acquired) {
                break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for route mutation lock", e);
            }
        }
        if (!acquired) {
            throw new IllegalStateException("MQ route mutation lock busy");
        }
        try {
            action.run();
        } finally {
            releaseRouteMutationLock(lockKey, token);
        }
    }

    /** 使用 Lua 校验 token 后释放路由变更锁，避免误删其他实例持有的锁。 */
    private void releaseRouteMutationLock(String lockKey, String token) {
        String script = """
                if redis.call('get', KEYS[1]) == ARGV[1] then
                    return redis.call('del', KEYS[1])
                end
                return 0
                """;
        redis.execute(RedisScript.of(script, Long.class), List.of(lockKey), token);
    }

    /** 使用 SCAN 查找当前命名空间下全部 MQ 路由 key。 */
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

    /** 过滤空值、空白值和非正整数，得到可写入 Redis 的画布 ID 集合。 */
    private Set<String> sanitizeCanvasIds(Set<String> canvasIds) {
        if (canvasIds == null || canvasIds.isEmpty()) {
            return Set.of();
        }
        return canvasIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> !id.isBlank())
                .map(String::trim)
                .filter(this::isPositiveLong)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** 判断字符串是否为正整数 ID。 */
    private boolean isPositiveLong(String value) {
        try {
            return Long.parseLong(value) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
