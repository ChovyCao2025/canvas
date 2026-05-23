package org.chovy.canvas.domain.notification;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
    void createForTask_trimsTitleToColumnLimit() {
        NotificationService service = new NotificationService(mapper);

        service.createForTask(
                "operator",
                "TASK_SUCCEEDED",
                "x".repeat(201),
                "VIP 人群 · 12 人",
                "/audiences?highlight=7&taskId=task_1",
                "task_1");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getTitle()).hasSize(200);
    }

    @Test
    void list_returnsPagedRecordsForUser() {
        Notification first = new Notification();
        first.setNotificationId("ntf_1");
        Notification second = new Notification();
        second.setNotificationId("ntf_2");
        Page<Notification> pageResult = new Page<>();
        pageResult.setRecords(List.of(first, second));
        when(mapper.selectPage(any(Page.class), any())).thenReturn(pageResult);
        NotificationService service = new NotificationService(mapper);

        List<Notification> notifications = service.list("operator", true, 1, 20);

        assertThat(notifications).containsExactly(first, second);
    }

    @Test
    void unreadCount_countsOnlyCurrentUserUnreadNotifications() {
        when(mapper.selectCount(any())).thenReturn(3L);
        NotificationService service = new NotificationService(mapper);

        assertThat(service.unreadCount("operator")).isEqualTo(3L);
    }

    @Test
    void markRead_updatesOnlyUnreadCurrentUserNotification() {
        NotificationService service = new NotificationService(mapper);

        service.markRead("operator", "ntf_1");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(mapper).update(captor.capture(), any(Wrapper.class));
        Notification updateEntity = captor.getValue();
        assertThat(updateEntity.getReadAt()).isNotNull();
        assertThat(updateEntity.getNotificationId()).isNull();
        assertThat(updateEntity.getUserId()).isNull();
        verify(mapper, never()).selectOne(any());
        verify(mapper, never()).updateById(any(Notification.class));
    }

    @Test
    void markAllRead_updatesUnreadCurrentUserNotificationsInOneStatement() {
        NotificationService service = new NotificationService(mapper);

        service.markAllRead("operator");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(mapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getReadAt()).isNotNull();
        verify(mapper, never()).selectList(any());
        verify(mapper, never()).updateById(any(Notification.class));
    }
}
