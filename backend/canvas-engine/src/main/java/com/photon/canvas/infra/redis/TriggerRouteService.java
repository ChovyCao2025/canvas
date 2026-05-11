package com.photon.canvas.infra.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 画布触发路由表（Redis Set）。
 * topic/eventCode → Set<canvasId>
 */
@Service
@RequiredArgsConstructor
public class TriggerRouteService {

    private final StringRedisTemplate redis;

    private static final String MQ_KEY       = "canvas:trigger:mq:";
    private static final String BEHAVIOR_KEY = "canvas:trigger:behavior:";
    private static final String TAGGER_KEY   = "canvas:trigger:tagger:";

    // ── 注册 ─────────────────────────────────────────────────────

    public void registerMq(Long canvasId, String topicKey) {
        redis.opsForSet().add(MQ_KEY + topicKey, String.valueOf(canvasId));
    }

    public void registerBehavior(Long canvasId, String eventCode) {
        redis.opsForSet().add(BEHAVIOR_KEY + eventCode, String.valueOf(canvasId));
    }

    public void registerTagger(Long canvasId, String tagCodeKey) {
        redis.opsForSet().add(TAGGER_KEY + tagCodeKey, String.valueOf(canvasId));
    }

    // ── 注销 ─────────────────────────────────────────────────────

    public void removeMq(Long canvasId, String topicKey) {
        redis.opsForSet().remove(MQ_KEY + topicKey, String.valueOf(canvasId));
    }

    public void removeBehavior(Long canvasId, String eventCode) {
        redis.opsForSet().remove(BEHAVIOR_KEY + eventCode, String.valueOf(canvasId));
    }

    public void removeTagger(Long canvasId, String tagCodeKey) {
        redis.opsForSet().remove(TAGGER_KEY + tagCodeKey, String.valueOf(canvasId));
    }

    public void removeAllForCanvas(Long canvasId, String graphJson) {
        // 扫描 graphJson 里所有触发器节点并清理（简化实现：调用方传入要清的 key）
        // 实际由 CanvasService 在下线时调用
    }

    // ── 查询 ─────────────────────────────────────────────────────

    public Set<String> getCanvasByMqTopic(String topicKey) {
        Set<String> ids = redis.opsForSet().members(MQ_KEY + topicKey);
        return ids != null ? ids : Set.of();
    }

    public Set<String> getCanvasByBehavior(String eventCode) {
        Set<String> ids = redis.opsForSet().members(BEHAVIOR_KEY + eventCode);
        return ids != null ? ids : Set.of();
    }

    public Set<String> getCanvasByTagger(String tagCodeKey) {
        Set<String> ids = redis.opsForSet().members(TAGGER_KEY + tagCodeKey);
        return ids != null ? ids : Set.of();
    }

    public boolean isRouteTableEmpty() {
        // 抽查：任意一个已知 key 是否存在
        Set<String> keys = redis.keys("canvas:trigger:*");
        return keys == null || keys.isEmpty();
    }
}
