package org.chovy.canvas.controller;

import io.jsonwebtoken.Claims;
import org.chovy.canvas.domain.notification.Notification;
import org.chovy.canvas.domain.notification.NotificationService;
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
        when(service.unreadCount("system")).thenReturn(2L);

        NotificationController controller = new NotificationController(service);

        var response = controller.unreadCount().block();

        assertThat(response.getData().get("count")).isEqualTo(2L);
    }

    @Test
    void markRead_marksCurrentUserNotificationWhenNoSecurityContextExists() {
        NotificationService service = mock(NotificationService.class);
        NotificationController controller = new NotificationController(service);

        var response = controller.markRead("ntf_1").block();

        assertThat(response.getCode()).isZero();
        verify(service).markRead("system", "ntf_1");
    }

    @Test
    void unreadCount_usesClaimsUsername() {
        NotificationService service = mock(NotificationService.class);
        when(service.unreadCount("alice")).thenReturn(3L);
        NotificationController controller = new NotificationController(service);
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
        when(service.unreadCount("system")).thenReturn(2L);
        NotificationController controller = new NotificationController(service);
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
        Notification notification = new Notification();
        notification.setNotificationId("ntf_1");
        when(service.list("system", true, 1, 100)).thenReturn(List.of(notification));
        NotificationController controller = new NotificationController(service);

        var response = controller.list(true, -5, 999).block();

        assertThat(response.getData()).hasSize(1);
        verify(service).list("system", true, 1, 100);
    }
}
