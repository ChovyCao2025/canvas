package org.chovy.canvas.infra.redis;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Redis key 统一构造（设计文档 25.3.1节）。
 *
 * 所有 Redis key 通过此类构造，支持命名空间前缀配置，
 * 解决多环境共用同一 Redis 时的 key 冲突问题。
 *
 * 配置：canvas.redis.key-prefix=canvas（默认）
 */
@Component
public class RedisKeyUtil {

    @Getter
    @Value("${canvas.redis.key-prefix:canvas}")
    private String prefix = "canvas";

    // ── 触发路由 ─────────────────────────────────────────────────
    public String triggerMq(String topicKey)       { return prefix + ":trigger:mq:" + topicKey; }
    public String triggerBehavior(String code)     { return prefix + ":trigger:behavior:" + code; }
    public String triggerTagger(String tagCodeKey) { return prefix + ":trigger:tagger:" + tagCodeKey; }
    public String triggerPattern()                 { return prefix + ":trigger:*"; }

    // ── 执行上下文 ─────────────────────────────────────────────────
    public String context(Long canvasId, String userId)  { return prefix + ":" + canvasId + ":user:" + userId; }
    public String resumeLock(Long canvasId, String userId){ return prefix + ":resume-lock:" + canvasId + ":" + userId; }
    public String dedup(Long canvasId, String userId, String msgId) {
        return prefix + ":dedup:" + canvasId + ":" + userId + ":" + msgId;
    }
    public String globalCount(Long canvasId)  { return prefix + ":global_count:" + canvasId; }
    public String quota(Long canvasId, String userId, String date) {
        return prefix + ":quota:" + canvasId + ":" + userId + ":" + date;
    }

    // ── 并发锁 ─────────────────────────────────────────────────────
    public String publishLock(Long canvasId) { return prefix + ":publish:lock:" + canvasId; }

    // ── 认证安全 ───────────────────────────────────────────────────
    public String loginFail(String username)   { return prefix + ":login:fail:" + username; }
    public String loginLocked(String username) { return prefix + ":login:locked:" + username; }
    public String jwtRevoked(String tokenHash) { return prefix + ":jwt:revoked:" + tokenHash; }

    // ── 缓存 ───────────────────────────────────────────────────────
    public String canvasConfig(Long canvasId, Long versionId) {
        return prefix + ":" + canvasId + ":v" + versionId + ":config";
    }
    public String cacheInvalidateChannel() { return prefix + ":cache:invalidate"; }

    // ── Kill Switch ────────────────────────────────────────────────
    public String killChannel(Long canvasId)    { return prefix + ":kill:" + canvasId; }
    public String killPattern()                 { return prefix + ":kill:*"; }

    // ── 消息中心 ───────────────────────────────────────────────────
    public String notificationWsTicket(String ticket) { return prefix + ":notification:ws-ticket:" + ticket; }
    public String notificationChannel()              { return prefix + ":notification:events"; }
}
