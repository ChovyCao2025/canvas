package org.chovy.canvas.domain.notification;

public interface NotificationRealtimePublisher {

    void publish(String eventType, String userId, Notification notification, Long unreadCount);
}
