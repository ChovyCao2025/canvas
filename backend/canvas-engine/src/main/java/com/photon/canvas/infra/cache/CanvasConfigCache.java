package com.photon.canvas.infra.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.photon.canvas.domain.canvas.CanvasVersionMapper;
import com.photon.canvas.engine.dag.DagGraph;
import com.photon.canvas.engine.dag.DagParser;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 三级画布配置缓存：L1 Caffeine → L2 Redis → L3 MySQL。
 * Key = "canvas:{canvasId}:v{versionId}:config"
 * 发布时通过 Redis Pub/Sub 通知各实例清除 L1。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CanvasConfigCache {

    private final StringRedisTemplate redis;
    private final CanvasVersionMapper canvasVersionMapper;
    private final DagParser dagParser;

    /** L1: JVM 本地缓存，最多 500 条，永不过期（依赖主动失效） */
    private Cache<String, DagGraph> l1;

    private static final Duration L2_TTL = Duration.ofHours(24);
    private static final String INVALIDATE_CHANNEL = "canvas:cache:invalidate";

    @PostConstruct
    void init() {
        l1 = Caffeine.newBuilder().maximumSize(500).build();
        // 订阅失效广播
        // redis.listenToChannel(INVALIDATE_CHANNEL, ...) — Phase 11 接入 Pub/Sub
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
        log.debug("画布配置缓存 MISS → MySQL canvasId={} versionId={}", canvasId, versionId);
        return graph;
    }

    public void invalidate(Long canvasId, Long versionId) {
        String l1Key = canvasId + ":v" + versionId;
        l1.invalidate(l1Key);
        redis.delete("canvas:" + canvasId + ":v" + versionId + ":config");
        // Phase 11: redis.convertAndSend(INVALIDATE_CHANNEL, l1Key);
    }
}
