package org.chovy.canvas.infra.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

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

    public void registerMq(Long canvasId, String topicKey) {
        redis.opsForSet().add(keys.triggerMq(topicKey), String.valueOf(canvasId));
    }
    public void registerBehavior(Long canvasId, String eventCode) {
        redis.opsForSet().add(keys.triggerBehavior(eventCode), String.valueOf(canvasId));
    }
    public void registerTagger(Long canvasId, String tagCodeKey) {
        redis.opsForSet().add(keys.triggerTagger(tagCodeKey), String.valueOf(canvasId));
    }
    public void removeMq(Long canvasId, String topicKey) {
        redis.opsForSet().remove(keys.triggerMq(topicKey), String.valueOf(canvasId));
    }
    public void removeBehavior(Long canvasId, String eventCode) {
        redis.opsForSet().remove(keys.triggerBehavior(eventCode), String.valueOf(canvasId));
    }
    public void removeTagger(Long canvasId, String tagCodeKey) {
        redis.opsForSet().remove(keys.triggerTagger(tagCodeKey), String.valueOf(canvasId));
    }
    public Set<String> getCanvasByMqTopic(String topicKey) {
        Set<String> ids = redis.opsForSet().members(keys.triggerMq(topicKey));
        return ids != null ? ids : Set.of();
    }
    public Set<String> getCanvasByBehavior(String eventCode) {
        Set<String> ids = redis.opsForSet().members(keys.triggerBehavior(eventCode));
        return ids != null ? ids : Set.of();
    }
    public Set<String> getCanvasByTagger(String tagCodeKey) {
        Set<String> ids = redis.opsForSet().members(keys.triggerTagger(tagCodeKey));
        return ids != null ? ids : Set.of();
    }

    /**
     * 检查路由表是否为空（用 SCAN，不用 KEYS，设计文档 6.4节）。
     */
    public boolean isRouteTableEmpty() {
        try (var cursor = reactiveFactory.getReactiveConnection()
                .keyCommands()
                .scan(ScanOptions.scanOptions().match(keys.triggerPattern()).count(1).build())
                .blockFirst()) {
            // 如果 blockFirst() 返回 null 说明没有任何 key
            return cursor == null;
        } catch (Exception e) {
            return true;
        }
    }
}
