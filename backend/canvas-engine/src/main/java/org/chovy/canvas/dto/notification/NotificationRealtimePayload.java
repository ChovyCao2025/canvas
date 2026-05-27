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
