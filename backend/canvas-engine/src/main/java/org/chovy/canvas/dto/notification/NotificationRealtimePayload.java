package org.chovy.canvas.dto.notification;

import java.time.LocalDateTime;
import java.util.List;

public record NotificationRealtimePayload(
        String eventType,
        NotificationDTO notification,
        List<NotificationDTO> notifications,
        Long unreadCount,
        LocalDateTime serverTime
) {
    public static NotificationRealtimePayload sync(List<NotificationDTO> notifications, long unreadCount) {
        return new NotificationRealtimePayload("SYNC", null, notifications, unreadCount, LocalDateTime.now());
    }

    public static NotificationRealtimePayload event(
            String eventType, NotificationDTO notification, Long unreadCount) {
        return new NotificationRealtimePayload(eventType, notification, List.of(), unreadCount, LocalDateTime.now());
    }
}
