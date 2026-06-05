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

    /** WebSocket 建连后首屏同步的通知条数。 */
    private static final int INITIAL_SYNC_SIZE = 20;

    /** WebSocket 票据服务，用于校验并消费一次性连接票据。 */
    private final NotificationWebSocketTicketService ticketService;
    /** 通知服务，用于读取首屏通知列表和未读数量。 */
    private final NotificationService notificationService;
    /** 实时通知服务，用于注册会话并推送增量消息。 */
    private final NotificationRealtimeService realtimeService;

    /**
     * 执行 handle 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param session session 方法执行所需的业务参数
     * @return 异步执行结果，订阅后产生节点结果或业务响应
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
     * 注册、调度或初始化 initial Payload 相关的业务数据。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param userId userId 对应的业务主键或标识
     * @return 异步执行结果，订阅后产生节点结果或业务响应
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
     * 执行 extract Ticket 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param session session 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
     */
    private String extractTicket(WebSocketSession session) {
        return UriComponentsBuilder.fromUri(session.getHandshakeInfo().getUri())
                .build()
                .getQueryParams()
                .getFirst("ticket");
    }
}
