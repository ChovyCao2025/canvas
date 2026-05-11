package com.photon.canvas.infra.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.photon.canvas.engine.context.ExecutionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * ExecutionContext 在 Redis 中的持久化（多阶段执行挂起/恢复）。
 * Key = canvas:{canvasId}:user:{userId}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContextPersistenceService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Value("${canvas.execution.context-ttl-sec:86400}")
    private long ttlSec;

    private String key(Long canvasId, String userId) {
        return "canvas:" + canvasId + ":user:" + userId;
    }

    private String resumeLockKey(Long canvasId, String userId) {
        return "canvas:resume-lock:" + canvasId + ":" + userId;
    }

    private String dedupKey(Long canvasId, String userId, String msgId) {
        return "canvas:dedup:" + canvasId + ":" + userId + ":" + msgId;
    }

    // ── 上下文持久化 ──────────────────────────────────────────────

    public void save(ExecutionContext ctx) {
        try {
            String json = objectMapper.writeValueAsString(ctx);
            redis.opsForValue().set(key(ctx.getCanvasId(), ctx.getUserId()),
                    json, Duration.ofSeconds(ttlSec));
        } catch (Exception e) {
            log.error("保存 ExecutionContext 失败: {}", e.getMessage());
        }
    }

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

    public void delete(Long canvasId, String userId) {
        redis.delete(key(canvasId, userId));
    }

    public boolean exists(Long canvasId, String userId) {
        return Boolean.TRUE.equals(redis.hasKey(key(canvasId, userId)));
    }

    // ── 恢复锁 ─────────────────────────────────────────────────────

    public boolean acquireResumeLock(Long canvasId, String userId, String instanceId,
                                      long timeoutSec) {
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
        return Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(dedupKey(canvasId, userId, msgId),
                        "1", ttl));
    }

    public void releaseDedup(String fullDedupKey) {
        redis.delete(fullDedupKey);
    }

    public String buildDedupKey(Long canvasId, String userId, String msgId) {
        return dedupKey(canvasId, userId, msgId);
    }
}
