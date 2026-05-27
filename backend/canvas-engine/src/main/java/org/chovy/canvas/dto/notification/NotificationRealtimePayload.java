package org.chovy.canvas.dto.notification;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知实时推送 Payload 数据传输对象。
 *
 * <p>用于在控制器、服务、异步任务或实时推送之间传递结构化数据，隔离外部 API 契约与数据库实体。
 * <p>该类型应保持轻量，只表达字段语义和序列化边界，不放入复杂业务流程。
 */
public record NotificationRealtimePayload(
        /** 实时事件类型，如 SYNC、CREATED、UPDATED、READ、PONG。 */
        String eventType,
        /** 单条事件关联的通知对象，批量同步事件可为空。 */
        NotificationDTO notification,
        /** 批量同步时返回的通知列表。 */
        List<NotificationDTO> notifications,
        /** 当前用户未读通知数。 */
        Long unreadCount,
        /** 服务端生成该推送载荷的时间。 */
        LocalDateTime serverTime
) {
    /**
     * 执行 sync 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param notifications notifications 方法执行所需的业务参数
     * @param unreadCount unreadCount 数量、阈值或分页参数
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public static NotificationRealtimePayload sync(List<NotificationDTO> notifications, long unreadCount) {
        return new NotificationRealtimePayload("SYNC", null, notifications, unreadCount, LocalDateTime.now());
    }

    /**
     * 执行 event 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param eventType eventType 类型标识或分类条件
     * @param notification notification 方法执行所需的业务参数
     * @param unreadCount unreadCount 数量、阈值或分页参数
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public static NotificationRealtimePayload event(
            String eventType, NotificationDTO notification, Long unreadCount) {
        return new NotificationRealtimePayload(eventType, notification, List.of(), unreadCount, LocalDateTime.now());
    }
}
