package org.chovy.canvas.domain.notification;

import org.chovy.canvas.dal.dataobject.NotificationDO;

/**
 * 通知实时推送发布器接口。
 *
 * <p>定义站内通知从领域服务发布到实时通道的最小契约，调用方不需要感知 WebSocket 连接管理细节。
 * <p>实现类负责把通知事件、目标用户和未读数封装为前端可消费的实时消息。
 */
public interface NotificationRealtimePublisher {

    void publish(String eventType, String userId, NotificationDO notification, Long unreadCount);
}
