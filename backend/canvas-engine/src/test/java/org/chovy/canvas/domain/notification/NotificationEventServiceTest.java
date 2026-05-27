package org.chovy.canvas.domain.notification;

import org.chovy.canvas.dal.dataobject.CanvasManualApprovalDO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 通知消息 Event 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class NotificationEventServiceTest {

    @Test
    void approvalPendingCreatesActionableNotificationForEachApprover() {
        NotificationService notificationService = mock(NotificationService.class);
        NotificationRecipientService recipientService = mock(NotificationRecipientService.class);
        NotificationEventService service = new NotificationEventService(notificationService, recipientService);
        CanvasManualApprovalDO approval = approval("approval_1");

        service.approvalPending(approval, List.of("alice", "bob"));

        ArgumentCaptor<NotificationCreateCommand> captor = ArgumentCaptor.forClass(NotificationCreateCommand.class);
        verify(notificationService, org.mockito.Mockito.times(2)).create(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(NotificationCreateCommand::userId, NotificationCreateCommand::category, NotificationCreateCommand::type)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("alice", "APPROVAL", "APPROVAL_PENDING"),
                        org.assertj.core.groups.Tuple.tuple("bob", "APPROVAL", "APPROVAL_PENDING"));
    }

    @Test
    void systemAlertCreatesNotificationForActiveAdmins() {
        NotificationService notificationService = mock(NotificationService.class);
        NotificationRecipientService recipientService = mock(NotificationRecipientService.class);
        when(recipientService.activeAdmins()).thenReturn(List.of("admin", "ops"));
        NotificationEventService service = new NotificationEventService(notificationService, recipientService);

        service.systemAlert(
                "MQ_TRIGGER_NO_ROUTE",
                "MQ 触发无匹配画布",
                "tag=ORDER_PAID",
                "/mq-config",
                "MQ_TRIGGER",
                "ORDER_PAID",
                "mq:no-route:ORDER_PAID",
                null);

        ArgumentCaptor<NotificationCreateCommand> captor = ArgumentCaptor.forClass(NotificationCreateCommand.class);
        verify(notificationService, org.mockito.Mockito.times(2)).create(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(NotificationCreateCommand::userId, NotificationCreateCommand::category, NotificationCreateCommand::type)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("admin", "ALERT", "MQ_TRIGGER_NO_ROUTE"),
                        org.assertj.core.groups.Tuple.tuple("ops", "ALERT", "MQ_TRIGGER_NO_ROUTE"));
    }

    @Test
    void canvasChangedCreatesChangeNotificationForAdmins() {
        NotificationService notificationService = mock(NotificationService.class);
        NotificationRecipientService recipientService = mock(NotificationRecipientService.class);
        when(recipientService.activeAdmins()).thenReturn(List.of("admin"));
        NotificationEventService service = new NotificationEventService(notificationService, recipientService);

        service.canvasChanged(
                "CANVAS_PUBLISHED",
                7L,
                "画布已发布",
                "operator=alice",
                "INFO",
                "alice");

        ArgumentCaptor<NotificationCreateCommand> captor = ArgumentCaptor.forClass(NotificationCreateCommand.class);
        verify(notificationService, org.mockito.Mockito.times(2)).create(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(NotificationCreateCommand::userId, NotificationCreateCommand::category, NotificationCreateCommand::type)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("admin", "CHANGE", "CANVAS_PUBLISHED"),
                        org.assertj.core.groups.Tuple.tuple("alice", "CHANGE", "CANVAS_PUBLISHED"));
    }

    private CanvasManualApprovalDO approval(String id) {
        return CanvasManualApprovalDO.builder()
                .id(id)
                .executionId("exec_1")
                .canvasId(7L)
                .nodeId("node_1")
                .userId("customer_1")
                .timeoutAt(LocalDateTime.now().plusHours(2))
                .build();
    }
}
