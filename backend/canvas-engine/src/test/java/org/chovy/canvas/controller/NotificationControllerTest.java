package org.chovy.canvas.web;

import io.jsonwebtoken.Claims;
import org.chovy.canvas.dal.dataobject.NotificationDO;
import org.chovy.canvas.domain.notification.NotificationService;
import org.chovy.canvas.domain.notification.NotificationWebSocketTicketService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationControllerTest {

    @Test
    void unreadCount_returnsCurrentUserCountWhenNoSecurityContextExists() {
        NotificationService service = mock(NotificationService.class);
        NotificationWebSocketTicketService ticketService = mock(NotificationWebSocketTicketService.class);
        when(service.unreadCount("system")).thenReturn(2L);

        NotificationController controller = new NotificationController(service, ticketService);

        var response = controller.unreadCount().block();

        assertThat(response.getData().get("count")).isEqualTo(2L);
    }

    @Test
    void markRead_marksCurrentUserNotificationWhenNoSecurityContextExists() {
        NotificationService service = mock(NotificationService.class);
        NotificationWebSocketTicketService ticketService = mock(NotificationWebSocketTicketService.class);
        NotificationController controller = new NotificationController(service, ticketService);

        var response = controller.markRead("ntf_1").block();

        assertThat(response.getCode()).isZero();
        verify(service).markRead("system", "ntf_1");
    }

    @Test
    void unreadCount_usesClaimsUsername() {
        NotificationService service = mock(NotificationService.class);
        NotificationWebSocketTicketService ticketService = mock(NotificationWebSocketTicketService.class);
        when(service.unreadCount("alice")).thenReturn(3L);
        NotificationController controller = new NotificationController(service, ticketService);
        Claims claims = mock(Claims.class);
        when(claims.get("username", String.class)).thenReturn("alice");
        var auth = new UsernamePasswordAuthenticationToken(claims, null, List.of());

        var response = controller.unreadCount()
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                .block();

        assertThat(response.getData().get("count")).isEqualTo(3L);
    }

    @Test
    void unreadCount_fallsBackWhenClaimsUsernameIsMissing() {
        NotificationService service = mock(NotificationService.class);
        NotificationWebSocketTicketService ticketService = mock(NotificationWebSocketTicketService.class);
        when(service.unreadCount("system")).thenReturn(2L);
        NotificationController controller = new NotificationController(service, ticketService);
        Claims claims = mock(Claims.class);
        var auth = new UsernamePasswordAuthenticationToken(claims, null, List.of());

        var response = controller.unreadCount()
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                .block();

        assertThat(response.getData().get("count")).isEqualTo(2L);
    }

    @Test
    void list_capsLargeSizeAtOneHundred() {
        NotificationService service = mock(NotificationService.class);
        NotificationWebSocketTicketService ticketService = mock(NotificationWebSocketTicketService.class);
        NotificationDO notification = new NotificationDO();
        notification.setNotificationId("ntf_1");
        when(service.list("system", true, null, false, 1, 100)).thenReturn(List.of(notification));
        NotificationController controller = new NotificationController(service, ticketService);

        var response = controller.list(true, null, false, -5, 999).block();

        assertThat(response.getData()).hasSize(1);
        verify(service).list("system", true, null, false, 1, 100);
    }

    @Test
    void list_passesCategoryAndArchivedFilters() {
        NotificationService service = mock(NotificationService.class);
        NotificationWebSocketTicketService ticketService = mock(NotificationWebSocketTicketService.class);
        NotificationController controller = new NotificationController(service, ticketService);

        controller.list(false, "ALERT", true, 2, 30).block();

        verify(service).list("system", false, "ALERT", true, 2, 30);
    }

    @Test
    void createWsTicket_usesCurrentUserAndReturnsTicket() {
        NotificationService service = mock(NotificationService.class);
        NotificationWebSocketTicketService ticketService = mock(NotificationWebSocketTicketService.class);
        when(ticketService.createTicket("alice")).thenReturn("ntf_ws_1");
        NotificationController controller = new NotificationController(service, ticketService);
        Claims claims = mock(Claims.class);
        when(claims.get("username", String.class)).thenReturn("alice");
        var auth = new UsernamePasswordAuthenticationToken(claims, null, List.of());

        var response = controller.createWsTicket()
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                .block();

        assertThat(response.getData().ticket()).isEqualTo("ntf_ws_1");
        assertThat(response.getData().expiresInSeconds()).isEqualTo(60);
        verify(ticketService).createTicket("alice");
    }

    @Test
    void archive_archivesCurrentUserNotification() {
        NotificationService service = mock(NotificationService.class);
        NotificationWebSocketTicketService ticketService = mock(NotificationWebSocketTicketService.class);
        NotificationController controller = new NotificationController(service, ticketService);

        var response = controller.archive("ntf_1").block();

        assertThat(response.getCode()).isZero();
        verify(service).archive("system", "ntf_1");
    }
}
