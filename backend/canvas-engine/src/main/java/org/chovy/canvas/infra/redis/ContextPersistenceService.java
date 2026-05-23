package org.chovy.canvas.infra.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * ExecutionContext 在 Redis 中的持久化（多阶段执行挂起/恢复）。
 * Key = canvas:{canvasId}:user:{userId}
 *
 * 设计目标：
 * 1) 支持节点挂起后的上下文恢复；
 * 2) 支持恢复过程并发互斥；
 * 3) 支持消息级幂等去重。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContextPersistenceService {

    /** 阻塞式 Redis 模板（执行链路中以轻量 KV 操作为主）。 */
    private final StringRedisTemplate redis;

    /** JSON 序列化器（ExecutionContext <-> String）。 */
    private final ObjectMapper objectMapper;

    /** 上下文快照过期时间（秒），默认 24 小时。 */
    @Value("${canvas.execution.context-ttl-sec:86400}")
    private long ttlSec;

    /** ExecutionContext 主键：同一画布 + 同一用户只保留一份上下文快照。 */
    private String key(Long canvasId, String userId) {
        return "canvas:" + canvasId + ":user:" + userId;
    }

    /** 恢复锁：防止并发触发同时恢复同一份 PAUSED 上下文。 */
    private String resumeLockKey(Long canvasId, String userId) {
        return "canvas:resume-lock:" + canvasId + ":" + userId;
    }

    /** 去重键：约束同一消息在 TTL 内只触发一次。 */
    private String dedupKey(Long canvasId, String userId, String msgId) {
        return "canvas:dedup:" + canvasId + ":" + userId + ":" + msgId;
    }

    // ── 上下文持久化 ──────────────────────────────────────────────

    /** 保存或覆盖上下文快照，并刷新 TTL。 */
    public void save(ExecutionContext ctx) {
        try {
            String json = objectMapper.writeValueAsString(ctx);
            // 敏感字段脱敏后再存 Redis（设计文档 13.8节）
            String masked = org.chovy.canvas.common.DataMaskingUtil.maskJson(
                    json, org.chovy.canvas.common.DataMaskingUtil.DEFAULT_SENSITIVE_KEYS);
            // 覆盖写 + TTL 续期：每次挂起/恢复都会刷新上下文生命周期
            redis.opsForValue().set(key(ctx.getCanvasId(), ctx.getUserId()),
                    masked, Duration.ofSeconds(ttlSec));
        } catch (Exception e) {
            log.error("保存 ExecutionContext 失败: {}", e.getMessage());
        }
    }

    /** 加载上下文；不存在或反序列化失败时返回 null。 */
    public ExecutionContext load(Long canvasId, String userId) {
        String json = redis.opsForValue().get(key(canvasId, userId));
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, ExecutionContext.class);
        } catch (Exception e) {
            log.error("反序列化 ExecutionContext 失败: {}", e.getMessage());
            return null;
        }
    }

    /** 删除上下文快照（执行结束或失败后清理）。 */
    public void delete(Long canvasId, String userId) {
        redis.delete(key(canvasId, userId));
    }

    /** 判断上下文是否存在（常用于恢复分支判断）。 */
    public boolean exists(Long canvasId, String userId) {
        return redis.hasKey(key(canvasId, userId));
    }

    // ── 恢复锁 ─────────────────────────────────────────────────────

    public boolean acquireResumeLock(Long canvasId, String userId, String instanceId,
                                      long timeoutSec) {
        // SETNX + EX：锁自动过期，避免实例崩溃后死锁
        return Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(resumeLockKey(canvasId, userId),
                        instanceId, Duration.ofSeconds(timeoutSec)));
    }

    public void releaseResumeLock(Long canvasId, String userId) {
        redis.delete(resumeLockKey(canvasId, userId));
    }

    // ── dedup key ────────────────────────────────────────────────

    public boolean acquireDedup(Long canvasId, String userId, String msgId,
                                  Duration ttl) {
        // SETNX 保证“首次写入成功”，重复消息直接返回 false
        return Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(dedupKey(canvasId, userId, msgId),
                        "1", ttl));
    }

    public void releaseDedup(String fullDedupKey) {
        // 仅在需要主动释放时调用；常规场景依赖 TTL 自然过期
        redis.delete(fullDedupKey);
    }

    /** 供调用方构造可观测/可删除的完整 dedup key。 */
    public String buildDedupKey(Long canvasId, String userId, String msgId) {
        return dedupKey(canvasId, userId, msgId);
    }
}
