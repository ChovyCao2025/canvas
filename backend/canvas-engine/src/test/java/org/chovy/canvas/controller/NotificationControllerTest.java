package org.chovy.canvas.controller;

import org.chovy.canvas.domain.notification.NotificationService;
import org.junit.jupiter.api.Test;

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
}
