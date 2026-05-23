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

    /** 统一 key 构造器（含命名空间前缀）。 */
    private final RedisKeyUtil keys;

    /** reactive 连接，仅用于 SCAN 探测路由表是否为空。 */
    private final ReactiveRedisConnectionFactory reactiveFactory;

    /** 注册 MQ 触发路由：topicKey -> canvasId。 */
    public void registerMq(Long canvasId, String topicKey) {
        redis.opsForSet().add(keys.triggerMq(topicKey), String.valueOf(canvasId));
    }
    /** 注册行为事件触发路由：eventCode -> canvasId。 */
    public void registerBehavior(Long canvasId, String eventCode) {
        redis.opsForSet().add(keys.triggerBehavior(eventCode), String.valueOf(canvasId));
    }
    /** 注册 tagger 实时触发路由：tagCodeKey -> canvasId。 */
    public void registerTagger(Long canvasId, String tagCodeKey) {
        redis.opsForSet().add(keys.triggerTagger(tagCodeKey), String.valueOf(canvasId));
    }
    /** 移除 MQ 路由。 */
    public void removeMq(Long canvasId, String topicKey) {
        redis.opsForSet().remove(keys.triggerMq(topicKey), String.valueOf(canvasId));
    }
    /** 移除行为事件路由。 */
    public void removeBehavior(Long canvasId, String eventCode) {
        redis.opsForSet().remove(keys.triggerBehavior(eventCode), String.valueOf(canvasId));
    }
    /** 移除 tagger 路由。 */
    public void removeTagger(Long canvasId, String tagCodeKey) {
        redis.opsForSet().remove(keys.triggerTagger(tagCodeKey), String.valueOf(canvasId));
    }
    /** 查询某 MQ topic 对应的画布集合。 */
    public Set<String> getCanvasByMqTopic(String topicKey) {
        Set<String> ids = redis.opsForSet().members(keys.triggerMq(topicKey));
        return ids != null ? ids : Set.of();
    }
    /** 查询某行为事件对应的画布集合。 */
    public Set<String> getCanvasByBehavior(String eventCode) {
        Set<String> ids = redis.opsForSet().members(keys.triggerBehavior(eventCode));
        return ids != null ? ids : Set.of();
    }
    /** 查询某 tagger code 对应的画布集合。 */
    public Set<String> getCanvasByTagger(String tagCodeKey) {
        Set<String> ids = redis.opsForSet().members(keys.triggerTagger(tagCodeKey));
        return ids != null ? ids : Set.of();
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
}
