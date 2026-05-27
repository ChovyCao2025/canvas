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
 * 通知消息 Web Socket节点处理器。
 *
 * <p>由 DAG 执行器在运行画布节点时调用，读取节点 config 与执行上下文，产出 NodeResult 决定后续路由。
 * <p>处理器应保持单节点职责，跨节点编排、重试和状态持久化由执行引擎统一管理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler implements WebSocketHandler {

    private static final int INITIAL_SYNC_SIZE = 20;

    private final NotificationWebSocketTicketService ticketService;
    private final NotificationService notificationService;
    private final NotificationRealtimeService realtimeService;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String ticket = extractTicket(session);
        if (ticket == null || ticket.isBlank()) {
            return session.close(CloseStatus.POLICY_VIOLATION);
        }
        return Mono.fromCallable(() -> ticketService.consumeTicket(ticket))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(userId -> {
                    if (userId == null || userId.isBlank()) {
                        return session.close(CloseStatus.POLICY_VIOLATION);
                    }
                    return initialPayload(userId)
                            .flatMap(payload -> realtimeService.register(userId, session, payload));
                })
                .onErrorResume(e -> {
                    log.warn("[NOTIFICATION_WS] 建立连接失败 sessionId={}: {}",
                            session.getId(), e.getMessage());
                    return session.close(CloseStatus.SERVER_ERROR);
                });
    }

    private Mono<NotificationRealtimePayload> initialPayload(String userId) {
        return Mono.fromCallable(() -> {
                    var notifications = notificationService
                            .list(userId, false, null, false, 1, INITIAL_SYNC_SIZE)
                            .stream()
                            .map(NotificationDTO::from)
                            .toList();
                    return NotificationRealtimePayload.sync(
                            notifications,
                            notificationService.unreadCount(userId));
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private String extractTicket(WebSocketSession session) {
        return UriComponentsBuilder.fromUri(session.getHandshakeInfo().getUri())
                .build()
                .getQueryParams()
                .getFirst("ticket");
    }
}
