package org.chovy.canvas.domain.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dto.notification.NotificationDTO;
import org.chovy.canvas.dto.notification.NotificationRealtimePayload;
import org.chovy.canvas.engine.reactive.BackgroundSubscriptionRegistry;
import org.chovy.canvas.infrastructure.redis.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.CloseStatus;
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
 * 通知实时推送服务。
 *
 * <p>维护本机 WebSocket 会话，并通过 Redis Pub/Sub 分发跨实例通知事件。
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
    /** 单用户最大 WebSocket 连接数。 */
    private final int maxSessionsPerUser;
    /** 当前服务实例最大 WebSocket 连接数。 */
    private final int maxTotalSessions;
    /** 后台订阅注册表，统一托管 Redis Pub/Sub 订阅生命周期。 */
    private final BackgroundSubscriptionRegistry backgroundSubscriptions;
    /** 响应式 Redis 监听容器，用于订阅跨实例实时通知频道。 */
    private ReactiveRedisMessageListenerContainer listenerContainer;
    /** Redis 通知频道订阅句柄，应用关闭时释放。 */
    private Disposable subscription;

    /** 注入序列化、Redis 发布订阅和业务 key 依赖。 */
    public NotificationRealtimeService(
            ObjectMapper objectMapper,
            StringRedisTemplate redis,
            ReactiveRedisConnectionFactory reactiveRedisConnectionFactory,
            RedisKeyUtil keys,
            @Value("${canvas.notifications.websocket.max-sessions-per-user:5}") int maxSessionsPerUser,
            @Value("${canvas.notifications.websocket.max-total-sessions:1000}") int maxTotalSessions) {
        this(objectMapper, redis, reactiveRedisConnectionFactory, keys,
                /**
                 * 执行 BackgroundSubscriptionRegistry 流程，围绕 background subscription registry 完成校验、计算或结果组装。
                 *
                 * @return 返回 BackgroundSubscriptionRegistry 流程生成的业务结果。
                 */
                maxSessionsPerUser, maxTotalSessions, new BackgroundSubscriptionRegistry());
    }

    /**
     * 创建 NotificationRealtimeService 实例并注入 domain.notification 场景依赖。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param redis redis 参数，用于 NotificationRealtimeService 流程中的校验、计算或对象转换。
     * @param reactiveRedisConnectionFactory 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param keys keys 参数，用于 NotificationRealtimeService 流程中的校验、计算或对象转换。
     * @param maxSessionsPerUser max sessions per user 参数，用于 NotificationRealtimeService 流程中的校验、计算或对象转换。
     * @param maxTotalSessions max total sessions 参数，用于 NotificationRealtimeService 流程中的校验、计算或对象转换。
     * @param backgroundSubscriptions background subscriptions 参数，用于 NotificationRealtimeService 流程中的校验、计算或对象转换。
     */
    @Autowired
    public NotificationRealtimeService(
            ObjectMapper objectMapper,
            StringRedisTemplate redis,
            ReactiveRedisConnectionFactory reactiveRedisConnectionFactory,
            RedisKeyUtil keys,
            @Value("${canvas.notifications.websocket.max-sessions-per-user:5}") int maxSessionsPerUser,
            @Value("${canvas.notifications.websocket.max-total-sessions:1000}") int maxTotalSessions,
            BackgroundSubscriptionRegistry backgroundSubscriptions) {
        this.objectMapper = objectMapper;
        this.redis = redis;
        this.reactiveRedisConnectionFactory = reactiveRedisConnectionFactory;
        this.keys = keys;
        this.maxSessionsPerUser = Math.max(1, maxSessionsPerUser);
        this.maxTotalSessions = Math.max(1, maxTotalSessions);
        this.backgroundSubscriptions = backgroundSubscriptions == null
                /**
                 * 执行 BackgroundSubscriptionRegistry 流程，围绕 background subscription registry 完成校验、计算或结果组装。
                 *
                 * @return 返回 BackgroundSubscriptionRegistry 流程生成的业务结果。
                 */
                ? new BackgroundSubscriptionRegistry()
                : backgroundSubscriptions;
    }

    /** 订阅 Redis 实时通知频道，接收其他实例发布的通知。 */
    @PostConstruct
    public void subscribeRedisChannel() {
        try {
            listenerContainer = new ReactiveRedisMessageListenerContainer(reactiveRedisConnectionFactory);
            subscription = backgroundSubscriptions.track(
                    "notification-realtime-redis",
                    listenerContainer.receive(ChannelTopic.of(keys.notificationChannel()))
                            .doOnNext(message -> handleRemote(message.getMessage())),
                    e -> log.error("[NOTIFICATION_WS] Redis 订阅异常: {}", e.getMessage(), e));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
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
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
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
        return register(null, userId, session, initialPayload);
    }

    /** 注册指定租户内用户 WebSocket 会话，并把初始通知快照推送到该连接。 */
    public Mono<Void> register(Long tenantId, String userId, WebSocketSession session, NotificationRealtimePayload initialPayload) {
        String sessionId = session.getId();
        String sessionKey = sessionKey(tenantId, userId);
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        if (!registerSession(sessionKey, sessionId, sink)) {
            log.warn("[NOTIFICATION_WS] 拒绝连接 tenantId={} userId={} sessionId={} activeUser={} activeTotal={}",
                    /**
                     * 执行 activeSessionCount 流程，围绕 active session count 完成校验、计算或结果组装。
                     *
                     * @param tenantId 租户 ID，用于限定数据隔离范围。
                     * @return 返回 activeSessionCount 流程生成的业务结果。
                     */
                    tenantId, userId, sessionId, activeSessionCount(tenantId, userId), totalActiveSessionCount());
            return session.close(CloseStatus.POLICY_VIOLATION);
        }
        // 一个用户可同时存在多个浏览器会话，按 sessionId 独立维护推送通道。
        emit(sink, initialPayload);
        Mono<Void> output = session.send(sink.asFlux().map(session::textMessage));
        Mono<Void> input = session.receive()
                .doOnNext(message -> {
                    if ("ping".equalsIgnoreCase(message.getPayloadAsText())) {
                        emit(sink, NotificationRealtimePayload.event("PONG", null, null));
                    }
                })
                .then()
                .doFinally(signal -> unregister(sessionKey, sessionId));
        return Mono.when(output, input)
                .doFinally(signal -> unregister(sessionKey, sessionId));
    }

    /** 发布实时通知到本机会话和跨实例 Redis 通道。 */
    @Override
    public void publish(String eventType, String userId, NotificationDO notification, Long unreadCount) {
        publish(eventType, notification == null ? null : notification.getTenantId(), userId, notification, unreadCount);
    }

    /** 发布指定租户内用户实时通知到本机会话和跨实例 Redis 通道。 */
    @Override
    public void publish(String eventType, Long tenantId, String userId, NotificationDO notification, Long unreadCount) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        NotificationRealtimePayload payload = NotificationRealtimePayload.event(
                eventType,
                notification == null ? null : NotificationDTO.from(notification),
                unreadCount);
        NotificationRealtimeEnvelope envelope = new NotificationRealtimeEnvelope(originId, tenantId, userId, payload);
        deliverLocal(envelope);
        try {
            // 先本机投递再广播 Redis，当前实例即使 Redis 短暂不可用也能收到实时通知。
            redis.convertAndSend(keys.notificationChannel(), objectMapper.writeValueAsString(envelope));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            log.warn("[NOTIFICATION_WS] Redis 广播失败，已完成本实例推送 userId={}: {}", userId, e.getMessage());
        }
    }

    /** 返回指定用户当前活跃 WebSocket 会话数。 */
    public int activeSessionCount(String userId) {
        return activeSessionCount(null, userId);
    }

    /** 返回指定租户内用户当前活跃 WebSocket 会话数。 */
    public int activeSessionCount(Long tenantId, String userId) {
        Map<String, Sinks.Many<String>> sessions = sessionsByUser.get(sessionKey(tenantId, userId));
        return sessions == null ? 0 : sessions.size();
    }

    /** 返回当前服务实例全部活跃 WebSocket 会话数。 */
    public int totalActiveSessionCount() {
        return sessionsByUser.values().stream()
                .mapToInt(Map::size)
                .sum();
    }

    /** 在连接数上限内注册会话；检查和写入必须保持原子性。 */
    private boolean registerSession(String sessionKey, String sessionId, Sinks.Many<String> sink) {
        synchronized (sessionsByUser) {
            if (activeSessionCountByKey(sessionKey) >= maxSessionsPerUser
                    /**
                     * 转换为接口返回或领域视图。
                     *
                     * @param maxTotalSessions max total sessions 参数，用于 totalActiveSessionCount 流程中的校验、计算或对象转换。
                     * @return 返回统计数量。
                     */
                    || totalActiveSessionCount() >= maxTotalSessions) {
                return false;
            }
            sessionsByUser.computeIfAbsent(sessionKey, ignored -> new ConcurrentHashMap<>())
                    .put(sessionId, sink);
            return true;
        }
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            log.warn("[NOTIFICATION_WS] 忽略无法解析的通知广播: {}", e.getMessage());
        }
    }

    /** 将通知投递到本机当前用户的全部活跃 WebSocket 会话。 */
    private void deliverLocal(NotificationRealtimeEnvelope envelope) {
        Map<String, Sinks.Many<String>> sessions = sessionsByUser.get(sessionKey(envelope.tenantId(), envelope.userId()));
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            log.warn("[NOTIFICATION_WS] 消息序列化失败: {}", e.getMessage());
        }
    }

    /** 注销 WebSocket 会话并在用户无会话时清理本地映射。 */
    private void unregister(String sessionKey, String sessionId) {
        synchronized (sessionsByUser) {
            Map<String, Sinks.Many<String>> sessions = sessionsByUser.get(sessionKey);
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (sessions == null) {
                // 汇总前面计算出的状态和明细，返回给调用方。
                return;
            }
            Sinks.Many<String> sink = sessions.remove(sessionId);
            if (sink != null) {
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                sink.tryEmitComplete();
            }
            if (sessions.isEmpty()) {
                sessionsByUser.remove(sessionKey);
            }
        }
    }

    /**
     * 执行 activeSessionCountByKey 流程，围绕 active session count by key 完成校验、计算或结果组装。
     *
     * @param sessionKey 业务键，用于在同一租户下定位资源。
     * @return 返回 active session count by key 计算得到的数量、金额或指标值。
     */
    private int activeSessionCountByKey(String sessionKey) {
        Map<String, Sinks.Many<String>> sessions = sessionsByUser.get(sessionKey);
        return sessions == null ? 0 : sessions.size();
    }

    /**
     * 执行 sessionKey 流程，围绕 session key 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @return 返回 session key 生成的文本或业务键。
     */
    private String sessionKey(Long tenantId, String userId) {
        return (tenantId == null ? "global" : tenantId) + ":" + userId;
    }
}
