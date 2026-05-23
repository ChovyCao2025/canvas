package org.chovy.canvas.dto.notification;

import org.chovy.canvas.domain.notification.Notification;

import java.time.LocalDateTime;

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
    public static NotificationDTO from(Notification notification) {
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
