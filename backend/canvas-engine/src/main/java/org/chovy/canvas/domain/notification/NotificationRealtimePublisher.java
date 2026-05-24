package org.chovy.canvas.domain.notification;

import org.chovy.canvas.dal.dataobject.NotificationDO;


public interface NotificationRealtimePublisher {

    void publish(String eventType, String userId, NotificationDO notification, Long unreadCount);
}
