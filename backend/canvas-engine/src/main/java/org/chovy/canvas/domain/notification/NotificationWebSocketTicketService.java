package org.chovy.canvas.domain.notification;

import org.chovy.canvas.infrastructure.redis.RedisKeyUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * 通知 WebSocket 票据 通知领域组件。
 *
 * <p>负责站内通知的创建、收件人解析、未读状态和实时推送封装。
 * <p>该组件连接异步任务、WebSocket 和通知持久化模型，保证消息中心口径一致。
 */
@Service
public class NotificationWebSocketTicketService {

    /** WebSocket 一次性认证票据在 Redis 中的有效期。 */
    private static final Duration TICKET_TTL = Duration.ofSeconds(60);
    /** WebSocket 票据有效期秒数，返回给前端用于过期提示。 */
    public static final int TICKET_TTL_SECONDS = 60;

    /** 阻塞式 Redis 模板，用于锁、去重、票据或跨实例通知。 */
    private final StringRedisTemplate redis;
    /** Redis key 工具，集中生成业务 key。 */
    private final RedisKeyUtil keys;

    /** 注入 Redis 和 key 生成工具，用于创建与消费一次性票据。 */
    public NotificationWebSocketTicketService(StringRedisTemplate redis, RedisKeyUtil keys) {
        this.redis = redis;
        this.keys = keys;
    }

    /** 创建一次性 WebSocket 认证票据。 */
    public String createTicket(String userId) {
        return createTicket(null, userId);
    }

    /** 创建绑定租户和用户的一次性 WebSocket 认证票据。 */
    public String createTicket(Long tenantId, String userId) {
        String ticket = "ntf_ws_" + UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(keys.notificationWsTicket(ticket), encode(tenantId, userId), TICKET_TTL);
        return ticket;
    }

    /** 消费一次性 WebSocket 票据并返回绑定用户。 */
    public String consumeTicket(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            return null;
        }
        return redis.opsForValue().getAndDelete(keys.notificationWsTicket(ticket));
    }

    /** 消费一次性 WebSocket 票据并返回绑定的租户和用户。 */
    public TicketSubject consumeTicketSubject(String ticket) {
        return decode(consumeTicket(ticket));
    }

    private String encode(Long tenantId, String userId) {
        if (tenantId == null) {
            return userId;
        }
        return tenantId + "|" + userId;
    }

    private TicketSubject decode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        int separator = raw.indexOf('|');
        if (separator < 0) {
            return new TicketSubject(null, raw);
        }
        String tenantPart = raw.substring(0, separator);
        String userId = raw.substring(separator + 1);
        Long tenantId = null;
        if (!tenantPart.isBlank()) {
            tenantId = Long.valueOf(tenantPart);
        }
        return new TicketSubject(tenantId, userId);
    }

    public record TicketSubject(Long tenantId, String userId) {
    }
}
