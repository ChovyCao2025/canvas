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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @return 返回 encode 生成的文本或业务键。
     */
    private String encode(Long tenantId, String userId) {
        if (tenantId == null) {
            return userId;
        }
        return tenantId + "|" + userId;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param raw raw 参数，用于 decode 流程中的校验、计算或对象转换。
     * @return 返回 decode 流程生成的业务结果。
     */
    private TicketSubject decode(String raw) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new TicketSubject(tenantId, userId);
    }

    /**
     * TicketSubject 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record TicketSubject(Long tenantId, String userId) {
    }
}
