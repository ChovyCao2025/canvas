package org.chovy.canvas.dto.notification;

import org.chovy.canvas.domain.notification.Notification;

import java.time.LocalDateTime;

public record NotificationDTO(
        String notificationId,
        String type,
        String title,
        String content,
        String targetUrl,
        String taskId,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
    public static NotificationDTO from(Notification notification) {
        return new NotificationDTO(
                notification.getNotificationId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getTargetUrl(),
                notification.getTaskId(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
