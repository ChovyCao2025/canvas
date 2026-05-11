package org.chovy.canvas.infra.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.chovy.canvas.domain.canvas.CanvasVersionMapper;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 三级画布配置缓存：L1 Caffeine → L2 Redis → L3 MySQL（设计文档 12.7节）。
 *
 * Key = "canvas:{canvasId}:v{versionId}:config"
 * TTL  = 24h（L2），L1 永久驻留直到主动失效
 *
 * 缓存失效广播（12.7节）：
 *   发布时调用 invalidate() → 删 L1 & L2 → Redis Pub/Sub 广播失效 key
 *   所有实例订阅 canvas:cache:invalidate 频道 → 删各自 L1 Caffeine
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CanvasConfigCache {

    private final StringRedisTemplate             redis;
    private final ReactiveStringRedisTemplate     reactiveRedis;
    private final ReactiveRedisConnectionFactory  reactiveFactory;
    private final CanvasVersionMapper             canvasVersionMapper;
    private final DagParser                       dagParser;

    /** L1: JVM 本地缓存，最多 500 条，永不过期（依赖主动失效） */
    private Cache<String, DagGraph> l1;

    static final String INVALIDATE_CHANNEL = "canvas:cache:invalidate";
    private static final Duration L2_TTL = Duration.ofHours(24);

    @PostConstruct
    void init() {
        l1 = Caffeine.newBuilder().maximumSize(500).build();
        // 订阅 Redis Pub/Sub 失效广播（12.7节）
        subscribeInvalidation();
    }

    /** 订阅失效广播，收到后清除本实例 L1 */
    private void subscribeInvalidation() {
        try {
            ReactiveRedisMessageListenerContainer container =
                    new ReactiveRedisMessageListenerContainer(reactiveFactory);
            container.receive(ChannelTopic.of(INVALIDATE_CHANNEL))
                    .map(msg -> msg.getMessage())
                    .doOnNext(l1Key -> {
                        l1.invalidate(l1Key);
                        log.debug("[CACHE] L1 失效广播收到 key={}", l1Key);
                    })
                    .doOnError(e -> log.error("[CACHE] Pub/Sub 订阅错误: {}", e.getMessage()))
                    .subscribe();
        } catch (Exception e) {
            log.warn("[CACHE] Pub/Sub 订阅失败（Redis 未连接时忽略）: {}", e.getMessage());
        }
    }

    public DagGraph get(Long canvasId, Long versionId) {
        String l1Key = canvasId + ":v" + versionId;

        // L1 命中
        DagGraph cached = l1.getIfPresent(l1Key);
        if (cached != null) return cached;

        // L2 Redis
        String redisKey = "canvas:" + canvasId + ":v" + versionId + ":config";
        String json = redis.opsForValue().get(redisKey);
        if (json != null) {
            DagGraph graph = dagParser.parse(json);
            l1.put(l1Key, graph);
            return graph;
        }

        // L3 MySQL
        var version = canvasVersionMapper.selectById(versionId);
        if (version == null) throw new IllegalArgumentException("版本不存在: " + versionId);

        DagGraph graph = dagParser.parse(version.getGraphJson());
        redis.opsForValue().set(redisKey, version.getGraphJson(), L2_TTL);
        l1.put(l1Key, graph);
        log.debug("[CACHE] MISS → MySQL canvasId={} versionId={}", canvasId, versionId);
        return graph;
    }

    /**
     * 失效指定版本缓存，并通过 Pub/Sub 广播给所有实例（12.7节）。
     * 发布新版本时调用。
     */
    public void invalidate(Long canvasId, Long versionId) {
        String l1Key   = canvasId + ":v" + versionId;
        String redisKey = "canvas:" + canvasId + ":v" + versionId + ":config";

        l1.invalidate(l1Key);
        redis.delete(redisKey);

        // 广播失效通知给其他实例的 L1 Caffeine
        reactiveRedis.convertAndSend(INVALIDATE_CHANNEL, l1Key)
                .subscribe(
                        count -> log.debug("[CACHE] 失效广播已发送 key={} 收到实例数={}", l1Key, count),
                        e     -> log.warn("[CACHE] 失效广播发送失败: {}", e.getMessage())
                );
    }

    /** 供测试和外部调用直接清除 L1（不广播） */
    public void evictL1(String l1Key) {
        l1.invalidate(l1Key);
    }
}
