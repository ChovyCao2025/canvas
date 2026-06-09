package org.chovy.canvas.domain.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dto.notification.NotificationDTO;
import org.chovy.canvas.dto.notification.NotificationRealtimePayload;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 通知消息 WebSocket 连接处理器。
 *
 * <p>负责校验客户端携带的一次性连接票据，并在连接建立时返回当前通知快照。
 * <p>后续实时消息由 {@link NotificationRealtimeService} 统一维护会话和广播。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler implements WebSocketHandler {

    /** WebSocket 建连后首屏同步的通知条数。 */
    private static final int INITIAL_SYNC_SIZE = 20;

    /** WebSocket 票据服务，用于校验并消费一次性连接票据。 */
    private final NotificationWebSocketTicketService ticketService;
    /** 通知服务，用于读取首屏通知列表和未读数量。 */
    private final NotificationService notificationService;
    /** 实时通知服务，用于注册会话并推送增量消息。 */
    private final NotificationRealtimeService realtimeService;

    /**
     * 处理通知 WebSocket 建连请求。
     *
     * <p>连接必须携带有效 ticket；校验失败时关闭连接，校验通过后先发送初始通知快照，
     * 再把会话注册到实时推送服务。
     *
     * @param session 当前 WebSocket 会话，包含握手 URI 和底层消息通道
     * @return 会话生命周期对应的异步完成信号
     */
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String ticket = extractTicket(session);
        if (ticket == null || ticket.isBlank()) {
            return session.close(CloseStatus.POLICY_VIOLATION);
        }
        return Mono.fromCallable(() -> ticketService.consumeTicketSubject(ticket))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(subject -> {
                    if (subject == null || subject.userId() == null || subject.userId().isBlank()) {
                        return session.close(CloseStatus.POLICY_VIOLATION);
                    }
                    // ticket 校验通过后先同步初始快照，再注册增量实时通道，避免首屏漏消息。
                    return initialPayload(subject)
                            .flatMap(payload -> realtimeService.register(subject.tenantId(), subject.userId(), session, payload));
                })
                .onErrorResume(e -> {
                    log.warn("[NOTIFICATION_WS] 建立连接失败 sessionId={}: {}",
                            session.getId(), e.getMessage());
                    return session.close(CloseStatus.SERVER_ERROR);
                });
    }

    /**
     * 构造 WebSocket 建连后的首屏同步载荷。
     *
     * <p>载荷包含最近通知列表和当前未读数，供前端在开始接收增量事件前完成本地状态初始化。
     *
     * @param subject 已通过 ticket 校验的连接主体，包含租户和用户身份
     * @return 首屏同步通知载荷
     */
    private Mono<NotificationRealtimePayload> initialPayload(NotificationWebSocketTicketService.TicketSubject subject) {
        return Mono.fromCallable(() -> {
                    // MyBatis 查询是阻塞 IO，放到 boundedElastic 避免占用 WebFlux 事件循环。
                    var notifications = notificationService
                            .list(subject.tenantId(), subject.userId(), false, null, false, 1, INITIAL_SYNC_SIZE)
                            .stream()
                            .map(NotificationDTO::from)
                            .toList();
                    return NotificationRealtimePayload.sync(
                            notifications,
                            notificationService.unreadCount(subject.userId(), subject.tenantId()));
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 从 WebSocket 握手 URI 的查询参数中提取连接票据。
     *
     * @param session 当前 WebSocket 会话
     * @return ticket 参数值；参数不存在时返回 null
     */
    private String extractTicket(WebSocketSession session) {
        return UriComponentsBuilder.fromUri(session.getHandshakeInfo().getUri())
                .build()
                .getQueryParams()
                .getFirst("ticket");
    }
}
