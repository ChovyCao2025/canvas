package org.chovy.canvas.domain.notification;

import org.chovy.canvas.dto.notification.NotificationRealtimePayload;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationWebSocketHandlerTest {

    @Test
    void handleRejectsMissingTicket() {
        NotificationWebSocketTicketService ticketService = mock(NotificationWebSocketTicketService.class);
        NotificationService notificationService = mock(NotificationService.class);
        NotificationRealtimeService realtimeService = mock(NotificationRealtimeService.class);
        NotificationWebSocketHandler handler =
                new NotificationWebSocketHandler(ticketService, notificationService, realtimeService);
        WebSocketSession session = session("ws://localhost/canvas/ws/notifications");
        when(session.close(CloseStatus.POLICY_VIOLATION)).thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(session)).verifyComplete();

        verify(session).close(CloseStatus.POLICY_VIOLATION);
    }

    @Test
    void handleConsumesTicketAndRegistersRealtimeSessionWithInitialSync() {
        NotificationWebSocketTicketService ticketService = mock(NotificationWebSocketTicketService.class);
        NotificationService notificationService = mock(NotificationService.class);
        NotificationRealtimeService realtimeService = mock(NotificationRealtimeService.class);
        NotificationWebSocketHandler handler =
                new NotificationWebSocketHandler(ticketService, notificationService, realtimeService);
        WebSocketSession session = session("ws://localhost/canvas/ws/notifications?ticket=ntf_ws_1");
        Notification notification = new Notification();
        notification.setNotificationId("ntf_1");
        notification.setType("TASK_SUCCEEDED");
        when(ticketService.consumeTicket("ntf_ws_1")).thenReturn("alice");
        when(notificationService.list("alice", false, null, false, 1, 20)).thenReturn(List.of(notification));
        when(notificationService.unreadCount("alice")).thenReturn(3L);
        when(realtimeService.register(eq("alice"), eq(session), any(NotificationRealtimePayload.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(session)).verifyComplete();

        verify(ticketService).consumeTicket("ntf_ws_1");
        verify(realtimeService).register(eq("alice"), eq(session), any(NotificationRealtimePayload.class));
    }

    private WebSocketSession session(String uri) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getHandshakeInfo()).thenReturn(new HandshakeInfo(
                URI.create(uri),
                HttpHeaders.EMPTY,
                Mono.empty(),
                null));
        return session;
    }
}
