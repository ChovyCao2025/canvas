package org.chovy.canvas.dto.notification;

import org.chovy.canvas.dal.dataobject.NotificationDO;

import java.time.LocalDateTime;

/**
 * 通知消息 数据传输对象。
 *
 * <p>用于在控制器、服务、异步任务或实时推送之间传递结构化数据，隔离外部 API 契约与数据库实体。
 * <p>该类型应保持轻量，只表达字段语义和序列化边界，不放入复杂业务流程。
 */
public record NotificationDTO(
        String notificationId,
        String type,
        String category,
        String severity,
        String status,
        String title,
        String content,
        String targetUrl,
        String actionLabel,
        String actionUrl,
        String taskId,
        String bizType,
        String bizId,
        String dedupKey,
        String payloadJson,
        LocalDateTime readAt,
        LocalDateTime archivedAt,
        LocalDateTime deliveredAt,
        LocalDateTime createdAt
) {
    public static NotificationDTO from(NotificationDO notification) {
        return new NotificationDTO(
                notification.getNotificationId(),
                notification.getType(),
                notification.getCategory(),
                notification.getSeverity(),
                notification.getStatus(),
                notification.getTitle(),
                notification.getContent(),
                notification.getTargetUrl(),
                notification.getActionLabel(),
                notification.getActionUrl(),
                notification.getTaskId(),
                notification.getBizType(),
                notification.getBizId(),
                notification.getDedupKey(),
                notification.getPayloadJson(),
                notification.getReadAt(),
                notification.getArchivedAt(),
                notification.getDeliveredAt(),
                notification.getCreatedAt()
        );
    }
}
