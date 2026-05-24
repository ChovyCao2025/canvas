package org.chovy.canvas.domain.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dto.notification.NotificationDTO;
import org.chovy.canvas.dto.notification.NotificationRealtimePayload;
import org.chovy.canvas.infrastructure.redis.RedisKeyUtil;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.chovy.canvas.dal.dataobject.NotificationDO;

@Slf4j
@Service
public class NotificationRealtimeService implements NotificationRealtimePublisher {

    private final String originId = UUID.randomUUID().toString();
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redis;
    private final ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;
    private final RedisKeyUtil keys;
    private final Map<String, Map<String, Sinks.Many<String>>> sessionsByUser = new ConcurrentHashMap<>();
    private ReactiveRedisMessageListenerContainer listenerContainer;
    private Disposable subscription;

    public NotificationRealtimeService(
            ObjectMapper objectMapper,
            StringRedisTemplate redis,
            ReactiveRedisConnectionFactory reactiveRedisConnectionFactory,
            RedisKeyUtil keys) {
        this.objectMapper = objectMapper;
        this.redis = redis;
        this.reactiveRedisConnectionFactory = reactiveRedisConnectionFactory;
        this.keys = keys;
    }

    @PostConstruct
    public void subscribeRedisChannel() {
        try {
            listenerContainer = new ReactiveRedisMessageListenerContainer(reactiveRedisConnectionFactory);
            subscription = listenerContainer.receive(ChannelTopic.of(keys.notificationChannel()))
                    .subscribe(message -> handleRemote(message.getMessage()),
                            e -> log.error("[NOTIFICATION_WS] Redis 订阅异常: {}", e.getMessage(), e));
        } catch (Exception e) {
            log.warn("[NOTIFICATION_WS] Redis 订阅启动失败，降级为单实例本地推送: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
        if (listenerContainer != null) {
            try {
                listenerContainer.destroy();
            } catch (Exception e) {
                log.warn("[NOTIFICATION_WS] Redis 订阅容器关闭失败: {}", e.getMessage());
            }
        }
        sessionsByUser.values().forEach(sessions ->
                sessions.values().forEach(sink -> sink.tryEmitComplete()));
        sessionsByUser.clear();
    }

    public Mono<Void> register(String userId, WebSocketSession session, NotificationRealtimePayload initialPayload) {
        String sessionId = session.getId();
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        sessionsByUser.computeIfAbsent(userId, ignored -> new ConcurrentHashMap<>()).put(sessionId, sink);
        emit(sink, initialPayload);
        Mono<Void> output = session.send(sink.asFlux().map(session::textMessage));
        Mono<Void> input = session.receive()
                .doOnNext(message -> {
                    if ("ping".equalsIgnoreCase(message.getPayloadAsText())) {
                        emit(sink, NotificationRealtimePayload.event("PONG", null, null));
                    }
                })
                .then()
                .doFinally(signal -> unregister(userId, sessionId));
        return Mono.when(output, input)
                .doFinally(signal -> unregister(userId, sessionId));
    }

    @Override
    public void publish(String eventType, String userId, NotificationDO notification, Long unreadCount) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        NotificationRealtimePayload payload = NotificationRealtimePayload.event(
                eventType,
                notification == null ? null : NotificationDTO.from(notification),
                unreadCount);
        NotificationRealtimeEnvelope envelope = new NotificationRealtimeEnvelope(originId, userId, payload);
        deliverLocal(envelope);
        try {
            redis.convertAndSend(keys.notificationChannel(), objectMapper.writeValueAsString(envelope));
        } catch (Exception e) {
            log.warn("[NOTIFICATION_WS] Redis 广播失败，已完成本实例推送 userId={}: {}", userId, e.getMessage());
        }
    }

    public int activeSessionCount(String userId) {
        Map<String, Sinks.Many<String>> sessions = sessionsByUser.get(userId);
        return sessions == null ? 0 : sessions.size();
    }

    private void handleRemote(String raw) {
        try {
            NotificationRealtimeEnvelope envelope = objectMapper.readValue(raw, NotificationRealtimeEnvelope.class);
            if (originId.equals(envelope.originId())) {
                return;
            }
            deliverLocal(envelope);
        } catch (Exception e) {
            log.warn("[NOTIFICATION_WS] 忽略无法解析的通知广播: {}", e.getMessage());
        }
    }

    private void deliverLocal(NotificationRealtimeEnvelope envelope) {
        Map<String, Sinks.Many<String>> sessions = sessionsByUser.get(envelope.userId());
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        Set<Map.Entry<String, Sinks.Many<String>>> entries = Set.copyOf(sessions.entrySet());
        for (Map.Entry<String, Sinks.Many<String>> entry : entries) {
            emit(entry.getValue(), envelope.payload());
        }
    }

    private void emit(Sinks.Many<String> sink, NotificationRealtimePayload payload) {
        try {
            sink.tryEmitNext(objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.warn("[NOTIFICATION_WS] 消息序列化失败: {}", e.getMessage());
        }
    }

    private void unregister(String userId, String sessionId) {
        Map<String, Sinks.Many<String>> sessions = sessionsByUser.get(userId);
        if (sessions == null) {
            return;
        }
        Sinks.Many<String> sink = sessions.remove(sessionId);
        if (sink != null) {
            sink.tryEmitComplete();
        }
        if (sessions.isEmpty()) {
            sessionsByUser.remove(userId);
        }
    }
}
