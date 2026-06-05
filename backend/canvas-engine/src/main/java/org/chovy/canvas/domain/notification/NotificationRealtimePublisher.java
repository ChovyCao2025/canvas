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
     * 发布或发送 publish 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param eventType eventType 类型标识或分类条件
     * @param userId userId 对应的业务主键或标识
     * @param notification notification 方法执行所需的业务参数
     * @param unreadCount unreadCount 数量、阈值或分页参数
     */
    void publish(String eventType, String userId, NotificationDO notification, Long unreadCount);

    /**
     * 发布指定租户内用户的实时通知。
     *
     * @param eventType eventType 类型标识或分类条件
     * @param tenantId tenantId 所属租户，null 表示旧版全局通道
     * @param userId userId 对应的业务主键或标识
     * @param notification notification 方法执行所需的业务参数
     * @param unreadCount unreadCount 数量、阈值或分页参数
     */
    default void publish(String eventType, Long tenantId, String userId, NotificationDO notification, Long unreadCount) {
        publish(eventType, userId, notification, unreadCount);
    }
}
