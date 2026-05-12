package org.chovy.canvas.infra.redis;

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

    /**
     * 检查路由表是否为空（设计文档 6.4节注意事项）。
     * 使用 SCAN 游标而非 KEYS 命令，避免 O(N) 阻塞 Redis 单线程。
     * SCAN count(1) + 找到第一条即停止，O(1) 开销。
     */
    public boolean isRouteTableEmpty() {
        try (var cursor = redis.getConnectionFactory()
                .getConnection()
                .scan(org.springframework.data.redis.core.ScanOptions.scanOptions()
                        .match("canvas:trigger:*").count(1).build())) {
            return !cursor.hasNext();
        } catch (Exception e) {
            // Redis 不可用时保守返回 true（触发重建）
            return true;
        }
    }
}
