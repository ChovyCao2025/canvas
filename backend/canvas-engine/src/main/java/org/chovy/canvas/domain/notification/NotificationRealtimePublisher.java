package org.chovy.canvas.domain.notification;

import org.chovy.canvas.dal.dataobject.NotificationDO;

/**
 * 通知实时推送发布器接口。
 *
 * <p>定义站内通知从领域服务发布到实时通道的最小契约，调用方不需要感知 WebSocket 连接管理细节。
 * <p>实现类负责把通知事件、目标用户和未读数封装为前端可消费的实时消息。
 */
public interface NotificationRealtimePublisher {

    /**
     * 向旧版全局实时通道发布用户通知事件。
     *
     * <p>未显式传入租户时由实现类按兼容逻辑处理，适用于尚未携带租户上下文的调用方。
     *
     * @param eventType 实时事件类型，如 CREATED、UPDATED、READ
     * @param userId 目标用户 ID
     * @param notification 事件关联的通知实体
     * @param unreadCount 目标用户当前未读通知数
     */
    void publish(String eventType, String userId, NotificationDO notification, Long unreadCount);

    /**
     * 发布指定租户内用户的实时通知。
     *
     * @param eventType 实时事件类型，如 CREATED、UPDATED、READ
     * @param tenantId 所属租户，null 表示旧版全局通道
     * @param userId 目标用户 ID
     * @param notification 事件关联的通知实体
     * @param unreadCount 目标用户当前未读通知数
     */
    default void publish(String eventType, Long tenantId, String userId, NotificationDO notification, Long unreadCount) {
        publish(eventType, userId, notification, unreadCount);
    }
}
