package org.chovy.canvas.domain.notification;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.chovy.canvas.dal.dataobject.NotificationDO;
import org.chovy.canvas.dal.mapper.NotificationMapper;

/**
 * 通知消息 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationMapper mapper;
    @Mock
    private NotificationRealtimePublisher realtimePublisher;

    @Test
    void createForTask_insertsUnreadNotification() {
        when(mapper.selectCount(any())).thenReturn(1L);
        NotificationService service = new NotificationService(mapper, realtimePublisher);

        NotificationDO notification = service.createForTask(
                "operator",
                "TASK_SUCCEEDED",
                "人群计算完成",
                "VIP 人群 · 12 人",
                "/audiences?highlight=7&taskId=task_1",
                "task_1");

        ArgumentCaptor<NotificationDO> captor = ArgumentCaptor.forClass(NotificationDO.class);
        verify(mapper).insert(captor.capture());
        NotificationDO inserted = captor.getValue();
        assertThat(notification).isSameAs(inserted);
        assertThat(inserted.getNotificationId()).startsWith("ntf_");
        assertThat(inserted.getUserId()).isEqualTo("operator");
        assertThat(inserted.getReadAt()).isNull();
        assertThat(inserted.getCategory()).isEqualTo("TASK");
        assertThat(inserted.getSeverity()).isEqualTo("SUCCESS");
        assertThat(inserted.getStatus()).isEqualTo("UNREAD");
        assertThat(inserted.getActionLabel()).isEqualTo("查看结果");
        assertThat(inserted.getActionUrl()).isEqualTo("/audiences?highlight=7&taskId=task_1");
        verify(realtimePublisher).publish(eq("NOTIFICATION_CREATED"), eq("operator"), eq(inserted), eq(1L));
    }

    @Test
    void createForTask_trimsTitleToColumnLimit() {
        NotificationService service = new NotificationService(mapper, realtimePublisher);

        service.createForTask(
                "operator",
                "TASK_SUCCEEDED",
                "x".repeat(201),
                "VIP 人群 · 12 人",
                "/audiences?highlight=7&taskId=task_1",
                "task_1");

        ArgumentCaptor<NotificationDO> captor = ArgumentCaptor.forClass(NotificationDO.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getTitle()).hasSize(200);
    }

    @Test
    void createForTask_returnsExistingNotificationWhenTaskUserTypeAlreadyExists() {
        NotificationDO existing = new NotificationDO();
        existing.setNotificationId("ntf_existing");
        when(mapper.insert(any(NotificationDO.class))).thenThrow(new DuplicateKeyException("duplicate notification"));
        when(mapper.selectOne(any())).thenReturn(existing);
        NotificationService service = new NotificationService(mapper, realtimePublisher);

        NotificationDO notification = service.createForTask(
                "operator",
                "TASK_SUCCEEDED",
                "人群计算完成",
                "VIP 人群 · 12 人",
                "/audiences?highlight=7&taskId=task_1",
                "task_1");

        assertThat(notification).isSameAs(existing);
        verify(realtimePublisher, never()).publish(any(), any(), any(), any());
    }

    @Test
    void createAlert_deduplicatesByUserAndDedupKey() {
        NotificationDO existing = new NotificationDO();
        existing.setNotificationId("ntf_existing");
        when(mapper.insert(any(NotificationDO.class))).thenThrow(new DuplicateKeyException("duplicate alert"));
        when(mapper.selectOne(any())).thenReturn(existing);
        NotificationService service = new NotificationService(mapper, realtimePublisher);

        NotificationDO notification = service.create(NotificationCreateCommand.builder()
                .userId("admin")
                .category("ALERT")
                .severity("ERROR")
                .type("MQ_TRIGGER_FAILED")
                .title("MQ 触发失败")
                .content("ORDER_PAID 解析失败")
                .dedupKey("alert:mq:ORDER_PAID")
                .build());

        assertThat(notification).isSameAs(existing);
    }

    @Test
    void list_returnsPagedRecordsForUser() {
        NotificationDO first = new NotificationDO();
        first.setNotificationId("ntf_1");
        NotificationDO second = new NotificationDO();
        second.setNotificationId("ntf_2");
        Page<NotificationDO> pageResult = new Page<>();
        pageResult.setRecords(List.of(first, second));
        when(mapper.selectPage(any(Page.class), any())).thenReturn(pageResult);
        NotificationService service = new NotificationService(mapper, realtimePublisher);

        List<NotificationDO> notifications = service.list("operator", true, 1, 20);

        assertThat(notifications).containsExactly(first, second);
    }

    @Test
    void unreadCount_countsOnlyCurrentUserUnreadNotifications() {
        when(mapper.selectCount(any())).thenReturn(3L);
        NotificationService service = new NotificationService(mapper, realtimePublisher);

        assertThat(service.unreadCount("operator")).isEqualTo(3L);
    }

    @Test
    void markRead_updatesOnlyUnreadCurrentUserNotification() {
        when(mapper.selectCount(any())).thenReturn(0L);
        NotificationService service = new NotificationService(mapper, realtimePublisher);

        service.markRead("operator", "ntf_1");

        ArgumentCaptor<NotificationDO> captor = ArgumentCaptor.forClass(NotificationDO.class);
        verify(mapper).update(captor.capture(), any(Wrapper.class));
        NotificationDO updateEntity = captor.getValue();
        assertThat(updateEntity.getReadAt()).isNotNull();
        assertThat(updateEntity.getStatus()).isEqualTo("READ");
        assertThat(updateEntity.getNotificationId()).isNull();
        assertThat(updateEntity.getUserId()).isNull();
        verify(realtimePublisher).publish(eq("NOTIFICATION_UPDATED"), eq("operator"), eq(null), eq(0L));
        verify(mapper, never()).selectOne(any());
        verify(mapper, never()).updateById(any(NotificationDO.class));
    }

    @Test
    void markAllRead_updatesUnreadCurrentUserNotificationsInOneStatement() {
        when(mapper.selectCount(any())).thenReturn(0L);
        NotificationService service = new NotificationService(mapper, realtimePublisher);

        service.markAllRead("operator");

        ArgumentCaptor<NotificationDO> captor = ArgumentCaptor.forClass(NotificationDO.class);
        verify(mapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getReadAt()).isNotNull();
        assertThat(captor.getValue().getStatus()).isEqualTo("READ");
        verify(realtimePublisher).publish(eq("NOTIFICATION_UPDATED"), eq("operator"), eq(null), eq(0L));
        verify(mapper, never()).selectList(any());
        verify(mapper, never()).updateById(any(NotificationDO.class));
    }

    @Test
    void archive_setsArchivedAtAndNotifiesUnreadCount() {
        when(mapper.selectCount(any())).thenReturn(2L);
        NotificationService service = new NotificationService(mapper, realtimePublisher);

        service.archive("operator", "ntf_1");

        ArgumentCaptor<NotificationDO> captor = ArgumentCaptor.forClass(NotificationDO.class);
        verify(mapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getArchivedAt()).isNotNull();
        assertThat(captor.getValue().getStatus()).isEqualTo("ARCHIVED");
        verify(realtimePublisher).publish(eq("NOTIFICATION_UPDATED"), eq("operator"), eq(null), eq(2L));
    }
}
