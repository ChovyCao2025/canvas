package org.chovy.canvas.domain.notification;

import org.chovy.canvas.infra.redis.RedisKeyUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class NotificationWebSocketTicketService {

    private static final Duration TICKET_TTL = Duration.ofSeconds(60);
    public static final int TICKET_TTL_SECONDS = 60;

    private final StringRedisTemplate redis;
    private final RedisKeyUtil keys;

    public NotificationWebSocketTicketService(StringRedisTemplate redis, RedisKeyUtil keys) {
        this.redis = redis;
        this.keys = keys;
    }

    public String createTicket(String userId) {
        String ticket = "ntf_ws_" + UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(keys.notificationWsTicket(ticket), userId, TICKET_TTL);
        return ticket;
    }

    public String consumeTicket(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            return null;
        }
        return redis.opsForValue().getAndDelete(keys.notificationWsTicket(ticket));
    }
}
