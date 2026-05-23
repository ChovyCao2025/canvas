package org.chovy.canvas.domain.notification;

import lombok.Builder;

@Builder
public record NotificationCreateCommand(
        String userId,
        String category,
        String severity,
        String type,
        String title,
        String content,
        String targetUrl,
        String actionLabel,
        String actionUrl,
        String taskId,
        String bizType,
        String bizId,
        String dedupKey,
        String payloadJson
) {
}
