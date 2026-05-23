package org.chovy.canvas.domain.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationMapper mapper;

    @Test
    void createForTask_insertsUnreadNotification() {
        NotificationService service = new NotificationService(mapper);

        Notification notification = service.createForTask(
                "operator",
                "TASK_SUCCEEDED",
                "人群计算完成",
                "VIP 人群 · 12 人",
                "/audiences?highlight=7&taskId=task_1",
                "task_1");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(mapper).insert(captor.capture());
        Notification inserted = captor.getValue();
        assertThat(notification).isSameAs(inserted);
        assertThat(inserted.getNotificationId()).startsWith("ntf_");
        assertThat(inserted.getUserId()).isEqualTo("operator");
        assertThat(inserted.getReadAt()).isNull();
    }

    @Test
    void unreadCount_countsOnlyCurrentUserUnreadNotifications() {
        when(mapper.selectCount(any())).thenReturn(3L);
        NotificationService service = new NotificationService(mapper);

        assertThat(service.unreadCount("operator")).isEqualTo(3L);
    }

    @Test
    void markRead_updatesOnlyCurrentUserNotification() {
        Notification notification = new Notification();
        notification.setNotificationId("ntf_1");
        notification.setUserId("operator");
        when(mapper.selectOne(any())).thenReturn(notification);
        NotificationService service = new NotificationService(mapper);

        service.markRead("operator", "ntf_1");

        assertThat(notification.getReadAt()).isNotNull();
        verify(mapper).updateById(notification);
    }
}
