package org.chovy.canvas.domain.notification;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RuntimeAlertNotificationTest {

    @Test
    void failedExecutionSpikeCreatesRuntimeAlertForAdmins() {
        NotificationService notificationService = mock(NotificationService.class);
        NotificationRecipientService recipientService = mock(NotificationRecipientService.class);
        when(recipientService.activeAdmins()).thenReturn(List.of("ops"));
        NotificationEventService service = new NotificationEventService(notificationService, recipientService);

        service.failedExecutionSpike(1L, 42, "5m");

        ArgumentCaptor<NotificationCreateCommand> captor = ArgumentCaptor.forClass(NotificationCreateCommand.class);
        verify(notificationService).create(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo("ops");
        assertThat(captor.getValue().category()).isEqualTo("ALERT");
        assertThat(captor.getValue().severity()).isEqualTo("ERROR");
        assertThat(captor.getValue().type()).isEqualTo("RUNTIME_FAILED_EXECUTION_SPIKE");
        assertThat(captor.getValue().dedupKey()).isEqualTo("runtime:failed-execution-spike:1:5m");
    }

    @Test
    void dlqGrowthCreatesWarningNotification() {
        NotificationService notificationService = mock(NotificationService.class);
        NotificationRecipientService recipientService = mock(NotificationRecipientService.class);
        when(recipientService.activeAdmins()).thenReturn(List.of("ops"));
        NotificationEventService service = new NotificationEventService(notificationService, recipientService);

        service.dlqGrowth("canvas-execution-dlq", 12);

        ArgumentCaptor<NotificationCreateCommand> captor = ArgumentCaptor.forClass(NotificationCreateCommand.class);
        verify(notificationService).create(captor.capture());
        assertThat(captor.getValue().severity()).isEqualTo("WARNING");
        assertThat(captor.getValue().type()).isEqualTo("RUNTIME_DLQ_GROWTH");
        assertThat(captor.getValue().targetUrl()).isEqualTo("/ops/runtime");
    }

    @Test
    void emergencyActionCompletedCreatesChangeNotification() {
        NotificationService notificationService = mock(NotificationService.class);
        NotificationRecipientService recipientService = mock(NotificationRecipientService.class);
        when(recipientService.activeAdmins()).thenReturn(List.of("ops"));
        NotificationEventService service = new NotificationEventService(notificationService, recipientService);

        service.emergencyActionCompleted("KILL", 7L, "root", "incident mitigation");

        ArgumentCaptor<NotificationCreateCommand> captor = ArgumentCaptor.forClass(NotificationCreateCommand.class);
        verify(notificationService).create(captor.capture());
        assertThat(captor.getValue().category()).isEqualTo("CHANGE");
        assertThat(captor.getValue().type()).isEqualTo("OPS_EMERGENCY_ACTION_COMPLETED");
        assertThat(captor.getValue().bizId()).isEqualTo("7");
        assertThat(captor.getValue().content()).contains("incident mitigation");
    }
}
