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

/**
 * 通知实时推送 通知领域组件。
 *
 * <p>负责站内通知的创建、收件人解析、未读状态和实时推送封装。
 * <p>该组件连接异步任务、WebSocket 和通知持久化模型，保证消息中心口径一致。
 */
@Slf4j
@Service
public class NotificationRealtimeService implements NotificationRealtimePublisher {

    /** 当前服务实例标识，用于避免 Redis 实时通知回环处理。 */
    private final String originId = UUID.randomUUID().toString();
    /** Jackson ObjectMapper，用于 JSON 序列化和反序列化。 */
    private final ObjectMapper objectMapper;
    /** 阻塞式 Redis 模板，用于锁、去重、票据或跨实例通知。 */
    private final StringRedisTemplate redis;
    /** 响应式 Redis 连接工厂，用于 WebSocket 跨实例通知订阅。 */
    private final ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;
    /** Redis key 工具，集中生成业务 key。 */
    private final RedisKeyUtil keys;
    /** 本机维护的用户到 WebSocket 会话 sink 的映射。 */
    private final Map<String, Map<String, Sinks.Many<String>>> sessionsByUser = new ConcurrentHashMap<>();
    /** 响应式 Redis 监听容器，用于订阅跨实例实时通知频道。 */
    private ReactiveRedisMessageListenerContainer listenerContainer;
    /** Redis 通知频道订阅句柄，应用关闭时释放。 */
    private Disposable subscription;

    /** 注入序列化、Redis 发布订阅和业务 key 依赖。 */
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

    /** 订阅 Redis 实时通知频道，接收其他实例发布的通知。 */
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

    /** 关闭服务时释放订阅和连接资源。 */
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

    /** 注册用户 WebSocket 会话，并把初始通知快照推送到该连接。 */
    public Mono<Void> register(String userId, WebSocketSession session, NotificationRealtimePayload initialPayload) {
        String sessionId = session.getId();
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        // 一个用户可同时存在多个浏览器会话，按 sessionId 独立维护推送通道。
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

    /** 发布实时通知到本机会话和跨实例 Redis 通道。 */
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
            // 先本机投递再广播 Redis，当前实例即使 Redis 短暂不可用也能收到实时通知。
            redis.convertAndSend(keys.notificationChannel(), objectMapper.writeValueAsString(envelope));
        } catch (Exception e) {
            log.warn("[NOTIFICATION_WS] Redis 广播失败，已完成本实例推送 userId={}: {}", userId, e.getMessage());
        }
    }

    /** 返回指定用户当前活跃 WebSocket 会话数。 */
    public int activeSessionCount(String userId) {
        Map<String, Sinks.Many<String>> sessions = sessionsByUser.get(userId);
        return sessions == null ? 0 : sessions.size();
    }

    /** 处理其他实例通过 Redis 广播过来的实时通知。 */
    private void handleRemote(String raw) {
        try {
            NotificationRealtimeEnvelope envelope = objectMapper.readValue(raw, NotificationRealtimeEnvelope.class);
            if (originId.equals(envelope.originId())) {
                // 忽略自己通过 Redis 广播回来的消息，避免同实例重复推送。
                return;
            }
            deliverLocal(envelope);
        } catch (Exception e) {
            log.warn("[NOTIFICATION_WS] 忽略无法解析的通知广播: {}", e.getMessage());
        }
    }

    /** 将通知投递到本机当前用户的全部活跃 WebSocket 会话。 */
    private void deliverLocal(NotificationRealtimeEnvelope envelope) {
        Map<String, Sinks.Many<String>> sessions = sessionsByUser.get(envelope.userId());
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        Set<Map.Entry<String, Sinks.Many<String>>> entries = Set.copyOf(sessions.entrySet());
        for (Map.Entry<String, Sinks.Many<String>> entry : entries) {
            // 对快照遍历，避免推送过程中连接关闭导致并发修改影响其他会话。
            emit(entry.getValue(), envelope.payload());
        }
    }

    /** 将实时通知载荷序列化后写入单个 WebSocket sink。 */
    private void emit(Sinks.Many<String> sink, NotificationRealtimePayload payload) {
        try {
            sink.tryEmitNext(objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.warn("[NOTIFICATION_WS] 消息序列化失败: {}", e.getMessage());
        }
    }

    /** 注销 WebSocket 会话并在用户无会话时清理本地映射。 */
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
